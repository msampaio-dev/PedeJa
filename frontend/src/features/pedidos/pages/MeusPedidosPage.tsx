import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Topo } from '../../../components/Topo'
import { ApiError } from '../../../services/api'
import { formatarPreco } from '../../../shared/formatadores'
import { useAuth } from '../../auth/context/useAuth'
import { listarMeusPedidos } from '../pedidoApi'
import { ROTULO_STATUS, formatarDataHora } from '../statusPedido'
import type { PedidoResumo } from '../types'
import styles from './Pedidos.module.css'

export function MeusPedidosPage() {
  const { token } = useAuth()
  const [pedidos, setPedidos] = useState<PedidoResumo[] | null>(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    if (!token) return
    listarMeusPedidos(token)
      .then((pagina) => setPedidos(pagina.conteudo))
      .catch((error) => setErro(error instanceof ApiError ? error.message : 'Não foi possível carregar seus pedidos.'))
  }, [token])

  return (
    <>
      <Topo />
      <main className={styles.pagina}>
        <h1>Meus pedidos</h1>
        {erro && <p className={styles.erro} role="alert">{erro}</p>}
        {!erro && !pedidos && <p role="status">Carregando...</p>}
        {pedidos?.length === 0 && (
          <p className={styles.meta}>Você ainda não fez pedidos. <Link to="/restaurantes">Ver restaurantes</Link></p>
        )}

        <ul className={styles.lista}>
          {pedidos?.map((pedido) => (
            <li key={pedido.id}>
              <Link className={styles.cartao} to={`/pedidos/${pedido.id}`}>
                <div>
                  <strong>{pedido.restauranteNome}</strong>
                  <span className={styles.meta}>Pedido #{pedido.id} · {formatarDataHora(pedido.criadoEm)}</span>
                </div>
                <div className={styles.direita}>
                  <span className={styles.status} data-status={pedido.status}>{ROTULO_STATUS[pedido.status]}</span>
                  <strong>{formatarPreco(pedido.total)}</strong>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      </main>
    </>
  )
}
