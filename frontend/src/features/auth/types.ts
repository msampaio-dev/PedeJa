export type PerfilUsuario = 'CLIENTE' | 'RESTAURANTE'

export type CadastroClienteRequest = {
  nome: string
  email: string
  senha: string
}

export type LoginRequest = {
  email: string
  senha: string
}

export type Sessao = {
  id: number
  nome: string
  email: string
  perfil: PerfilUsuario
}

export type LoginResponse = {
  token: string
  tipo: 'Bearer'
  expiraEm: string
  usuario: Sessao
}
