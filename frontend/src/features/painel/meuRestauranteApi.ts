import { apiRequest } from '../../services/api'
import type { Pedido, StatusPedido } from '../pedidos/types'
import type { DadosItemCardapio, DadosRestaurante, ItemCardapio, Restaurante } from '../restaurantes/types'

function autorizado(token: string, opcoes: RequestInit = {}): RequestInit {
  return { ...opcoes, headers: { ...opcoes.headers, Authorization: `Bearer ${token}` } }
}

export function buscarMeuRestaurante(token: string) {
  return apiRequest<Restaurante>('/meu-restaurante', autorizado(token))
}

export function atualizarMeuRestaurante(token: string, dados: DadosRestaurante) {
  return apiRequest<Restaurante>('/meu-restaurante', autorizado(token, {
    method: 'PUT',
    body: JSON.stringify(dados),
  }))
}

export function definirAbertura(token: string, aberto: boolean) {
  return apiRequest<Restaurante>('/meu-restaurante/abertura', autorizado(token, {
    method: 'PATCH',
    body: JSON.stringify({ aberto }),
  }))
}

export function listarMeusItens(token: string) {
  return apiRequest<ItemCardapio[]>('/meu-restaurante/itens', autorizado(token))
}

export function criarItem(token: string, dados: DadosItemCardapio) {
  return apiRequest<ItemCardapio>('/meu-restaurante/itens', autorizado(token, {
    method: 'POST',
    body: JSON.stringify(dados),
  }))
}

export function atualizarItem(token: string, id: number, dados: DadosItemCardapio) {
  return apiRequest<ItemCardapio>(`/meu-restaurante/itens/${id}`, autorizado(token, {
    method: 'PUT',
    body: JSON.stringify(dados),
  }))
}

export function listarPedidosDoRestaurante(token: string) {
  return apiRequest<Pedido[]>('/meu-restaurante/pedidos', autorizado(token))
}

export function mudarStatusPedido(token: string, pedidoId: number, status: StatusPedido) {
  return apiRequest<Pedido>(`/meu-restaurante/pedidos/${pedidoId}/status`, autorizado(token, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  }))
}
