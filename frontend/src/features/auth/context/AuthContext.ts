import { createContext } from 'react'
import type { LoginRequest, Sessao } from '../types'

export type AuthContextValue = {
  sessao: Sessao | null
  token: string | null
  carregando: boolean
  entrar: (credenciais: LoginRequest) => Promise<Sessao>
  sair: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
