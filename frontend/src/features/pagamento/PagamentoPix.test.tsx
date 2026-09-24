import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { PagamentoPix } from './PagamentoPix'
import type { Pagamento } from './pagamentoApi'

const { gerarCobranca, simularNoBanco } = vi.hoisted(() => ({
  gerarCobranca: vi.fn(),
  simularNoBanco: vi.fn(),
}))
vi.mock('./pagamentoApi', () => ({ gerarCobranca, simularNoBanco }))

const pendente: Pagamento = {
  id: 1, pedidoId: 9, status: 'PENDENTE', valor: 30, cobrancaId: 'cob_123',
  pixCopiaECola: '000201PIX', expiraEm: new Date(Date.now() + 900_000).toISOString(),
}

describe('PagamentoPix', () => {
  it('gera a cobrança e mostra o código Pix', async () => {
    gerarCobranca.mockResolvedValue(pendente)
    const aoMudar = vi.fn()
    render(<PagamentoPix token="t" pedidoId={9} pagamento={null} aoMudar={aoMudar} />)

    await userEvent.click(screen.getByRole('button', { name: 'Pagar com Pix' }))

    expect(gerarCobranca).toHaveBeenCalledWith('t', 9)
    expect(aoMudar).toHaveBeenCalledWith(pendente)
  })

  it('simula o banco e espera a confirmação chegar pelo webhook', async () => {
    simularNoBanco.mockResolvedValue(null)
    render(<PagamentoPix token="t" pedidoId={9} pagamento={pendente} aoMudar={vi.fn()} />)

    expect(screen.getByText('000201PIX')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Simular pagamento aprovado' }))

    expect(simularNoBanco).toHaveBeenCalledWith('cob_123', 'APROVADO')
    expect(await screen.findByText('Aguardando a confirmação do banco...')).toBeInTheDocument()
  })

  it('oferece nova cobrança depois de uma recusa', () => {
    render(<PagamentoPix token="t" pedidoId={9} pagamento={{ ...pendente, status: 'RECUSADO' }} aoMudar={vi.fn()} />)

    expect(screen.getByText(/foi recusado/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Pagar com Pix' })).toBeInTheDocument()
  })
})
