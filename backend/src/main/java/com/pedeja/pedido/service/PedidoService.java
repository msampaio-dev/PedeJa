package com.pedeja.pedido.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pedeja.cardapio.entity.ItemCardapio;
import com.pedeja.cardapio.repository.ItemCardapioRepository;
import com.pedeja.pedido.dto.CriacaoPedidoRequest;
import com.pedeja.pedido.dto.PedidoResponse;
import com.pedeja.pedido.dto.PedidoResumoResponse;
import com.pedeja.pedido.entity.LinhaPedido;
import com.pedeja.pedido.entity.Pedido;
import com.pedeja.pedido.entity.StatusPedido;
import com.pedeja.pedido.exception.ItemIndisponivelException;
import com.pedeja.pedido.exception.PedidoNaoEncontradoException;
import com.pedeja.pedido.exception.RestauranteFechadoException;
import com.pedeja.pedido.exception.StatusNaoPermitidoException;
import com.pedeja.pedido.repository.PedidoRepository;
import com.pedeja.restaurante.entity.Restaurante;
import com.pedeja.restaurante.exception.RestauranteNaoEncontradoException;
import com.pedeja.restaurante.repository.RestauranteRepository;
import com.pedeja.restaurante.service.RestauranteService;
import com.pedeja.shared.dto.PaginaResponse;

@Service
public class PedidoService {

	private final PedidoRepository pedidoRepository;
	private final RestauranteRepository restauranteRepository;
	private final ItemCardapioRepository itemCardapioRepository;
	private final RestauranteService restauranteService;
	private final Clock clock;

	public PedidoService(
			PedidoRepository pedidoRepository,
			RestauranteRepository restauranteRepository,
			ItemCardapioRepository itemCardapioRepository,
			RestauranteService restauranteService,
			Clock clock
	) {
		this.pedidoRepository = pedidoRepository;
		this.restauranteRepository = restauranteRepository;
		this.itemCardapioRepository = itemCardapioRepository;
		this.restauranteService = restauranteService;
		this.clock = clock;
	}

	public record PedidoCriado(PedidoResponse pedido, boolean novo) {
	}

	/**
	 * Com a mesma chave de idempotência, o cliente recebe o pedido que já existe
	 * em vez de um novo. Se duas requisições com a mesma chave chegarem juntas,
	 * as duas passam pela busca, e a constraint uk_pedidos_cliente_chave barra a
	 * segunda com 409.
	 */
	@Transactional
	public PedidoCriado criar(Long clienteId, CriacaoPedidoRequest request, String chaveIdempotencia) {
		if (chaveIdempotencia != null) {
			var existente = pedidoRepository.findByClienteIdAndChaveIdempotencia(clienteId, chaveIdempotencia);
			if (existente.isPresent()) {
				return new PedidoCriado(PedidoResponse.from(existente.get()), false);
			}
		}

		Restaurante restaurante = restauranteRepository.findById(request.restauranteId())
				.orElseThrow(RestauranteNaoEncontradoException::new);

		if (!restaurante.isAberto()) {
			throw new RestauranteFechadoException();
		}

		List<LinhaPedido> linhas = conferirItens(restaurante, request.itens());
		Pedido pedido = new Pedido(
				clienteId, restaurante, linhas, request.enderecoEntrega(), request.observacao(),
				chaveIdempotencia, Instant.now(clock));

		return new PedidoCriado(PedidoResponse.from(pedidoRepository.save(pedido)), true);
	}

