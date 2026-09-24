import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { rotaInicial } from '../rotaInicial'
import type { PerfilUsuario } from '../types'

type Props = {
  children: ReactNode
  perfil: PerfilUsuario
}

export function RotaProtegida({ children, perfil }: Props) {
  const location = useLocation()
  const { sessao, carregando } = useAuth()

  if (carregando) {
    return <p role="status" style={{ padding: 24 }}>Carregando...</p>
  }

  if (!sessao) {
    return <Navigate to="/entrar" replace state={{ retorno: location.pathname }} />
  }

  // Restaurante que abre uma rota de cliente (ou o contrário) volta para a
  // própria área, em vez de ver uma tela de erro.
  if (sessao.perfil !== perfil) {
    return <Navigate to={rotaInicial(sessao.perfil)} replace />
  }

  return children
}
