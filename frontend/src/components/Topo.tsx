import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/context/useAuth'
import styles from './Topo.module.css'

export function Topo() {
  const navigate = useNavigate()
  const { sessao, sair } = useAuth()

  function encerrar() {
    sair()
    navigate('/entrar', { replace: true })
  }

  return (
    <header className={styles.topo}>
      <div className={styles.conteudo}>
        <Link className={styles.marca} to="/">PedeJá</Link>
        {sessao && (
          <div className={styles.usuario}>
            <span>Olá, {sessao.nome.split(' ')[0]}</span>
            <button className="botao botao-secundario" onClick={encerrar} type="button">Sair</button>
          </div>
        )}
      </div>
    </header>
  )
}
