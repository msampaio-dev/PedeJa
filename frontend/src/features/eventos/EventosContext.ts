import { createContext } from 'react'
import type { StatusPedido } from '../pedidos/types'

export type EventoPedido = {
  pedidoId: number
  status: StatusPedido
}

export type EventosContextValue = {
  conectado: boolean
  /** Registra quem quer saber das mudanças de pedido. Devolve a função que cancela o registro. */
  assinar: (aoMudar: (evento: EventoPedido) => void) => () => void
}

export const EventosContext = createContext<EventosContextValue | null>(null)
