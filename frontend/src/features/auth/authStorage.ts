import type { LoginResponse } from './types'

const CHAVE = 'pedeja:credencial'

// localStorage mantém o login entre abas e depois de fechar o navegador, como
// num app de delivery. O token expira em 2 horas, o que limita o estrago se
// alguém usar o mesmo computador depois.
export function salvarCredencial(credencial: LoginResponse) {
  localStorage.setItem(CHAVE, JSON.stringify(credencial))
}

export function lerCredencial(): LoginResponse | null {
  const valor = localStorage.getItem(CHAVE)
  if (!valor) return null

  try {
    const credencial = JSON.parse(valor) as LoginResponse
    if (new Date(credencial.expiraEm).getTime() <= Date.now()) {
      removerCredencial()
      return null
    }
    return credencial
  } catch {
    removerCredencial()
    return null
  }
}

export function removerCredencial() {
  localStorage.removeItem(CHAVE)
}
