import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { AvisoServidor } from './components/AvisoServidor'
import { LimiteDeErro } from './components/LimiteDeErro'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <LimiteDeErro>
      <App />
      <AvisoServidor />
    </LimiteDeErro>
  </StrictMode>,
)
