package com.pedeja.auth.dto;

import com.pedeja.usuario.entity.PerfilUsuario;
import com.pedeja.usuario.entity.Usuario;

public record SessaoResponse(
		Long id,
		String nome,
		String email,
		PerfilUsuario perfil
) {

	public static SessaoResponse from(Usuario usuario) {
		return new SessaoResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil());
	}
}
