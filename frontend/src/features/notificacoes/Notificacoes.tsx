import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiRequest } from '../../services/api'
import { useAuth } from '../auth/context/useAuth'
import { useEventosPedido } from '../eventos/useEventosPedido'
import { formatarDataHora } from '../pedidos/statusPedido'
import styles from './Notificacoes.module.css'

type Notificacao = {
  id: number
  pedidoId: number
  mensagem: string
  lida: boolean
  criadoEm: string
}

// O SSE avisa assim que o evento sai do outbox, mas a notificação é gravada por
// outro consumidor, na fila dele. Uma espera curta evita buscar antes da hora.
const ESPERA_APOS_EVENTO_MS = 1000

export function Notificacoes() {
  const { token, sessao } = useAuth()
  const [notificacoes, setNotificacoes] = useState<Notificacao[]>([])
  const [aberto, setAberto] = useState(false)
  const painel = useRef<HTMLDivElement>(null)

  const carregar = useCallback(async () => {
    if (!token) return
    setNotificacoes(await apiRequest<Notificacao[]>('/notificacoes', {
      headers: { Authorization: `Bearer ${token}` },
    }))
  }, [token])

  useEffect(() => {
    carregar().catch(() => undefined)
  }, [carregar])

  useEventosPedido(() => {
    setTimeout(() => { carregar().catch(() => undefined) }, ESPERA_APOS_EVENTO_MS)
  })

  useEffect(() => {
    if (!aberto) return
    function fecharAoClicarFora(evento: MouseEvent) {
      if (painel.current && !painel.current.contains(evento.target as Node)) setAberto(false)
    }
    document.addEventListener('mousedown', fecharAoClicarFora)
    return () => document.removeEventListener('mousedown', fecharAoClicarFora)
  }, [aberto])

  const naoLidas = notificacoes.filter((n) => !n.lida).length

  async function alternar() {
    const abrindo = !aberto
    setAberto(abrindo)
    if (abrindo && naoLidas > 0 && token) {
      await apiRequest<void>('/notificacoes/lidas', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      }).catch(() => undefined)
      // Mostra como não lidas nesta abertura; marca como lidas na próxima.
      setTimeout(() => setNotificacoes((atuais) => atuais.map((n) => ({ ...n, lida: true }))), 3000)
    }
  }

  const destino = (pedidoId: number) => (sessao?.perfil === 'RESTAURANTE' ? '/restaurante/pedidos' : `/pedidos/${pedidoId}`)

  return (
    <div className={styles.envoltorio} ref={painel}>
      <button
        aria-expanded={aberto}
        aria-label={naoLidas > 0 ? `Notificações, ${naoLidas} não lidas` : 'Notificações'}
        className={styles.sino}
        onClick={alternar}
        type="button"
      >
        <span aria-hidden="true">🔔</span>
        {naoLidas > 0 && <span className={styles.contador}>{naoLidas}</span>}
      </button>

      {aberto && (
        <div className={styles.painel} role="dialog" aria-label="Notificações">
          {notificacoes.length === 0 && <p className={styles.vazio}>Nenhuma notificação ainda.</p>}
          <ul>
            {notificacoes.map((n) => (
              <li className={n.lida ? '' : styles.naoLida} key={n.id}>
                <Link onClick={() => setAberto(false)} to={destino(n.pedidoId)}>
                  <span>{n.mensagem}</span>
                  <small>{formatarDataHora(n.criadoEm)}</small>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
