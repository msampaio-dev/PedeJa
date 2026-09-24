import { createContext } from 'react'

export type RestauranteDaSacola = {
  id: number
  nome: string
  taxaEntrega: number
}

export type ItemDaSacola = {
  itemId: number
  nome: string
  preco: number
  quantidade: number
}

export type Sacola = {
  restaurante: RestauranteDaSacola | null
  itens: ItemDaSacola[]
}

export type SacolaContextValue = Sacola & {
  quantidadeTotal: number
  subtotal: number
  adicionar: (restaurante: RestauranteDaSacola, item: Omit<ItemDaSacola, 'quantidade'>) => void
  alterarQuantidade: (itemId: number, delta: number) => void
  esvaziar: () => void
}

export const SacolaContext = createContext<SacolaContextValue | null>(null)
