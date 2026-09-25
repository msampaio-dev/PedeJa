import { env } from '../config/env'
import { acompanharRequisicao } from './estadoServidor'

export type CampoInvalido = {
  campo: string
  mensagem: string
}

type ErroApi = {
  status?: number
  mensagem?: string
  campos?: CampoInvalido[]
}

export class ApiError extends Error {
  readonly status: number
  readonly campos: CampoInvalido[]

  constructor(status: number, mensagem: string, campos: CampoInvalido[] = []) {
    super(mensagem)
    this.name = 'ApiError'
    this.status = status
    this.campos = campos
  }
}

// Somam uns 60 segundos, o tempo que a API leva para acordar no plano gratuito.
export const ESPERAS_NOVA_TENTATIVA_MS = [2000, 4000, 8000, 15000, 30000]

// Respostas de quem está entre o navegador e a API enquanto ela não sobe.
const STATUS_SERVIDOR_INDISPONIVEL = [502, 503, 504]

function espera(milissegundos: number) {
  return new Promise((resolver) => setTimeout(resolver, milissegundos))
}

/**
 * Só GET é repetido sozinho. Repetir um POST pode fazer a mesma coisa duas
 * vezes, e quem precisa disso (o pedido) já manda a própria chave de
 * idempotência e decide quando tentar de novo.
 */
function podeRepetir(options: RequestInit) {
  return !options.method || options.method.toUpperCase() === 'GET'
}

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')

  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const encerrar = acompanharRequisicao()
  try {
    const response = await buscarComNovasTentativas(`${env.apiUrl}${path}`, { ...options, headers })
    return await lerResposta<T>(response)
  } finally {
    encerrar()
  }
}

async function buscarComNovasTentativas(url: string, options: RequestInit): Promise<Response> {
  const tentativas = podeRepetir(options) ? ESPERAS_NOVA_TENTATIVA_MS.length : 0

  for (let tentativa = 0; ; tentativa++) {
    try {
      const response = await fetch(url, options)
      if (!STATUS_SERVIDOR_INDISPONIVEL.includes(response.status) || tentativa >= tentativas) {
        return response
      }
    } catch (erro) {
      if (erro instanceof DOMException && erro.name === 'AbortError') throw erro
      if (tentativa >= tentativas) {
        throw new ApiError(0, 'Não foi possível conectar ao servidor. Verifique sua conexão e tente de novo.')
      }
    }
    await espera(ESPERAS_NOVA_TENTATIVA_MS[tentativa])
  }
}

async function lerResposta<T>(response: Response): Promise<T> {
  const possuiJson = response.headers.get('content-type')?.includes('application/json')
  const body = possuiJson ? (await response.json() as ErroApi | T) : null

  if (!response.ok) {
    const erro = body as ErroApi | null
    throw new ApiError(
      response.status,
      erro?.mensagem || 'Não foi possível concluir a solicitação.',
      erro?.campos || [],
    )
  }

  return body as T
}
