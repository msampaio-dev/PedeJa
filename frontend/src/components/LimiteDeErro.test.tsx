import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { LimiteDeErro } from './LimiteDeErro'

function Quebrado(): never {
  throw new Error('falha de renderização')
}

describe('LimiteDeErro', () => {
  it('troca a tela em branco por uma saída quando um componente quebra', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined)

    render(<LimiteDeErro><Quebrado /></LimiteDeErro>)

    expect(screen.getByRole('heading', { name: 'Esta tela não pôde ser exibida.' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Recarregar a página' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Voltar ao início' })).toHaveAttribute('href', '/')
  })
})
