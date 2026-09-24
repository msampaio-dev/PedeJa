package com.pedeja.restaurante.service;

import static com.pedeja.restaurante.repository.RestauranteSpecifications.comNomeContendo;
import static com.pedeja.restaurante.repository.RestauranteSpecifications.daCategoria;

import java.time.Clock;
import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pedeja.cardapio.dto.ItemCardapioResponse;
import com.pedeja.cardapio.repository.ItemCardapioRepository;
import com.pedeja.restaurante.dto.CadastroRestauranteRequest;
import com.pedeja.restaurante.dto.CardapioPublicoResponse;
import com.pedeja.restaurante.dto.RestauranteResponse;
import com.pedeja.restaurante.entity.CategoriaRestaurante;
import com.pedeja.restaurante.entity.DadosRestaurante;
import com.pedeja.restaurante.entity.Restaurante;
import com.pedeja.restaurante.exception.RestauranteNaoEncontradoException;
import com.pedeja.restaurante.exception.RestauranteSemCardapioException;
import com.pedeja.restaurante.repository.RestauranteRepository;
import com.pedeja.shared.dto.PaginaResponse;
import com.pedeja.usuario.entity.PerfilUsuario;
import com.pedeja.usuario.entity.Usuario;
import com.pedeja.usuario.service.UsuarioService;

@Service
public class RestauranteService {

	// Abertos primeiro: ninguém quer rolar a lista passando por restaurante fechado.
	private static final Sort ORDEM_LISTAGEM = Sort.by(Sort.Order.desc("aberto"), Sort.Order.asc("nome"));

	private final RestauranteRepository restauranteRepository;
	private final ItemCardapioRepository itemCardapioRepository;
	private final UsuarioService usuarioService;
	private final Clock clock;

	public RestauranteService(
			RestauranteRepository restauranteRepository,
			ItemCardapioRepository itemCardapioRepository,
			UsuarioService usuarioService,
			Clock clock
	) {
		this.restauranteRepository = restauranteRepository;
		this.itemCardapioRepository = itemCardapioRepository;
		this.usuarioService = usuarioService;
		this.clock = clock;
	}

	@Transactional
	public RestauranteResponse cadastrar(CadastroRestauranteRequest request) {
		Usuario dono = usuarioService.criar(
				request.nomeResponsavel(), request.email(), request.senha(), PerfilUsuario.RESTAURANTE);

		Restaurante restaurante = new Restaurante(dono.getId(), request.restaurante(), Instant.now(clock));
		return RestauranteResponse.from(restauranteRepository.save(restaurante));
	}

	@Transactional(readOnly = true)
	public PaginaResponse<RestauranteResponse> listar(
			CategoriaRestaurante categoria,
			String busca,
			int pagina,
			int tamanho
	) {
		var filtro = daCategoria(categoria).and(comNomeContendo(busca));
		var paginacao = PageRequest.of(pagina, tamanho, ORDEM_LISTAGEM);
		return PaginaResponse.from(restauranteRepository.findAll(filtro, paginacao), RestauranteResponse::from);
	}

	/**
	 * O cardápio público mostra só os itens disponíveis. Restaurante fechado
	 * continua visível, para o cliente ver o que tem lá, mas a fase 3 vai recusar
	 * pedido para ele.
	 */
	@Transactional(readOnly = true)
	public CardapioPublicoResponse cardapioPublico(Long restauranteId) {
		Restaurante restaurante = restauranteRepository.findById(restauranteId)
				.orElseThrow(RestauranteNaoEncontradoException::new);

		var itens = itemCardapioRepository.findByRestauranteIdAndDisponivelTrueOrderByNomeAsc(restauranteId)
				.stream()
				.map(ItemCardapioResponse::from)
				.toList();

		return new CardapioPublicoResponse(RestauranteResponse.from(restaurante), itens);
	}

	@Transactional(readOnly = true)
	public RestauranteResponse buscarDoDono(Long usuarioId) {
		return RestauranteResponse.from(restauranteDoDono(usuarioId));
	}

	@Transactional
	public RestauranteResponse atualizar(Long usuarioId, DadosRestaurante dados) {
		Restaurante restaurante = restauranteDoDono(usuarioId);
		restaurante.atualizar(dados);
		return RestauranteResponse.from(restaurante);
	}

	@Transactional
	public RestauranteResponse definirAberto(Long usuarioId, boolean aberto) {
		Restaurante restaurante = restauranteDoDono(usuarioId);

		if (aberto && !itemCardapioRepository.existsByRestauranteIdAndDisponivelTrue(restaurante.getId())) {
			throw new RestauranteSemCardapioException();
		}

		restaurante.definirAberto(aberto);
		return RestauranteResponse.from(restaurante);
	}

	/**
	 * O restaurante é sempre achado pelo usuário do token, nunca por um id vindo
	 * da requisição. Assim um dono não consegue mexer no restaurante de outro.
	 */
	@Transactional(readOnly = true)
	public Restaurante restauranteDoDono(Long usuarioId) {
		return restauranteRepository.findByUsuarioId(usuarioId)
				.orElseThrow(RestauranteNaoEncontradoException::new);
	}
}
