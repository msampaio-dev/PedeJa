import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { RotaProtegida } from './features/auth/components/RotaProtegida'
import { AuthProvider } from './features/auth/context/AuthProvider'
import { useAuth } from './features/auth/context/useAuth'
import { CadastroPage } from './features/auth/pages/CadastroPage'
import { LoginPage } from './features/auth/pages/LoginPage'
import { rotaInicial } from './features/auth/rotaInicial'
import { EventosProvider } from './features/eventos/EventosProvider'
import { PainelRestaurantePage } from './features/painel/PainelRestaurantePage'
import { PedidosRestaurantePage } from './features/painel/PedidosRestaurantePage'
import { MeusPedidosPage } from './features/pedidos/pages/MeusPedidosPage'
import { PedidoPage } from './features/pedidos/pages/PedidoPage'
import { CadastroRestaurantePage } from './features/restaurantes/pages/CadastroRestaurantePage'
import { CardapioPage } from './features/restaurantes/pages/CardapioPage'
import { RestaurantesPage } from './features/restaurantes/pages/RestaurantesPage'
import { SacolaPage } from './features/sacola/SacolaPage'
import { SacolaProvider } from './features/sacola/SacolaProvider'

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
        <EventosProvider>
          <SacolaProvider>
            <Routes>
              <Route path="/" element={<Inicio />} />
              <Route path="/entrar" element={<LoginPage />} />
              <Route path="/cadastro" element={<CadastroPage />} />
              <Route path="/cadastro/restaurante" element={<CadastroRestaurantePage />} />
              <Route path="/restaurantes" element={<RestaurantesPage />} />
              <Route path="/restaurantes/:id" element={<CardapioPage />} />
              <Route path="/sacola" element={<SacolaPage />} />
              <Route path="/pedidos" element={
                <RotaProtegida perfil="CLIENTE"><MeusPedidosPage /></RotaProtegida>
              } />
              <Route path="/pedidos/:id" element={
                <RotaProtegida perfil="CLIENTE"><PedidoPage /></RotaProtegida>
              } />
              <Route path="/restaurante" element={
                <RotaProtegida perfil="RESTAURANTE"><PainelRestaurantePage /></RotaProtegida>
              } />
              <Route path="/restaurante/pedidos" element={
                <RotaProtegida perfil="RESTAURANTE"><PedidosRestaurantePage /></RotaProtegida>
              } />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </SacolaProvider>
        </EventosProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