	/**
	 * Junta itens repetidos, busca todos numa consulta só e confere cada um:
	 * precisa existir, ser deste restaurante e estar disponível.
	 */
	private List<LinhaPedido> conferirItens(Restaurante restaurante, List<CriacaoPedidoRequest.ItemRequest> itensPedidos) {
		Map<Long, Integer> quantidades = new LinkedHashMap<>();
		for (var item : itensPedidos) {
			quantidades.merge(item.itemId(), item.quantidade(), Integer::sum);
		}

		Map<Long, ItemCardapio> doCardapio = itemCardapioRepository.findAllById(quantidades.keySet())
				.stream()
				.collect(Collectors.toMap(ItemCardapio::getId, Function.identity()));

		return quantidades.entrySet().stream().map(entrada -> {
			ItemCardapio item = doCardapio.get(entrada.getKey());

			if (item == null || !item.getRestaurante().getId().equals(restaurante.getId())) {
				throw new ItemIndisponivelException("Um dos itens não pertence ao cardápio deste restaurante");
			}
			if (!item.isDisponivel()) {
				throw new ItemIndisponivelException("O item \"%s\" não está mais disponível".formatted(item.getNome()));
			}
			if (entrada.getValue() > 50) {
				throw new ItemIndisponivelException("A quantidade máxima de \"%s\" é 50".formatted(item.getNome()));
			}

			return new LinhaPedido(item.getId(), item.getNome(), item.getPreco(), entrada.getValue());
		}).toList();
	}

	@Transactional(readOnly = true)
	public PaginaResponse<PedidoResumoResponse> listarDoCliente(Long clienteId, int pagina, int tamanho) {
		var resultado = pedidoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId, PageRequest.of(pagina, tamanho));
		return PaginaResponse.from(resultado, PedidoResumoResponse::from);
	}

	/** Pedido de outro cliente responde 404, e não 403, para não confirmar que o id existe. */
	@Transactional(readOnly = true)
	public PedidoResponse buscarDoCliente(Long clienteId, Long pedidoId) {
		return pedidoRepository.findByIdAndClienteId(pedidoId, clienteId)
				.map(PedidoResponse::from)
				.orElseThrow(PedidoNaoEncontradoException::new);
	}

	@Transactional
	public PedidoResponse cancelarPeloCliente(Long clienteId, Long pedidoId) {
		Pedido pedido = travarDoCliente(clienteId, pedidoId);
		pedido.mudarStatus(StatusPedido.CANCELADO, Instant.now(clock));
		return PedidoResponse.from(pedido);
	}

	@Transactional(readOnly = true)
	public List<PedidoResponse> listarDoRestaurante(Long usuarioId) {
		Restaurante restaurante = restauranteService.restauranteDoDono(usuarioId);
		return pedidoRepository
				.findTop100ByRestauranteIdAndStatusInOrderByCriadoEmAsc(
						restaurante.getId(), StatusPedido.EM_ANDAMENTO_NO_RESTAURANTE)
				.stream()
				.map(PedidoResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PedidoResponse buscarDoRestaurante(Long usuarioId, Long pedidoId) {
		Restaurante restaurante = restauranteService.restauranteDoDono(usuarioId);
		return pedidoRepository.findByIdAndRestauranteId(pedidoId, restaurante.getId())
				.filter(pedido -> pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO)
				.map(PedidoResponse::from)
				.orElseThrow(PedidoNaoEncontradoException::new);
	}

	@Transactional
	public PedidoResponse mudarStatusPeloRestaurante(Long usuarioId, Long pedidoId, StatusPedido novo) {
		if (!StatusPedido.DEFINIDOS_PELO_RESTAURANTE.contains(novo)) {
			throw new StatusNaoPermitidoException(novo);
		}

		Restaurante restaurante = restauranteService.restauranteDoDono(usuarioId);
		Pedido pedido = pedidoRepository.buscarParaAtualizar(pedidoId)
				.filter(p -> p.getRestaurante().getId().equals(restaurante.getId()))
				.filter(p -> p.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO)
				.orElseThrow(PedidoNaoEncontradoException::new);

		pedido.mudarStatus(novo, Instant.now(clock));
		return PedidoResponse.from(pedido);
	}

	/** Trava a linha do pedido (FOR UPDATE) e confere se ele é do cliente. */
	@Transactional
	public Pedido travarDoCliente(Long clienteId, Long pedidoId) {
		return pedidoRepository.buscarParaAtualizar(pedidoId)
				.filter(pedido -> pedido.pertenceAoCliente(clienteId))
				.orElseThrow(PedidoNaoEncontradoException::new);
	}
}
