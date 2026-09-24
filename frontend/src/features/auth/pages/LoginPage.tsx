import { type FormEvent, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../../../services/api'
import { useAuth } from '../context/useAuth'
import { rotaInicial } from '../rotaInicial'
import type { LoginRequest } from '../types'
import styles from './AuthPage.module.css'

// Contas criadas pela carga de demonstração da API (db/devdata). Existem para
// quem chega pelo portfólio ver os dois lados sem se cadastrar.
const DEMO_CLIENTE = { email: 'cliente@demo.pedeja.local', senha: 'Demo123!' }
const DEMO_RESTAURANTE = { email: 'restaurante@demo.pedeja.local', senha: 'Demo123!' }

type EstadoNavegacao = { retorno?: string; cadastroConcluido?: boolean } | null

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const estado = location.state as EstadoNavegacao
  const { entrar } = useAuth()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function acessar(credenciais: LoginRequest) {
    setErro('')
    setEnviando(true)

    try {
      const sessao = await entrar(credenciais)
      navigate(estado?.retorno || rotaInicial(sessao.perfil), { replace: true })
    } catch (error) {
      setErro(error instanceof ApiError ? error.message : 'Não foi possível entrar.')
    } finally {
      setEnviando(false)
    }
  }

  function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    acessar({ email: email.trim(), senha })
  }

  return (
    <main className={styles.pagina}>
      <section className={styles.cartao}>
        <Link className={styles.marca} to="/">PedeJá</Link>
        <h1>Entrar</h1>

        <form className={styles.formulario} onSubmit={enviar}>
          {estado?.cadastroConcluido && (
            <div className={styles.sucesso} role="status">Conta criada. Agora é só entrar.</div>
          )}
          {erro && <div className={styles.erro} role="alert">{erro}</div>}

          <label>
            E-mail
            <input autoComplete="email" onChange={(e) => setEmail(e.target.value)}
              required type="email" value={email} />
          </label>
          <label>
            Senha
            <input autoComplete="current-password" onChange={(e) => setSenha(e.target.value)}
              required type="password" value={senha} />
          </label>

          <button className="botao" disabled={enviando} type="submit">
            {enviando ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        <div className={styles.demonstracao}>
          <p>Quer só olhar? Entre com uma conta de demonstração:</p>
          <div>
            <button className="botao botao-secundario" disabled={enviando} type="button"
              onClick={() => acessar(DEMO_CLIENTE)}>
              Cliente
            </button>
            <button className="botao botao-secundario" disabled={enviando} type="button"
              onClick={() => acessar(DEMO_RESTAURANTE)}>
              Restaurante
            </button>
          </div>
        </div>

        <p className={styles.rodape}>Ainda não tem conta? <Link to="/cadastro">Criar conta</Link></p>
      </section>
    </main>
  )
}
