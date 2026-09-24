package com.pedeja.cardapio.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pedeja.cardapio.dto.ItemCardapioResponse;
import com.pedeja.cardapio.entity.DadosItemCardapio;
import com.pedeja.cardapio.entity.ItemCardapio;
import com.pedeja.cardapio.exception.ItemCardapioDuplicadoException;
import com.pedeja.cardapio.exception.ItemCardapioNaoEncontradoException;
import com.pedeja.cardapio.repository.ItemCardapioRepository;
import com.pedeja.restaurante.entity.Restaurante;
import com.pedeja.restaurante.service.RestauranteService;

@Service
public class CardapioService {

	private final ItemCardapioRepository itemCardapioRepository;
	private final RestauranteService restauranteService;
	private final Clock clock;

	public CardapioService(
			ItemCardapioRepository itemCardapioRepository,
			RestauranteService restauranteService,
			Clock clock
	) {
		this.itemCardapioRepository = itemCardapioRepository;
		this.restauranteService = restauranteService;
		this.clock = clock;
	}

	/** Visão do dono: inclui os itens indisponíveis, que o cliente não vê. */
	@Transactional(readOnly = true)
	public List<ItemCardapioResponse> listarDoDono(Long usuarioId) {
		Restaurante restaurante = restauranteService.restauranteDoDono(usuarioId);
		return itemCardapioRepository.findByRestauranteIdOrderByNomeAsc(restaurante.getId())
				.stream()
				.map(ItemCardapioResponse::from)
				.toList();
	}

	/**
	 * Mesmo esquema do e-mail no cadastro: a verificação dá a mensagem clara, e o
	 * índice único uk_itens_cardapio_restaurante_nome segura a corrida.
	 */
	@Transactional
	public ItemCardapioResponse criar(Long usuarioId, DadosItemCardapio dados) {
		Restaurante restaurante = restauranteService.restauranteDoDono(usuarioId);

		if (itemCardapioRepository.existsByRestauranteIdAndNomeIgnoreCase(restaurante.getId(), dados.nome().trim())) {
			throw new ItemCardapioDuplicadoException();
		}

		ItemCardapio item = new ItemCardapio(restaurante, dados, Instant.now(clock));
		return ItemCardapioResponse.from(itemCardapioRepository.save(item));
	}

	@Transactional
	public ItemCardapioResponse atualizar(Long usuarioId, Long itemId, DadosItemCardapio dados) {
		Restaurante restaurante = restauranteService.restauranteDoDono(usuarioId);
		ItemCardapio item = itemCardapioRepository.findByIdAndRestauranteId(itemId, restaurante.getId())
				.orElseThrow(ItemCardapioNaoEncontradoException::new);

		if (itemCardapioRepository.existsByRestauranteIdAndNomeIgnoreCaseAndIdNot(
				restaurante.getId(), dados.nome().trim(), itemId)) {
			throw new ItemCardapioDuplicadoException();
		}

		item.atualizar(dados);
		return ItemCardapioResponse.from(item);
	}
}
