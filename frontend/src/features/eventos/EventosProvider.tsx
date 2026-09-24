import { type ReactNode, useCallback, useEffect, useRef, useState } from 'react'
import { env } from '../../config/env'
import { useAuth } from '../auth/context/useAuth'
import { type EventoPedido, EventosContext } from './EventosContext'
import { escutarEventos } from './leitorSse'

// Espera crescente entre reconexões, até 30 segundos. A API no plano gratuito
// hiberna e pode levar um minuto para voltar.
const ESPERAS_RECONEXAO_MS = [1000, 2000, 5000, 10000, 30000]

/**
 * Uma conexão SSE por aba, aberta enquanto houver alguém logado. As telas se
 * inscrevem aqui em vez de abrir conexões próprias.
 */
export function EventosProvider({ children }: { children: ReactNode }) {
  const { token } = useAuth()
  const [conectado, setConectado] = useState(false)
  const assinantes = useRef(new Set<(evento: EventoPedido) => void>())

  useEffect(() => {
    if (!token) return
    const controle = new AbortController()

    async function manterConexao() {
      for (let tentativa = 0; !controle.signal.aborted; tentativa++) {
        try {
          await escutarEventos(`${env.apiUrl}/eventos`, token!, (evento) => {
            if (evento.nome === 'conectado') {
              setConectado(true)
              tentativa = 0
            }
            if (evento.nome === 'pedido') {
              const dados = JSON.parse(evento.dados) as EventoPedido
              assinantes.current.forEach((aoMudar) => aoMudar(dados))
            }
          }, controle.signal)
        } catch {
          // Queda de rede ou servidor dormindo: cai na espera e tenta de novo.
        }
        setConectado(false)
        if (controle.signal.aborted) return

        const espera = ESPERAS_RECONEXAO_MS[Math.min(tentativa, ESPERAS_RECONEXAO_MS.length - 1)]
        await new Promise((resolver) => setTimeout(resolver, espera))
      }
    }

    manterConexao()
    return () => controle.abort()
  }, [token])

  const assinar = useCallback((aoMudar: (evento: EventoPedido) => void) => {
    assinantes.current.add(aoMudar)
    return () => { assinantes.current.delete(aoMudar) }
  }, [])

  return (
    <EventosContext.Provider value={{ conectado, assinar }}>
      {children}
    </EventosContext.Provider>
  )
}
