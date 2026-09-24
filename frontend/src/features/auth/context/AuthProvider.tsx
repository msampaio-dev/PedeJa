import { type ReactNode, useEffect, useState } from 'react'
import { ApiError } from '../../../services/api'
import { buscarSessao, login } from '../authApi'
import { lerCredencial, removerCredencial, salvarCredencial } from '../authStorage'
import type { LoginRequest, Sessao } from '../types'
import { AuthContext } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null)
  const [sessao, setSessao] = useState<Sessao | null>(null)
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    let ativo = true
    const credencial = lerCredencial()

    if (!credencial) {
      setCarregando(false)
      return
    }

    // Confere com a API se o token salvo ainda vale. Só 401 derruba a sessão:
    // falha de rede não diz nada sobre o token.
    buscarSessao(credencial.token)
      .then((sessaoAtual) => {
        if (!ativo) return
        setToken(credencial.token)
        setSessao(sessaoAtual)
      })
      .catch((erro) => {
        if (erro instanceof ApiError && erro.status === 401) removerCredencial()
      })
      .finally(() => {
        if (ativo) setCarregando(false)
      })

    return () => { ativo = false }
  }, [])

  async function entrar(credenciais: LoginRequest) {
    const credencial = await login(credenciais)
    salvarCredencial(credencial)
    setToken(credencial.token)
    setSessao(credencial.usuario)
    return credencial.usuario
  }

  function sair() {
    removerCredencial()
    setToken(null)
    setSessao(null)
  }

  return (
    <AuthContext.Provider value={{ sessao, token, carregando, entrar, sair }}>
      {children}
    </AuthContext.Provider>
  )
}
