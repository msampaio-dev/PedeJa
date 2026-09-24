import { ApiError, apiRequest } from '../../services/api'

export type StatusPagamento = 'PENDENTE' | 'APROVADO' | 'RECUSADO' | 'EXPIRADO'

export type Pagamento = {
  id: number
  pedidoId: number
  status: StatusPagamento
  valor: number
  cobrancaId: string
  pixCopiaECola: string
  expiraEm: string
}

function autorizado(token: string, opcoes: RequestInit = {}): RequestInit {
  return { ...opcoes, headers: { ...opcoes.headers, Authorization: `Bearer ${token}` } }
}

export function gerarCobranca(token: string, pedidoId: number) {
  return apiRequest<Pagamento>(`/pedidos/${pedidoId}/pagamento`, autorizado(token, { method: 'POST' }))
}

/** Devolve null quando o pedido ainda não teve nenhuma cobrança. */
export async function buscarPagamento(token: string, pedidoId: number) {
  try {
    return await apiRequest<Pagamento>(`/pedidos/${pedidoId}/pagamento`, autorizado(token))
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null
    throw error
  }
}

/**
 * Faz o papel do app do banco no sandbox. Não recebe token: quem chama o
 * gateway é o "banco", não o cliente logado no PedeJá.
 */
export function simularNoBanco(cobrancaId: string, resultado: 'APROVADO' | 'RECUSADO') {
  return apiRequest<void>(`/gateway-fake/cobrancas/${cobrancaId}/simulacao`, {
    method: 'POST',
    body: JSON.stringify({ resultado }),
  })
}
