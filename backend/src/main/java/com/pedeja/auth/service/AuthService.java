package com.pedeja.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pedeja.auth.dto.CadastroClienteRequest;
import com.pedeja.auth.dto.SessaoResponse;
import com.pedeja.auth.exception.CredenciaisInvalidasException;
import com.pedeja.auth.exception.EmailJaCadastradoException;
import com.pedeja.auth.exception.UsuarioNaoEncontradoException;
import com.pedeja.usuario.entity.PerfilUsuario;
import com.pedeja.usuario.entity.Usuario;
import com.pedeja.usuario.repository.UsuarioRepository;

@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final String senhaHashFicticia;

	public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, Clock clock) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.senhaHashFicticia = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	/**
	 * A verificação prévia devolve uma mensagem clara no caso comum. Dois cadastros
	 * simultâneos com o mesmo e-mail passam juntos por ela, e aí quem barra o
	 * segundo é a constraint uk_usuarios_email (o GlobalExceptionHandler responde 409).
	 */
	@Transactional
	public SessaoResponse cadastrarCliente(CadastroClienteRequest request) {
		String email = normalizarEmail(request.email());

		if (usuarioRepository.existsByEmail(email)) {
			throw new EmailJaCadastradoException();
		}

		Usuario usuario = new Usuario(
				request.nome().trim(),
				email,
				passwordEncoder.encode(request.senha()),
				PerfilUsuario.CLIENTE,
				Instant.now(clock));

		return SessaoResponse.from(usuarioRepository.save(usuario));
	}

	@Transactional(readOnly = true)
	public Usuario autenticar(String email, String senha) {
		Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(email)).orElse(null);

		// Compara a senha mesmo quando o e-mail não existe, para o tempo de resposta
		// não revelar quais contas estão cadastradas.
		String hash = usuario != null ? usuario.getSenhaHash() : senhaHashFicticia;
		boolean senhaCorreta = passwordEncoder.matches(senha, hash);

		if (usuario == null || !senhaCorreta) {
			throw new CredenciaisInvalidasException();
		}

		return usuario;
	}

	@Transactional(readOnly = true)
	public SessaoResponse buscarSessao(Long usuarioId) {
		return usuarioRepository.findById(usuarioId)
				.map(SessaoResponse::from)
				.orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));
	}

	private String normalizarEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
