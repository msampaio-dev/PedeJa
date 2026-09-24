import { useContext, useEffect, useRef } from 'react'
import { type EventoPedido, EventosContext } from './EventosContext'

/**
 * Chama aoMudar a cada mudança de pedido que chegar pelo SSE. Guarda a função
 * numa ref para a inscrição não ser refeita a cada renderização.
 */
export function useEventosPedido(aoMudar: (evento: EventoPedido) => void) {
  const contexto = useContext(EventosContext)
  const ultimaFuncao = useRef(aoMudar)

  useEffect(() => {
    ultimaFuncao.current = aoMudar
  })

  useEffect(() => {
    if (!contexto) return
    return contexto.assinar((evento) => ultimaFuncao.current(evento))
  }, [contexto])

  return { conectado: contexto?.conectado ?? false }
}
