package com.pedeja.gatewayfake;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.pedeja.pagamento.gateway.GatewayPagamento;
import com.pedeja.pagamento.webhook.EventoPagamento.Resultado;

/**
 * Simula um provedor de Pix, como o modo sandbox do Mercado Pago ou do Stripe.
 *
 * Roda dentro da mesma aplicação só por conveniência de deploy, mas se comporta
 * como sistema externo: guarda as cobranças na própria memória (não no banco da
 * API) e avisa o resultado por HTTP, no webhook, com assinatura. Reiniciar a
 * aplicação apaga as cobranças daqui, como se o sandbox tivesse sido zerado.
 */
@Component
@ConditionalOnProperty(name = "app.gateway-fake.enabled", havingValue = "true")
public class GatewayFake implements GatewayPagamento {

	private final Map<String, Cobranca> cobrancas = new ConcurrentHashMap<>();
	private final EntregadorWebhook entregador;
	private final Clock clock;

	public GatewayFake(EntregadorWebhook entregador, Clock clock) {
		this.entregador = entregador;
		this.clock = clock;
	}

	private record Cobranca(String id, String referencia, BigDecimal valor, Instant expiraEm, Resultado resultado) {

		Cobranca decidir(Resultado resultado) {
			return new Cobranca(id, referencia, valor, expiraEm, resultado);
		}
	}

	@Override
	public CobrancaCriada criarCobranca(NovaCobranca nova) {
		String id = "cob_" + UUID.randomUUID();
		cobrancas.put(id, new Cobranca(id, nova.referencia(), nova.valor(), nova.expiraEm(), null));

		String pix = "00020126580014BR.GOV.BCB.PIX0136%s5204000053039865406%s5802BR5913PEDEJA SANDBOX6009SAO PAULO"
				.formatted(id, nova.valor().toPlainString());
		return new CobrancaCriada(id, pix);
	}

	/**
	 * Faz o papel do banco do cliente: paga (ou recusa) a cobrança. O resultado
	 * não volta nesta resposta; vai para a API pelo webhook, alguns instantes
	 * depois, como num gateway de verdade.
	 */
	public void simular(String cobrancaId, Resultado resultado) {
		Instant agora = Instant.now(clock);

		Cobranca atualizada = cobrancas.compute(cobrancaId, (id, cobranca) -> {
			if (cobranca == null) {
				throw new SimulacaoInvalidaException(404, "Cobrança não encontrada no sandbox");
			}
			if (cobranca.resultado() != null) {
				throw new SimulacaoInvalidaException(409, "Esta cobrança já foi paga ou recusada");
			}
			if (!agora.isBefore(cobranca.expiraEm())) {
				throw new SimulacaoInvalidaException(422, "Esta cobrança venceu. Gere outra no pedido");
			}
			return cobranca.decidir(resultado);
		});

		entregador.agendar(atualizada.id(), resultado);
	}
}
