import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { RotaProtegida } from './features/auth/components/RotaProtegida'
import { AuthProvider } from './features/auth/context/AuthProvider'
import { useAuth } from './features/auth/context/useAuth'
import { CadastroPage } from './features/auth/pages/CadastroPage'
import { LoginPage } from './features/auth/pages/LoginPage'
import { rotaInicial } from './features/auth/rotaInicial'
import { AreaEmConstrucao } from './pages/AreaEmConstrucao'

function Inicio() {
  const { sessao, carregando } = useAuth()
  if (carregando) return null
  return <Navigate to={sessao ? rotaInicial(sessao.perfil) : '/entrar'} replace />
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Inicio />} />
          <Route path="/entrar" element={<LoginPage />} />
          <Route path="/cadastro" element={<CadastroPage />} />
          <Route path="/restaurantes" element={
            <RotaProtegida perfil="CLIENTE">
              <AreaEmConstrucao titulo="Restaurantes" descricao="A lista de restaurantes chega na fase 2." />
            </RotaProtegida>
          } />
          <Route path="/restaurante" element={
            <RotaProtegida perfil="RESTAURANTE">
              <AreaEmConstrucao titulo="Painel do restaurante" descricao="Cardápio e pedidos chegam nas próximas fases." />
            </RotaProtegida>
          } />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
