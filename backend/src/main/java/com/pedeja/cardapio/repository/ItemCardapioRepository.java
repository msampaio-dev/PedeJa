package com.pedeja.cardapio.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pedeja.cardapio.entity.ItemCardapio;

public interface ItemCardapioRepository extends JpaRepository<ItemCardapio, Long> {

	List<ItemCardapio> findByRestauranteIdOrderByNomeAsc(Long restauranteId);

	List<ItemCardapio> findByRestauranteIdAndDisponivelTrueOrderByNomeAsc(Long restauranteId);

	/**
	 * Busca o item já filtrando pelo restaurante do dono. Um id de outro
	 * restaurante devolve vazio (404), e o dono nem fica sabendo que ele existe.
	 */
	Optional<ItemCardapio> findByIdAndRestauranteId(Long id, Long restauranteId);

	boolean existsByRestauranteIdAndDisponivelTrue(Long restauranteId);

	boolean existsByRestauranteIdAndNomeIgnoreCase(Long restauranteId, String nome);

	boolean existsByRestauranteIdAndNomeIgnoreCaseAndIdNot(Long restauranteId, String nome, Long id);
}
