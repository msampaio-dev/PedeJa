import { type ReactNode, useEffect, useState } from 'react'
import { type ItemDaSacola, type RestauranteDaSacola, type Sacola, SacolaContext } from './SacolaContext'

const CHAVE = 'pedeja:sacola'
const VAZIA: Sacola = { restaurante: null, itens: [] }

function lerSacola(): Sacola {
  try {
    const valor = localStorage.getItem(CHAVE)
    return valor ? (JSON.parse(valor) as Sacola) : VAZIA
  } catch {
    return VAZIA
  }
}

/**
 * A sacola vive só no navegador, como no app de verdade: ninguém precisa de
 * conta para montar o pedido, só para enviá-lo. Os preços guardados aqui servem
 * para mostrar o total; o valor cobrado é recalculado pela API.
 */
export function SacolaProvider({ children }: { children: ReactNode }) {
  const [sacola, setSacola] = useState<Sacola>(lerSacola)

  useEffect(() => {
    try {
      localStorage.setItem(CHAVE, JSON.stringify(sacola))
    } catch {
      // Sem armazenamento (aba anônima, cota cheia) a sacola só não sobrevive ao recarregar.
    }
  }, [sacola])

  // Uma sacola tem itens de um restaurante só. Adicionar de outro troca o
  // conteúdo; a tela pergunta antes.
  function adicionar(restaurante: RestauranteDaSacola, item: Omit<ItemDaSacola, 'quantidade'>) {
    setSacola((atual) => {
      const base = atual.restaurante?.id === restaurante.id ? atual : { restaurante, itens: [] }
      const existente = base.itens.find((i) => i.itemId === item.itemId)
      const itens = existente
        ? base.itens.map((i) => (i.itemId === item.itemId ? { ...i, quantidade: Math.min(i.quantidade + 1, 50) } : i))
        : [...base.itens, { ...item, quantidade: 1 }]
      return { restaurante, itens }
    })
  }

  function alterarQuantidade(itemId: number, delta: number) {
    setSacola((atual) => {
      const itens = atual.itens
        .map((i) => (i.itemId === itemId ? { ...i, quantidade: Math.min(i.quantidade + delta, 50) } : i))
        .filter((i) => i.quantidade > 0)
      return itens.length === 0 ? VAZIA : { ...atual, itens }
    })
  }

  function esvaziar() {
    setSacola(VAZIA)
  }

  const quantidadeTotal = sacola.itens.reduce((soma, i) => soma + i.quantidade, 0)
  // Soma em centavos inteiros: somar reais em ponto flutuante gera 0,30000000000000004.
  const subtotal = sacola.itens.reduce((soma, i) => soma + Math.round(i.preco * 100) * i.quantidade, 0) / 100

  return (
    <SacolaContext.Provider value={{ ...sacola, quantidadeTotal, subtotal, adicionar, alterarQuantidade, esvaziar }}>
      {children}
    </SacolaContext.Provider>
  )
}
