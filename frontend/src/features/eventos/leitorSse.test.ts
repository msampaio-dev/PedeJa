import { describe, expect, it } from 'vitest'
import { separarEventos } from './leitorSse'

describe('separarEventos', () => {
  it('lê nome e dados de cada evento e ignora o ping', () => {
    const { eventos, resto } = separarEventos(
      ':ping\n\nevent:pedido\ndata:{"pedidoId":7,"status":"PAGO"}\n\n',
    )

    expect(eventos).toEqual([{ nome: 'pedido', dados: '{"pedidoId":7,"status":"PAGO"}' }])
    expect(resto).toBe('')
  })

  it('guarda o evento cortado ao meio até o próximo pedaço chegar', () => {
    const primeiro = separarEventos('event:pedido\ndata:{"pedi')
    expect(primeiro.eventos).toEqual([])

    const segundo = separarEventos(primeiro.resto + 'doId":7}\n\n')
    expect(segundo.eventos).toEqual([{ nome: 'pedido', dados: '{"pedidoId":7}' }])
  })

  it('aceita quebra de linha do Windows', () => {
    const { eventos } = separarEventos('event:conectado\r\ndata:{}\r\n\r\n')

    expect(eventos).toEqual([{ nome: 'conectado', dados: '{}' }])
  })
})
