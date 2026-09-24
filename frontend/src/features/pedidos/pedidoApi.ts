import { apiRequest } from '../../services/api'
import type { Pagina } from '../restaurantes/types'
import type { CriacaoPedidoRequest, Pedido, PedidoResumo } from './types'

function autorizado(token: string, opcoes: RequestInit = {}): RequestInit {
  return { ...opcoes, headers: { ...opcoes.headers, Authorization: `Bearer ${token}` } }
}

export function fazerPedido(token: string, dados: CriacaoPedidoRequest, chaveIdempotencia: string) {
  return apiRequest<Pedido>('/pedidos', autorizado(token, {
    method: 'POST',
    headers: { 'Idempotency-Key': chaveIdempotencia },
    body: JSON.stringify(dados),
  }))
}

export function listarMeusPedidos(token: string) {
  return apiRequest<Pagina<PedidoResumo>>('/pedidos', autorizado(token))
}

export function buscarPedido(token: string, id: number) {
  return apiRequest<Pedido>(`/pedidos/${id}`, autorizado(token))
}

export function cancelarPedido(token: string, id: number) {
  return apiRequest<Pedido>(`/pedidos/${id}/cancelamento`, autorizado(token, { method: 'POST' }))
}
