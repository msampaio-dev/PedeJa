import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../services/api'
import { AuthContext, type AuthContextValue } from '../auth/context/AuthContext'
import type { Sessao } from '../auth/types'
import { SacolaPage } from './SacolaPage'
import { SacolaProvider } from './SacolaProvider'

const { fazerPedido } = vi.hoisted(() => ({ fazerPedido: vi.fn() }))
vi.mock('../pedidos/pedidoApi', () => ({ fazerPedido }))

const cliente: Sessao = { id: 1, nome: 'Ana', email: 'ana@teste.com', perfil: 'CLIENTE' }

function guardarSacola() {
  localStorage.setItem('pedeja:sacola', JSON.stringify({
    restaurante: { id: 7, nome: 'Burger do Bairro', taxaEntrega: 4.99 },
    itens: [{ itemId: 3, nome: 'Clássico', preco: 29.9, quantidade: 2 }],
  }))
}

function renderizar(sessao: Sessao | null) {
  const contexto: AuthContextValue = {
    sessao, token: sessao ? 'token' : null, carregando: false, entrar: vi.fn(), sair: vi.fn(),
  }
  render(
    <AuthContext.Provider value={contexto}>
      <SacolaProvider>
        <MemoryRouter initialEntries={['/sacola']}>
          <Routes>
            <Route path="/sacola" element={<SacolaPage />} />
            <Route path="/entrar" element={<p>tela de login</p>} />
            <Route path="/pedidos/:id" element={<p>tela do pedido</p>} />
          </Routes>
        </MemoryRouter>
      </SacolaProvider>
    </AuthContext.Provider>,
  )
}

async function preencherEEnviar() {
  await userEvent.type(screen.getByLabelText('Endereço de entrega'), 'Rua A, 10')
  await userEvent.click(screen.getByRole('button', { name: /pedido/ }))
}

describe('SacolaPage', () => {
  beforeEach(() => {
    fazerPedido.mockReset()
    guardarSacola()
  })

  it('soma o total com a taxa sem erro de arredondamento', () => {
    renderizar(cliente)

    // 2 × 29,90 + 4,99 = 64,79
    expect(screen.getByText('R$ 64,79')).toBeInTheDocument()
  })

  it('manda o visitante entrar antes de enviar', async () => {
    renderizar(null)

    await preencherEEnviar()

    expect(await screen.findByText('tela de login')).toBeInTheDocument()
    expect(fazerPedido).not.toHaveBeenCalled()
  })

  it('repete a mesma chave de idempotência quando o envio falha e o cliente tenta de novo', async () => {
    fazerPedido
      .mockRejectedValueOnce(new ApiError(0, 'Não foi possível conectar à API.'))
      .mockResolvedValueOnce({ id: 42 })
    renderizar(cliente)

    await preencherEEnviar()
    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível conectar à API.')
    await userEvent.click(screen.getByRole('button', { name: /Fazer pedido/ }))

    expect(await screen.findByText('tela do pedido')).toBeInTheDocument()
    const [primeiraChave, segundaChave] = fazerPedido.mock.calls.map((chamada) => chamada[2])
    expect(segundaChave).toBe(primeiraChave)
  })

  it('gera outra chave quando a sacola muda', async () => {
    fazerPedido.mockRejectedValue(new ApiError(0, 'Falhou'))
    renderizar(cliente)

    await preencherEEnviar()
    await screen.findByRole('alert')
    await userEvent.click(screen.getByRole('button', { name: 'Aumentar Clássico' }))
    await userEvent.click(screen.getByRole('button', { name: /Fazer pedido/ }))

    await vi.waitFor(() => expect(fazerPedido).toHaveBeenCalledTimes(2))
    const [primeiraChave, segundaChave] = fazerPedido.mock.calls.map((chamada) => chamada[2])
    expect(segundaChave).not.toBe(primeiraChave)
  })
})
