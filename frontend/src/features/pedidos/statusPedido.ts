import type { StatusPedido } from './types'

export const ROTULO_STATUS: Record<StatusPedido, string> = {
  AGUARDANDO_PAGAMENTO: 'Aguardando pagamento',
  PAGO: 'Pago, esperando o restaurante',
  ACEITO: 'Pedido aceito pelo restaurante',
  EM_PREPARO: 'Preparando seu pedido',
  SAIU_PARA_ENTREGA: 'Saiu para entrega',
  ENTREGUE: 'Entregue',
  CANCELADO: 'Cancelado',
  RECUSADO: 'Recusado pelo restaurante',
}

export function formatarDataHora(iso: string) {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}
