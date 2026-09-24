import { env } from '../config/env'

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

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')

  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
		headers.set('Content-Type', 'application/json')
	}

  let response: Response

  try {
    response = await fetch(`${env.apiUrl}${path}`, { ...options, headers })
  } catch {
    throw new ApiError(0, 'Não foi possível conectar à API. Verifique se o backend está ligado.')
  }

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

export function apiAssetUrl(path: string | null | undefined) {
	if (!path) return null
	if (/^https?:\/\//i.test(path)) return path
	return new URL(path, new URL(env.apiUrl, window.location.origin).origin).toString()
}
