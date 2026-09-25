import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './api'
import { servidorAcordando } from './estadoServidor'

function respostaJson(status: number, corpo: unknown) {
  return new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': 'application/json' } })
}

describe('apiRequest com a API hibernando', () => {
  const fetchFalso = vi.fn()

  beforeEach(() => {
    vi.useFakeTimers()
    vi.stubGlobal('fetch', fetchFalso)
    fetchFalso.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('tenta o GET de novo enquanto o servidor acorda e avisa que está demorando', async () => {
    fetchFalso
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockResolvedValueOnce(respostaJson(503, {}))
      .mockResolvedValueOnce(respostaJson(200, { ok: true }))

    const resultado = apiRequest<{ ok: boolean }>('/restaurantes')

    await vi.advanceTimersByTimeAsync(4000)
    expect(servidorAcordando()).toBe(true)

    await vi.advanceTimersByTimeAsync(10000)
    await expect(resultado).resolves.toEqual({ ok: true })
    expect(fetchFalso).toHaveBeenCalledTimes(3)
    expect(servidorAcordando()).toBe(false)
  })

  it('não repete POST, que poderia fazer a mesma coisa duas vezes', async () => {
    fetchFalso.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(apiRequest('/auth/login', { method: 'POST', body: '{}' }))
      .rejects.toBeInstanceOf(ApiError)
    expect(fetchFalso).toHaveBeenCalledTimes(1)
  })

  it('desiste depois de cerca de um minuto', async () => {
    fetchFalso.mockRejectedValue(new TypeError('Failed to fetch'))

    const resultado = apiRequest('/restaurantes')
    const verificacao = expect(resultado).rejects.toMatchObject({ status: 0 })
    await vi.advanceTimersByTimeAsync(60000)

    await verificacao
    expect(fetchFalso).toHaveBeenCalledTimes(6)
  })

  it('não repete erro de verdade da API, como 404', async () => {
    fetchFalso.mockResolvedValue(respostaJson(404, { mensagem: 'Restaurante não encontrado' }))

    await expect(apiRequest('/restaurantes/99/cardapio')).rejects.toMatchObject({
      status: 404,
      message: 'Restaurante não encontrado',
    })
    expect(fetchFalso).toHaveBeenCalledTimes(1)
  })
})
