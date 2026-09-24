import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, type CampoInvalido } from '../../../services/api'
import { CATEGORIAS, ROTULO_CATEGORIA } from '../../../shared/formatadores'
import styles from '../../auth/pages/AuthPage.module.css'
import { cadastrarRestaurante } from '../restauranteApi'
import type { CategoriaRestaurante } from '../types'

export function CadastroRestaurantePage() {
  const navigate = useNavigate()
  const [nomeResponsavel, setNomeResponsavel] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [nome, setNome] = useState('')
  const [categoria, setCategoria] = useState<CategoriaRestaurante>('PIZZA')
  const [taxaEntrega, setTaxaEntrega] = useState('5.00')
  const [tempoEntrega, setTempoEntrega] = useState('40')
  const [erro, setErro] = useState('')
  const [campos, setCampos] = useState<CampoInvalido[]>([])
  const [enviando, setEnviando] = useState(false)

  // A API devolve campos aninhados como "restaurante.nome".
  function erroDoCampo(campo: string) {
    return campos.find((c) => c.campo === campo)?.mensagem
  }

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErro('')
    setCampos([])
    setEnviando(true)

    try {
      await cadastrarRestaurante({
        nomeResponsavel: nomeResponsavel.trim(),
        email: email.trim(),
        senha,
        restaurante: {
          nome: nome.trim(),
          descricao: '',
          categoria,
          taxaEntrega: Number(taxaEntrega),
          tempoEntregaMinutos: Number(tempoEntrega),
        },
      })
      navigate('/entrar', { state: { cadastroConcluido: true } })
    } catch (error) {
      if (error instanceof ApiError) {
        setErro(error.message)
        setCampos(error.campos)
      } else {
        setErro('Não foi possível cadastrar o restaurante.')
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className={styles.pagina}>
      <section className={styles.cartao}>
        <Link className={styles.marca} to="/">PedeJá</Link>
        <h1>Cadastrar restaurante</h1>

        <form className={styles.formulario} onSubmit={enviar}>
          {erro && <div className={styles.erro} role="alert">{erro}</div>}

          <label>
            Seu nome
            <input autoComplete="name" maxLength={120} onChange={(e) => setNomeResponsavel(e.target.value)}
              required value={nomeResponsavel} />
            {erroDoCampo('nomeResponsavel') && <span className={styles.campoErro}>{erroDoCampo('nomeResponsavel')}</span>}
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
          <label>
            Nome do restaurante
            <input maxLength={120} onChange={(e) => setNome(e.target.value)} required value={nome} />
            {erroDoCampo('restaurante.nome') && <span className={styles.campoErro}>{erroDoCampo('restaurante.nome')}</span>}
          </label>
          <label>
            Categoria
            <select onChange={(e) => setCategoria(e.target.value as CategoriaRestaurante)} value={categoria}>
              {CATEGORIAS.map((c) => <option key={c} value={c}>{ROTULO_CATEGORIA[c]}</option>)}
            </select>
          </label>
          <label>
            Taxa de entrega (R$)
            <input min="0" onChange={(e) => setTaxaEntrega(e.target.value)} required step="0.01"
              type="number" value={taxaEntrega} />
            {erroDoCampo('restaurante.taxaEntrega') && <span className={styles.campoErro}>{erroDoCampo('restaurante.taxaEntrega')}</span>}
          </label>
          <label>
            Tempo de entrega (minutos)
            <input max="180" min="5" onChange={(e) => setTempoEntrega(e.target.value)} required
              type="number" value={tempoEntrega} />
            {erroDoCampo('restaurante.tempoEntregaMinutos') && <span className={styles.campoErro}>{erroDoCampo('restaurante.tempoEntregaMinutos')}</span>}
          </label>

          <button className="botao" disabled={enviando} type="submit">
            {enviando ? 'Cadastrando...' : 'Cadastrar restaurante'}
          </button>
        </form>

        <p className={styles.rodape}>Quer pedir comida? <Link to="/cadastro">Criar conta de cliente</Link></p>
      </section>
    </main>
  )
}
