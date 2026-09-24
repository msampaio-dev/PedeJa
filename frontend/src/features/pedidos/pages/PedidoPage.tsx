import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Topo } from '../../../components/Topo'
import { ApiError } from '../../../services/api'
import { formatarPreco } from '../../../shared/formatadores'
import { useAuth } from '../../auth/context/useAuth'
import { buscarPedido, cancelarPedido } from '../pedidoApi'
import { ROTULO_STATUS, formatarDataHora } from '../statusPedido'
import type { Pedido } from '../types'
import styles from './Pedidos.module.css'

function mensagemDe(error: unknown, padrao: string) {
  return error instanceof ApiError ? error.message : padrao
}

export function PedidoPage() {
  const { id } = useParams()
  const { token } = useAuth()
  const [pedido, setPedido] = useState<Pedido | null>(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    if (!token) return
    buscarPedido(token, Number(id))
      .then(setPedido)
      .catch((error) => setErro(mensagemDe(error, 'Não foi possível carregar o pedido.')))
  }, [token, id])

  async function cancelar() {
    if (!token || !pedido) return
    if (!window.confirm('Cancelar este pedido?')) return
    setErro('')
    try {
      setPedido(await cancelarPedido(token, pedido.id))
    } catch (error) {
      setErro(mensagemDe(error, 'Não foi possível cancelar o pedido.'))
    }
  }

  return (
    <>
      <Topo />
      <main className={styles.pagina}>
        <Link className={styles.voltar} to="/pedidos">← Meus pedidos</Link>
        {erro && <p className={styles.erro} role="alert">{erro}</p>}
        {!erro && !pedido && <p role="status">Carregando...</p>}

        {pedido && (
          <>
            <header className={styles.cabecalho}>
              <div>
                <h1>Pedido #{pedido.id}</h1>
                <span className={styles.meta}>
                  <Link to={`/restaurantes/${pedido.restaurante.id}`}>{pedido.restaurante.nome}</Link>
                  {' · '}{formatarDataHora(pedido.criadoEm)}
                </span>
              </div>
              <span className={styles.status} data-status={pedido.status}>{ROTULO_STATUS[pedido.status]}</span>
            </header>

            <section className={styles.bloco}>
              <h2>Itens</h2>
              <ul className={styles.itens}>
                {pedido.itens.map((item) => (
                  <li key={item.itemCardapioId}>
                    <span>{item.quantidade}× {item.nome}</span>
                    <span>{formatarPreco(item.subtotal)}</span>
                  </li>
                ))}
              </ul>
              <dl className={styles.totais}>
                <div><dt>Subtotal</dt><dd>{formatarPreco(pedido.subtotal)}</dd></div>
                <div><dt>Taxa de entrega</dt><dd>{pedido.taxaEntrega === 0 ? 'Grátis' : formatarPreco(pedido.taxaEntrega)}</dd></div>
                <div className={styles.total}><dt>Total</dt><dd>{formatarPreco(pedido.total)}</dd></div>
              </dl>
              <p className={styles.meta}>Entrega em {pedido.enderecoEntrega}</p>
              {pedido.observacao && <p className={styles.meta}>Observação: {pedido.observacao}</p>}
            </section>

            <section className={styles.bloco}>
              <h2>Acompanhamento</h2>
              <ol className={styles.linhaDoTempo}>
                {pedido.historico.map((evento) => (
                  <li key={`${evento.status}-${evento.ocorridoEm}`}>
                    <strong>{ROTULO_STATUS[evento.status]}</strong>
                    <span className={styles.meta}>{formatarDataHora(evento.ocorridoEm)}</span>
                  </li>
                ))}
              </ol>
            </section>

            {pedido.status === 'AGUARDANDO_PAGAMENTO' && (
              <button className="botao botao-secundario" onClick={cancelar} type="button">Cancelar pedido</button>
            )}
          </>
        )}
      </main>
    </>
  )
}
