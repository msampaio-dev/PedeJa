package com.pedeja.pagamento.webhook;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Assinatura HMAC-SHA256 no formato "t=&lt;epoch&gt;,v1=&lt;hex&gt;", no estilo do Stripe.
 *
 * Só quem conhece o segredo compartilhado com o gateway consegue gerar uma
 * assinatura válida, então um POST forjado no webhook é recusado. O timestamp
 * entra no cálculo e tem prazo: uma notificação verdadeira capturada e
 * reenviada horas depois (replay) também é recusada.
 */
@Component
public class AssinaturaWebhook {

	public static final String HEADER = "X-PedeJa-Assinatura";

	private static final Duration TOLERANCIA = Duration.ofMinutes(5);

	private final byte[] segredo;
	private final Clock clock;

	public AssinaturaWebhook(@Value("${app.pagamento.webhook-secret}") String segredo, Clock clock) {
		if (segredo == null || segredo.length() < 32) {
			throw new IllegalStateException("app.pagamento.webhook-secret deve ter pelo menos 32 caracteres");
		}
		this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
		this.clock = clock;
	}

	public String assinar(String corpo) {
		long timestamp = Instant.now(clock).getEpochSecond();
		return "t=%d,v1=%s".formatted(timestamp, hmac(timestamp + "." + corpo));
	}

	public boolean valida(String cabecalho, String corpo) {
		if (cabecalho == null || corpo == null) {
			return false;
		}

		Long timestamp = null;
		String recebida = null;
		for (String parte : cabecalho.split(",")) {
			String[] chaveValor = parte.trim().split("=", 2);
			if (chaveValor.length != 2) {
				continue;
			}
			if (chaveValor[0].equals("t")) {
				try {
					timestamp = Long.parseLong(chaveValor[1]);
				} catch (NumberFormatException e) {
					return false;
				}
			} else if (chaveValor[0].equals("v1")) {
				recebida = chaveValor[1];
			}
		}

		if (timestamp == null || recebida == null) {
			return false;
		}

		Duration idade = Duration.between(Instant.ofEpochSecond(timestamp), Instant.now(clock)).abs();
		if (idade.compareTo(TOLERANCIA) > 0) {
			return false;
		}

		String esperada = hmac(timestamp + "." + corpo);
		// Comparação em tempo constante: equals() pararia no primeiro caractere
		// diferente, e o tempo de resposta ajudaria a adivinhar a assinatura.
		return MessageDigest.isEqual(
				esperada.getBytes(StandardCharsets.UTF_8),
				recebida.getBytes(StandardCharsets.UTF_8));
	}

	private String hmac(String conteudo) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("Não foi possível calcular o HMAC", e);
		}
	}
}
