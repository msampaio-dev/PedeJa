import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { RotaProtegida } from './features/auth/components/RotaProtegida'
import { AuthProvider } from './features/auth/context/AuthProvider'
import { useAuth } from './features/auth/context/useAuth'
import { CadastroPage } from './features/auth/pages/CadastroPage'
import { LoginPage } from './features/auth/pages/LoginPage'
import { rotaInicial } from './features/auth/rotaInicial'
import { PainelRestaurantePage } from './features/painel/PainelRestaurantePage'
import { CadastroRestaurantePage } from './features/restaurantes/pages/CadastroRestaurantePage'
import { CardapioPage } from './features/restaurantes/pages/CardapioPage'
import { RestaurantesPage } from './features/restaurantes/pages/RestaurantesPage'

// Visitante e cliente caem na lista de restaurantes; o dono, no painel.
function Inicio() {
  const { sessao, carregando } = useAuth()
  if (carregando) return null
  return <Navigate to={sessao ? rotaInicial(sessao.perfil) : '/restaurantes'} replace />
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Inicio />} />
          <Route path="/entrar" element={<LoginPage />} />
          <Route path="/cadastro" element={<CadastroPage />} />
          <Route path="/cadastro/restaurante" element={<CadastroRestaurantePage />} />
          <Route path="/restaurantes" element={<RestaurantesPage />} />
          <Route path="/restaurantes/:id" element={<CardapioPage />} />
          <Route path="/restaurante" element={
            <RotaProtegida perfil="RESTAURANTE"><PainelRestaurantePage /></RotaProtegida>
          } />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
