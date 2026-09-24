package com.pedeja.usuario.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Cada conta tem um único perfil. Quem pede comida e quem vende são pessoas
 * diferentes no PedeJá, então não há conta que seja cliente e restaurante ao
 * mesmo tempo (diferente do AgendaPro, onde um profissional também agenda).
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "senha_hash", nullable = false, length = 100)
	private String senhaHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PerfilUsuario perfil;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected Usuario() {
	}

	public Usuario(String nome, String email, String senhaHash, PerfilUsuario perfil, Instant criadoEm) {
		this.nome = nome;
		this.email = email;
		this.senhaHash = senhaHash;
		this.perfil = perfil;
		this.criadoEm = criadoEm;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getEmail() {
		return email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public PerfilUsuario getPerfil() {
		return perfil;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}
}
