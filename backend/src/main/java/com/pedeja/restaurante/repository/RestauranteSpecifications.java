package com.pedeja.restaurante.repository;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.pedeja.restaurante.entity.CategoriaRestaurante;
import com.pedeja.restaurante.entity.Restaurante;

/**
 * Filtros opcionais da listagem. Com Specification, filtro ausente simplesmente
 * não entra no WHERE. A alternativa, "(:categoria IS NULL OR ...)" numa @Query,
 * esbarra no PostgreSQL, que não consegue deduzir o tipo de um parâmetro nulo.
 */
public final class RestauranteSpecifications {

	private RestauranteSpecifications() {
	}

	public static Specification<Restaurante> daCategoria(CategoriaRestaurante categoria) {
		return (root, query, cb) -> categoria == null ? null : cb.equal(root.get("categoria"), categoria);
	}

	public static Specification<Restaurante> comNomeContendo(String termo) {
		if (termo == null || termo.isBlank()) {
			return (root, query, cb) -> null;
		}
		String padrao = "%" + termo.trim().toLowerCase(Locale.ROOT) + "%";
		return (root, query, cb) -> cb.like(cb.lower(root.get("nome")), padrao);
	}
}
