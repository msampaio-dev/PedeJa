import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Topo } from '../../../components/Topo'
import { ApiError } from '../../../services/api'
import {
  CATEGORIAS,
  EMOJI_CATEGORIA,
  ROTULO_CATEGORIA,
  formatarTaxaEntrega,
} from '../../../shared/formatadores'
import { listarRestaurantes } from '../restauranteApi'
import type { CategoriaRestaurante, Restaurante } from '../types'
import styles from './RestaurantesPage.module.css'

// Espera o usuário parar de digitar antes de consultar a API, em vez de
// disparar uma requisição por tecla.
const ESPERA_BUSCA_MS = 300

export function RestaurantesPage() {
  const [categoria, setCategoria] = useState<CategoriaRestaurante | null>(null)
  const [busca, setBusca] = useState('')
  const [restaurantes, setRestaurantes] = useState<Restaurante[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  useEffect(() => {
    let ativo = true

    const espera = setTimeout(() => {
      setCarregando(true)
      setErro('')
      listarRestaurantes({ categoria, busca })
        .then((pagina) => { if (ativo) setRestaurantes(pagina.conteudo) })
        .catch((error) => {
          if (ativo) setErro(error instanceof ApiError ? error.message : 'Não foi possível carregar os restaurantes.')
        })
        .finally(() => { if (ativo) setCarregando(false) })
    }, ESPERA_BUSCA_MS)

    // Uma resposta antiga que chegue depois de o filtro mudar é descartada.
    return () => {
      ativo = false
      clearTimeout(espera)
    }
  }, [categoria, busca])

  return (
    <>
      <Topo />
      <main className={styles.pagina}>
        <input
          aria-label="Buscar restaurante"
          className={styles.busca}
          onChange={(e) => setBusca(e.target.value)}
          placeholder="Buscar restaurante"
          type="search"
          value={busca}
        />

        <div className={styles.categorias} role="group" aria-label="Categorias">
          {CATEGORIAS.map((c) => (
            <button
              aria-pressed={categoria === c}
              className={styles.categoria}
              key={c}
              onClick={() => setCategoria(categoria === c ? null : c)}
              type="button"
            >
              <span aria-hidden="true">{EMOJI_CATEGORIA[c]}</span>
              {ROTULO_CATEGORIA[c]}
            </button>
          ))}
        </div>

        <h1>Restaurantes</h1>

        {erro && <p className={styles.erro} role="alert">{erro}</p>}
        {!erro && !carregando && restaurantes.length === 0 && (
          <p className={styles.vazio}>Nenhum restaurante encontrado.</p>
        )}

        <ul className={styles.lista} aria-busy={carregando}>
          {restaurantes.map((r) => (
            <li key={r.id}>
              <Link className={`${styles.cartao} ${r.aberto ? '' : styles.fechado}`} to={`/restaurantes/${r.id}`}>
                <span className={styles.logo} aria-hidden="true">{EMOJI_CATEGORIA[r.categoria]}</span>
                <div>
                  <strong>{r.nome}</strong>
                  <span className={styles.meta}>
                    {ROTULO_CATEGORIA[r.categoria]} · {r.tempoEntregaMinutos} min
                  </span>
                  <span className={styles.meta}>
                    {r.aberto ? formatarTaxaEntrega(r.taxaEntrega) : 'Fechado agora'}
                  </span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      </main>
    </>
  )
}
