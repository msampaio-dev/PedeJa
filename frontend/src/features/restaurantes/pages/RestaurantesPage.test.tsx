import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../../auth/context/AuthContext'
import type { Pagina, Restaurante } from '../types'
import { RestaurantesPage } from './RestaurantesPage'

const { listarRestaurantes } = vi.hoisted(() => ({ listarRestaurantes: vi.fn() }))
vi.mock('../restauranteApi', () => ({ listarRestaurantes }))

function pagina(conteudo: Restaurante[]): Pagina<Restaurante> {
  return { conteudo, pagina: 0, tamanho: 20, totalElementos: conteudo.length, totalPaginas: 1, primeira: true, ultima: true }
}

const cantina: Restaurante = {
  id: 1, nome: 'Cantina da Nona', descricao: null, categoria: 'PIZZA',
  taxaEntrega: 0, tempoEntregaMinutos: 45, aberto: true,
}

function renderizar() {
  const contexto: AuthContextValue = { sessao: null, token: null, carregando: false, entrar: vi.fn(), sair: vi.fn() }
  render(
    <AuthContext.Provider value={contexto}>
      <MemoryRouter><RestaurantesPage /></MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('RestaurantesPage', () => {
  beforeEach(() => {
    listarRestaurantes.mockResolvedValue(pagina([cantina]))
  })

  it('lista os restaurantes com a entrega grátis destacada', async () => {
    renderizar()

    expect(await screen.findByText('Cantina da Nona')).toBeInTheDocument()
    expect(screen.getByText('Entrega grátis')).toBeInTheDocument()
  })

  it('filtra pela categoria escolhida e desfaz o filtro no segundo clique', async () => {
    renderizar()
    await screen.findByText('Cantina da Nona')

    const pizza = screen.getByRole('button', { name: /Pizza/ })
    await userEvent.click(pizza)
    await vi.waitFor(() =>
      expect(listarRestaurantes).toHaveBeenLastCalledWith({ categoria: 'PIZZA', busca: '' }))
    expect(pizza).toHaveAttribute('aria-pressed', 'true')

    await userEvent.click(pizza)
    await vi.waitFor(() =>
      expect(listarRestaurantes).toHaveBeenLastCalledWith({ categoria: null, busca: '' }))
  })

  it('avisa quando nenhum restaurante combina com a busca', async () => {
    listarRestaurantes.mockResolvedValue(pagina([]))
    renderizar()

    expect(await screen.findByText('Nenhum restaurante encontrado.')).toBeInTheDocument()
  })
})
