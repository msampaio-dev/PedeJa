import { type FormEvent, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Topo } from '../../components/Topo'
import { ApiError } from '../../services/api'
import { formatarPreco } from '../../shared/formatadores'
import { useAuth } from '../auth/context/useAuth'
import { fazerPedido } from '../pedidos/pedidoApi'
import styles from './SacolaPage.module.css'
import { useSacola } from './useSacola'

export function SacolaPage() {
  const navigate = useNavigate()
  const { sessao, token } = useAuth()
  const sacola = useSacola()
  const [endereco, setEndereco] = useState('')
  const [observacao, setObservacao] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  // Uma chave por conteúdo de sacola. Reenviar a mesma sacola (clique duplo,
  // nova tentativa depois de erro de rede) usa a mesma chave, e a API devolve o
  // pedido já criado. Mudou a sacola, é outro pedido e a chave muda junto.
  const conteudo = JSON.stringify(sacola.itens)
  const chaveIdempotencia = useMemo(() => crypto.randomUUID(), [conteudo])

  const taxaEntrega = sacola.restaurante?.taxaEntrega ?? 0
  const total = Math.round((sacola.subtotal + taxaEntrega) * 100) / 100

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!sacola.restaurante) return

    if (!sessao || !token) {
      navigate('/entrar', { state: { retorno: '/sacola' } })
      return
    }

    if (sessao.perfil !== 'CLIENTE') {
      setErro('Contas de restaurante não fazem pedidos. Entre com uma conta de cliente.')
      return
    }

    setErro('')
    setEnviando(true)

    try {
      const pedido = await fazerPedido(token, {
        restauranteId: sacola.restaurante.id,
        itens: sacola.itens.map((i) => ({ itemId: i.itemId, quantidade: i.quantidade })),
        enderecoEntrega: endereco.trim(),
        observacao: observacao.trim(),
      }, chaveIdempotencia)
      sacola.esvaziar()
      navigate(`/pedidos/${pedido.id}`, { replace: true })
    } catch (error) {
      setErro(error instanceof ApiError ? error.message : 'Não foi possível enviar o pedido.')
    } finally {
      setEnviando(false)
    }
  }

  if (!sacola.restaurante) {
    return (
      <>
        <Topo />
        <main className={styles.pagina}>
          <h1>Sacola</h1>
          <p className={styles.meta}>Sua sacola está vazia.</p>
          <Link className="botao" to="/restaurantes">Ver restaurantes</Link>
        </main>
      </>
    )
  }

  return (
    <>
      <Topo />
      <main className={styles.pagina}>
        <h1>Sacola</h1>
        <p className={styles.meta}>
          Pedido em <Link to={`/restaurantes/${sacola.restaurante.id}`}>{sacola.restaurante.nome}</Link>
        </p>

        <ul className={styles.itens}>
          {sacola.itens.map((item) => (
            <li key={item.itemId}>
              <div>
                <strong>{item.nome}</strong>
                <span className={styles.meta}>{formatarPreco(item.preco)}</span>
              </div>
              <div className={styles.quantidade}>
                <button aria-label={`Diminuir ${item.nome}`} onClick={() => sacola.alterarQuantidade(item.itemId, -1)} type="button">−</button>
                <span aria-label={`Quantidade de ${item.nome}`}>{item.quantidade}</span>
                <button aria-label={`Aumentar ${item.nome}`} onClick={() => sacola.alterarQuantidade(item.itemId, 1)} type="button">+</button>
              </div>
            </li>
          ))}
        </ul>

        <dl className={styles.totais}>
          <div><dt>Subtotal</dt><dd>{formatarPreco(sacola.subtotal)}</dd></div>
          <div><dt>Taxa de entrega</dt><dd>{taxaEntrega === 0 ? 'Grátis' : formatarPreco(taxaEntrega)}</dd></div>
          <div className={styles.total}><dt>Total</dt><dd>{formatarPreco(total)}</dd></div>
        </dl>

        <form className={styles.formulario} onSubmit={enviar}>
          {erro && <p className={styles.erro} role="alert">{erro}</p>}
          <label>
            Endereço de entrega
            <input maxLength={300} onChange={(e) => setEndereco(e.target.value)} placeholder="Rua, número, complemento"
              required value={endereco} />
          </label>
          <label>
            Observação (opcional)
            <input maxLength={300} onChange={(e) => setObservacao(e.target.value)} placeholder="Ex.: sem cebola"
              value={observacao} />
          </label>
          <button className="botao" disabled={enviando} type="submit">
            {!sessao ? 'Entrar para fazer o pedido' : enviando ? 'Enviando...' : `Fazer pedido · ${formatarPreco(total)}`}
          </button>
        </form>
      </main>
    </>
  )
}
