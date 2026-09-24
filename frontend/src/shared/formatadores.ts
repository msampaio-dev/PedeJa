import type { CategoriaRestaurante } from '../features/restaurantes/types'

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function formatarPreco(valor: number) {
  return moeda.format(valor)
}

export function formatarTaxaEntrega(valor: number) {
  return valor === 0 ? 'Entrega grátis' : `Entrega ${formatarPreco(valor)}`
}

export const ROTULO_CATEGORIA: Record<CategoriaRestaurante, string> = {
  PIZZA: 'Pizza',
  LANCHES: 'Lanches',
  JAPONESA: 'Japonesa',
  BRASILEIRA: 'Brasileira',
  DOCES: 'Doces',
  SAUDAVEL: 'Saudável',
}

export const EMOJI_CATEGORIA: Record<CategoriaRestaurante, string> = {
  PIZZA: '🍕',
  LANCHES: '🍔',
  JAPONESA: '🍣',
  BRASILEIRA: '🍛',
  DOCES: '🍰',
  SAUDAVEL: '🥗',
}

export const CATEGORIAS = Object.keys(ROTULO_CATEGORIA) as CategoriaRestaurante[]
