import { apiRequest } from '../../services/api'
import type {
  CadastroRestauranteRequest,
  CardapioPublico,
  CategoriaRestaurante,
  Pagina,
  Restaurante,
} from './types'

type FiltroRestaurantes = {
  categoria?: CategoriaRestaurante | null
  busca?: string
}

export function listarRestaurantes({ categoria, busca }: FiltroRestaurantes) {
  const parametros = new URLSearchParams()
  if (categoria) parametros.set('categoria', categoria)
  if (busca?.trim()) parametros.set('busca', busca.trim())

  const consulta = parametros.toString()
  return apiRequest<Pagina<Restaurante>>(`/restaurantes${consulta ? `?${consulta}` : ''}`)
}

export function buscarCardapio(restauranteId: number) {
  return apiRequest<CardapioPublico>(`/restaurantes/${restauranteId}/cardapio`)
}

export function cadastrarRestaurante(dados: CadastroRestauranteRequest) {
  return apiRequest<Restaurante>('/restaurantes/cadastro', {
    method: 'POST',
    body: JSON.stringify(dados),
  })
}
