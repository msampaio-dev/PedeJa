package com.pedeja.auth.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pedeja.auth.dto.CadastroClienteRequest;
import com.pedeja.auth.dto.SessaoResponse;
import com.pedeja.auth.exception.CredenciaisInvalidasException;
import com.pedeja.auth.exception.UsuarioNaoEncontradoException;
import com.pedeja.usuario.entity.PerfilUsuario;
import com.pedeja.usuario.entity.Usuario;
import com.pedeja.usuario.repository.UsuarioRepository;
import com.pedeja.usuario.service.UsuarioService;

@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final UsuarioService usuarioService;
	private final PasswordEncoder passwordEncoder;
	private final String senhaHashFicticia;

	public AuthService(
			UsuarioRepository usuarioRepository,
			UsuarioService usuarioService,
			PasswordEncoder passwordEncoder
	) {
		this.usuarioRepository = usuarioRepository;
		this.usuarioService = usuarioService;
		this.passwordEncoder = passwordEncoder;
		this.senhaHashFicticia = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Transactional
	public SessaoResponse cadastrarCliente(CadastroClienteRequest request) {
		Usuario usuario = usuarioService.criar(request.nome(), request.email(), request.senha(), PerfilUsuario.CLIENTE);
		return SessaoResponse.from(usuario);
	}

	@Transactional(readOnly = true)
	public Usuario autenticar(String email, String senha) {
		Usuario usuario = usuarioRepository.findByEmail(UsuarioService.normalizarEmail(email)).orElse(null);

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
}
