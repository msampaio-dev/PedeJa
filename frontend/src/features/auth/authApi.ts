import { apiRequest } from '../../services/api'
import type { CadastroClienteRequest, LoginRequest, LoginResponse, Sessao } from './types'

export function cadastrarCliente(dados: CadastroClienteRequest) {
  return apiRequest<Sessao>('/auth/cadastro', {
    method: 'POST',
    body: JSON.stringify(dados),
  })
}

export function login(dados: LoginRequest) {
  return apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(dados),
  })
}

export function buscarSessao(token: string) {
  return apiRequest<Sessao>('/auth/me', {
    headers: { Authorization: `Bearer ${token}` },
  })
}
