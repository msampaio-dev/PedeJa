import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Topo } from '../../../components/Topo'
import { ApiError } from '../../../services/api'
import {
  EMOJI_CATEGORIA,
  ROTULO_CATEGORIA,
  formatarPreco,
  formatarTaxaEntrega,
} from '../../../shared/formatadores'
import { buscarCardapio } from '../restauranteApi'
import type { CardapioPublico } from '../types'
import styles from './CardapioPage.module.css'

export function CardapioPage() {
  const { id } = useParams()
  const [cardapio, setCardapio] = useState<CardapioPublico | null>(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    let ativo = true
    buscarCardapio(Number(id))
      .then((dados) => { if (ativo) setCardapio(dados) })
      .catch((error) => {
        if (ativo) setErro(error instanceof ApiError ? error.message : 'Não foi possível carregar o cardápio.')
      })
    return () => { ativo = false }
  }, [id])

  return (
    <>
      <Topo />
      <main className={styles.pagina}>
        <Link className={styles.voltar} to="/restaurantes">← Restaurantes</Link>

        {erro && <p className={styles.erro} role="alert">{erro}</p>}
        {!erro && !cardapio && <p role="status">Carregando...</p>}

        {cardapio && (
          <>
            <header className={styles.cabecalho}>
              <span className={styles.logo} aria-hidden="true">{EMOJI_CATEGORIA[cardapio.restaurante.categoria]}</span>
              <div>
                <h1>{cardapio.restaurante.nome}</h1>
                {cardapio.restaurante.descricao && <p>{cardapio.restaurante.descricao}</p>}
                <p className={styles.meta}>
                  {ROTULO_CATEGORIA[cardapio.restaurante.categoria]}
                  {' · '}{cardapio.restaurante.tempoEntregaMinutos} min
                  {' · '}{formatarTaxaEntrega(cardapio.restaurante.taxaEntrega)}
                </p>
              </div>
            </header>

            {!cardapio.restaurante.aberto && (
              <p className={styles.aviso} role="status">
                Este restaurante está fechado agora. Dá para ver o cardápio, mas não para pedir.
              </p>
            )}

            <h2>Cardápio</h2>
            {cardapio.itens.length === 0 && <p className={styles.meta}>Nenhum item disponível.</p>}

            <ul className={styles.itens}>
              {cardapio.itens.map((item) => (
                <li className={styles.item} key={item.id}>
                  <div>
                    <strong>{item.nome}</strong>
                    {item.descricao && <p>{item.descricao}</p>}
                  </div>
                  <span className={styles.preco}>{formatarPreco(item.preco)}</span>
                </li>
              ))}
            </ul>
          </>
        )}
      </main>
    </>
  )
}
