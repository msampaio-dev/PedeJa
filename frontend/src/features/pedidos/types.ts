export type StatusPedido =
  | 'AGUARDANDO_PAGAMENTO'
  | 'PAGO'
  | 'ACEITO'
  | 'EM_PREPARO'
  | 'SAIU_PARA_ENTREGA'
  | 'ENTREGUE'
  | 'CANCELADO'
  | 'RECUSADO'

export type Pedido = {
  id: number
  status: StatusPedido
  restaurante: { id: number; nome: string }
  itens: { itemCardapioId: number; nome: string; precoUnitario: number; quantidade: number; subtotal: number }[]
  subtotal: number
  taxaEntrega: number
  total: number
  enderecoEntrega: string
  observacao: string | null
  criadoEm: string
  historico: { status: StatusPedido; ocorridoEm: string }[]
}

export type PedidoResumo = {
  id: number
  status: StatusPedido
  restauranteNome: string
  total: number
  criadoEm: string
}

export type CriacaoPedidoRequest = {
  restauranteId: number
  itens: { itemId: number; quantidade: number }[]
  enderecoEntrega: string
  observacao: string
}
