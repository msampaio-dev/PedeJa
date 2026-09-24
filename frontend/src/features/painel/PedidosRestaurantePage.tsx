import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Topo } from '../../components/Topo'
import { ApiError } from '../../services/api'
import { formatarPreco } from '../../shared/formatadores'
import { useAuth } from '../auth/context/useAuth'
import { useEventosPedido } from '../eventos/useEventosPedido'
import { ROTULO_STATUS, formatarDataHora } from '../pedidos/statusPedido'
import type { Pedido, StatusPedido } from '../pedidos/types'
import { listarPedidosDoRestaurante, mudarStatusPedido } from './meuRestauranteApi'
import styles from './PedidosRestaurantePage.module.css'

type Acao = { rotulo: string; status: StatusPedido; secundaria?: boolean }

// O próximo passo de cada status, na ordem da máquina de estados da API.
const ACOES: Partial<Record<StatusPedido, Acao[]>> = {
  PAGO: [
    { rotulo: 'Aceitar', status: 'ACEITO' },
    { rotulo: 'Recusar', status: 'RECUSADO', secundaria: true },
  ],
  ACEITO: [{ rotulo: 'Começar preparo', status: 'EM_PREPARO' }],
  EM_PREPARO: [{ rotulo: 'Saiu para entrega', status: 'SAIU_PARA_ENTREGA' }],
  SAIU_PARA_ENTREGA: [{ rotulo: 'Marcar como entregue', status: 'ENTREGUE' }],
}

// Rede de segurança caso o SSE caia sem avisar.
const INTERVALO_ATUALIZACAO_MS = 30000

function mensagemDe(error: unknown, padrao: string) {
  return error instanceof ApiError ? error.message : padrao
}

export function PedidosRestaurantePage() {
  const { token } = useAuth()
  const [pedidos, setPedidos] = useState<Pedido[] | null>(null)
  const [novos, setNovos] = useState<Set<number>>(new Set())
  const [erro, setErro] = useState('')
  const [ocupado, setOcupado] = useState<number | null>(null)

  const carregar = useCallback(async () => {
    if (!token) return
    setPedidos(await listarPedidosDoRestaurante(token))
  }, [token])

  useEffect(() => {
    carregar().catch((error) => setErro(mensagemDe(error, 'Não foi possível carregar os pedidos.')))
    const intervalo = setInterval(() => { carregar().catch(() => undefined) }, INTERVALO_ATUALIZACAO_MS)
    return () => clearInterval(intervalo)
  }, [carregar])

  const { conectado } = useEventosPedido((evento) => {
    if (evento.status === 'PAGO') {
      setNovos((atuais) => new Set(atuais).add(evento.pedidoId))
    }
    carregar().catch(() => undefined)
  })

  async function avancar(pedido: Pedido, acao: Acao) {
    if (!token) return
    if (acao.status === 'RECUSADO' && !window.confirm(`Recusar o pedido #${pedido.id}?`)) return
    setErro('')
    setOcupado(pedido.id)
    try {
      await mudarStatusPedido(token, pedido.id, acao.status)
      setNovos((atuais) => {
        const copia = new Set(atuais)
        copia.delete(pedido.id)
        return copia
      })
      await carregar()
    } catch (error) {
      setErro(mensagemDe(error, 'Não foi possível atualizar o pedido.'))
    } finally {
      setOcupado(null)
    }
  }

  return (
    <>
      <Topo />
      <main className={styles.pagina}>
        <header className={styles.cabecalho}>
          <div>
            <h1>Pedidos em andamento</h1>
            <span className={styles.meta}>
              {conectado ? 'Recebendo pedidos em tempo real' : 'Reconectando... a lista atualiza a cada 30 s'}
            </span>
          </div>
          <Link className="botao botao-secundario" to="/restaurante">Cardápio e dados</Link>
        </header>

        {erro && <p className={styles.erro} role="alert">{erro}</p>}
        {!erro && !pedidos && <p role="status">Carregando...</p>}
        {pedidos?.length === 0 && (
          <p className={styles.meta}>Nenhum pedido em andamento. Pedidos pagos aparecem aqui na hora.</p>
        )}

        <ul className={styles.lista}>
          {pedidos?.map((pedido) => (
            <li className={`${styles.cartao} ${novos.has(pedido.id) ? styles.novo : ''}`} key={pedido.id}>
              <div className={styles.topoCartao}>
                <strong>Pedido #{pedido.id}</strong>
                <span className={styles.status} data-status={pedido.status}>{ROTULO_STATUS[pedido.status]}</span>
              </div>
              <span className={styles.meta}>{formatarDataHora(pedido.criadoEm)} · {pedido.enderecoEntrega}</span>

              <ul className={styles.itens}>
                {pedido.itens.map((item) => (
                  <li key={item.itemCardapioId}>{item.quantidade}× {item.nome}</li>
                ))}
              </ul>
              {pedido.observacao && <p className={styles.observacao}>Obs.: {pedido.observacao}</p>}

              <div className={styles.rodape}>
                <strong>{formatarPreco(pedido.total)}</strong>
                <div className={styles.acoes}>
                  {ACOES[pedido.status]?.map((acao) => (
                    <button
                      className={`botao ${acao.secundaria ? 'botao-secundario' : ''}`}
                      disabled={ocupado === pedido.id}
                      key={acao.status}
                      onClick={() => avancar(pedido, acao)}
                      type="button"
                    >
                      {acao.rotulo}
                    </button>
                  ))}
                </div>
              </div>
            </li>
          ))}
        </ul>
      </main>
    </>
  )
}
