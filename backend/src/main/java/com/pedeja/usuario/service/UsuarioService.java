package com.pedeja.usuario.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pedeja.usuario.entity.PerfilUsuario;
import com.pedeja.usuario.entity.Usuario;
import com.pedeja.usuario.exception.EmailJaCadastradoException;
import com.pedeja.usuario.repository.UsuarioRepository;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, Clock clock) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	/**
	 * Exige uma transação aberta por quem chama. O cadastro de restaurante cria o
	 * usuário e o restaurante juntos, e se o segundo falhar o primeiro não pode
	 * ficar gravado sozinho.
	 *
	 * A verificação prévia devolve uma mensagem clara no caso comum. Dois cadastros
	 * simultâneos com o mesmo e-mail passam juntos por ela, e aí quem barra o
	 * segundo é a constraint uk_usuarios_email (o GlobalExceptionHandler responde 409).
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public Usuario criar(String nome, String email, String senha, PerfilUsuario perfil) {
		String emailNormalizado = normalizarEmail(email);

		if (usuarioRepository.existsByEmail(emailNormalizado)) {
			throw new EmailJaCadastradoException();
		}

		return usuarioRepository.save(new Usuario(
				nome.trim(),
				emailNormalizado,
				passwordEncoder.encode(senha),
				perfil,
				Instant.now(clock)));
	}

	public static String normalizarEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
