import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/context/useAuth'
import { Notificacoes } from '../features/notificacoes/Notificacoes'
import { useSacola } from '../features/sacola/useSacola'
import styles from './Topo.module.css'

export function Topo() {
  const navigate = useNavigate()
  const { sessao, carregando, sair } = useAuth()
  const { quantidadeTotal } = useSacola()
  const ehRestaurante = sessao?.perfil === 'RESTAURANTE'

  function encerrar() {
    sair()
    navigate('/entrar', { replace: true })
  }

  return (
    <header className={styles.topo}>
      <div className={styles.conteudo}>
        <Link className={styles.marca} to="/">PedeJá</Link>
        <nav className={styles.usuario}>
          {ehRestaurante && <Link className={styles.link} to="/restaurante/pedidos">Pedidos</Link>}
          {ehRestaurante && <Link className={styles.link} to="/restaurante">Cardápio</Link>}
          {sessao?.perfil === 'CLIENTE' && <Link className={styles.link} to="/pedidos">Meus pedidos</Link>}
          {!ehRestaurante && (
            <Link className={styles.link} to="/sacola">
              Sacola{quantidadeTotal > 0 && <span className={styles.contador}>{quantidadeTotal}</span>}
            </Link>
          )}
          {sessao && <Notificacoes />}
          {sessao && (
            <button className="botao botao-secundario" onClick={encerrar} type="button">Sair</button>
          )}
          {!sessao && !carregando && (
            <Link className="botao botao-secundario" to="/entrar">Entrar</Link>
          )}
        </nav>
      </div>
    </header>
  )
}
