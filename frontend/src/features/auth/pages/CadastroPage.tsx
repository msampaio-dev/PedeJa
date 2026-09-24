import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, type CampoInvalido } from '../../../services/api'
import { cadastrarCliente } from '../authApi'
import styles from './AuthPage.module.css'

export function CadastroPage() {
  const navigate = useNavigate()
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [campos, setCampos] = useState<CampoInvalido[]>([])
  const [enviando, setEnviando] = useState(false)

  function erroDoCampo(campo: string) {
    return campos.find((c) => c.campo === campo)?.mensagem
  }

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErro('')
    setCampos([])
    setEnviando(true)

    try {
      await cadastrarCliente({ nome: nome.trim(), email: email.trim(), senha })
      navigate('/entrar', { state: { cadastroConcluido: true } })
    } catch (error) {
      if (error instanceof ApiError) {
        setErro(error.message)
        setCampos(error.campos)
      } else {
        setErro('Não foi possível criar a conta.')
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className={styles.pagina}>
      <section className={styles.cartao}>
        <Link className={styles.marca} to="/">PedeJá</Link>
        <h1>Criar conta</h1>

        <form className={styles.formulario} onSubmit={enviar}>
          {erro && <div className={styles.erro} role="alert">{erro}</div>}

          <label>
            Nome
            <input autoComplete="name" maxLength={120} onChange={(e) => setNome(e.target.value)}
              required value={nome} />
            {erroDoCampo('nome') && <span className={styles.campoErro}>{erroDoCampo('nome')}</span>}
          </label>
          <label>
            E-mail
            <input autoComplete="email" maxLength={254} onChange={(e) => setEmail(e.target.value)}
              required type="email" value={email} />
            {erroDoCampo('email') && <span className={styles.campoErro}>{erroDoCampo('email')}</span>}
          </label>
          <label>
            Senha
            <input autoComplete="new-password" minLength={6} maxLength={72}
              onChange={(e) => setSenha(e.target.value)} required type="password" value={senha} />
            {erroDoCampo('senha') && <span className={styles.campoErro}>{erroDoCampo('senha')}</span>}
          </label>

          <button className="botao" disabled={enviando} type="submit">
            {enviando ? 'Criando...' : 'Criar conta'}
          </button>
        </form>

        <p className={styles.rodape}>Já tem conta? <Link to="/entrar">Entrar</Link></p>
      </section>
    </main>
  )
}
