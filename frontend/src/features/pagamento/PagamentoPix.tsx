import { useState } from 'react'
import { ApiError } from '../../services/api'
import { formatarPreco } from '../../shared/formatadores'
import { type Pagamento, gerarCobranca, simularNoBanco } from './pagamentoApi'
import styles from './PagamentoPix.module.css'

type Props = {
  token: string
  pedidoId: number
  pagamento: Pagamento | null
  aoMudar: (pagamento: Pagamento) => void
}

function mensagemDe(error: unknown, padrao: string) {
  return error instanceof ApiError ? error.message : padrao
}

export function PagamentoPix({ token, pedidoId, pagamento, aoMudar }: Props) {
  const [erro, setErro] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [copiado, setCopiado] = useState(false)
  const [aguardandoBanco, setAguardandoBanco] = useState(false)

  async function gerar() {
    setErro('')
    setOcupado(true)
    try {
      aoMudar(await gerarCobranca(token, pedidoId))
      setAguardandoBanco(false)
    } catch (error) {
      setErro(mensagemDe(error, 'Não foi possível gerar a cobrança.'))
    } finally {
      setOcupado(false)
    }
  }

  async function simular(resultado: 'APROVADO' | 'RECUSADO') {
    if (!pagamento) return
    setErro('')
    setOcupado(true)
    try {
      await simularNoBanco(pagamento.cobrancaId, resultado)
      // A resposta não traz o resultado: ele chega pelo webhook e a tela de
      // pedido percebe na próxima consulta.
      setAguardandoBanco(true)
    } catch (error) {
      setErro(mensagemDe(error, 'Não foi possível simular o pagamento.'))
    } finally {
      setOcupado(false)
    }
  }

  async function copiar() {
    if (!pagamento) return
    try {
      await navigator.clipboard.writeText(pagamento.pixCopiaECola)
      setCopiado(true)
    } catch {
      setCopiado(false)
    }
  }

  const pendente = pagamento?.status === 'PENDENTE'

  return (
    <section className={styles.bloco} aria-labelledby="titulo-pagamento">
      <h2 id="titulo-pagamento">Pagamento</h2>
      {erro && <p className={styles.erro} role="alert">{erro}</p>}

      {pagamento?.status === 'RECUSADO' && (
        <p className={styles.erro} role="status">O pagamento foi recusado. Você pode gerar outra cobrança.</p>
      )}
      {pagamento?.status === 'EXPIRADO' && (
        <p className={styles.aviso} role="status">A cobrança venceu. Gere outra para pagar.</p>
      )}

      {!pendente && (
        <button className="botao" disabled={ocupado} onClick={gerar} type="button">
          {ocupado ? 'Gerando...' : 'Pagar com Pix'}
        </button>
      )}

      {pendente && pagamento && (
        <>
          <p>
            Pague {formatarPreco(pagamento.valor)} com o código abaixo até{' '}
            {new Date(pagamento.expiraEm).toLocaleTimeString('pt-BR', { timeStyle: 'short' })}.
          </p>
          <div className={styles.pix}>
            <code>{pagamento.pixCopiaECola}</code>
            <button className="botao botao-secundario" onClick={copiar} type="button">
              {copiado ? 'Copiado' : 'Copiar código'}
            </button>
          </div>

          {aguardandoBanco ? (
            <p className={styles.aviso} role="status">Aguardando a confirmação do banco...</p>
          ) : (
            <div className={styles.sandbox}>
              <strong>Ambiente de demonstração</strong>
              <p>
                Nenhum dinheiro de verdade circula aqui. Use os botões para fazer o papel do seu banco:
                o resultado chega ao PedeJá pelo webhook, em alguns segundos.
              </p>
              <div className={styles.acoes}>
                <button className="botao" disabled={ocupado} onClick={() => simular('APROVADO')} type="button">
                  Simular pagamento aprovado
                </button>
                <button className="botao botao-secundario" disabled={ocupado} onClick={() => simular('RECUSADO')} type="button">
                  Simular recusa
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </section>
  )
}
