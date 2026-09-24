import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../services/api'
import { AuthContext, type AuthContextValue } from '../context/AuthContext'
import type { Sessao } from '../types'
import { LoginPage } from './LoginPage'

function renderizar(entrar: AuthContextValue['entrar']) {
  const contexto: AuthContextValue = { sessao: null, token: null, carregando: false, entrar, sair: vi.fn() }

  render(
    <AuthContext.Provider value={contexto}>
      <MemoryRouter initialEntries={['/entrar']}>
        <Routes>
          <Route path="/entrar" element={<LoginPage />} />
          <Route path="/restaurantes" element={<p>área do cliente</p>} />
          <Route path="/restaurante/pedidos" element={<p>área do restaurante</p>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

const sessao = (perfil: Sessao['perfil']): Sessao => ({ id: 1, nome: 'Teste', email: 't@t.com', perfil })

describe('LoginPage', () => {
  it('leva o cliente para a lista de restaurantes', async () => {
    renderizar(vi.fn().mockResolvedValue(sessao('CLIENTE')))

    await userEvent.click(screen.getByRole('button', { name: 'Cliente' }))

    expect(await screen.findByText('área do cliente')).toBeInTheDocument()
  })

  it('leva o restaurante para o painel', async () => {
    renderizar(vi.fn().mockResolvedValue(sessao('RESTAURANTE')))

    await userEvent.click(screen.getByRole('button', { name: 'Restaurante' }))

    expect(await screen.findByText('área do restaurante')).toBeInTheDocument()
  })

  it('mostra a mensagem da API quando o login falha', async () => {
    renderizar(vi.fn().mockRejectedValue(new ApiError(401, 'E-mail ou senha inválidos')))

    await userEvent.type(screen.getByLabelText('E-mail'), 'x@x.com')
    await userEvent.type(screen.getByLabelText('Senha'), 'errada')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos')
  })
})
