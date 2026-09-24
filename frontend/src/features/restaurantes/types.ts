export type CategoriaRestaurante = 'PIZZA' | 'LANCHES' | 'JAPONESA' | 'BRASILEIRA' | 'DOCES' | 'SAUDAVEL'

export type Restaurante = {
  id: number
  nome: string
  descricao: string | null
  categoria: CategoriaRestaurante
  taxaEntrega: number
  tempoEntregaMinutos: number
  aberto: boolean
}

export type ItemCardapio = {
  id: number
  nome: string
  descricao: string | null
  preco: number
  disponivel: boolean
}

export type CardapioPublico = {
  restaurante: Restaurante
  itens: ItemCardapio[]
}

export type Pagina<T> = {
  conteudo: T[]
  pagina: number
  tamanho: number
  totalElementos: number
  totalPaginas: number
  primeira: boolean
  ultima: boolean
}

export type DadosRestaurante = {
  nome: string
  descricao: string
  categoria: CategoriaRestaurante
  taxaEntrega: number
  tempoEntregaMinutos: number
}

export type DadosItemCardapio = {
  nome: string
  descricao: string
  preco: number
  disponivel: boolean
}

export type CadastroRestauranteRequest = {
  nomeResponsavel: string
  email: string
  senha: string
  restaurante: DadosRestaurante
}
