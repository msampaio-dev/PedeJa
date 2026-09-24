import { type FormEvent, useEffect, useState } from 'react'
import { Topo } from '../../components/Topo'
import { ApiError } from '../../services/api'
import { CATEGORIAS, ROTULO_CATEGORIA, formatarPreco } from '../../shared/formatadores'
import { useAuth } from '../auth/context/useAuth'
import type { CategoriaRestaurante, DadosItemCardapio, ItemCardapio, Restaurante } from '../restaurantes/types'
import {
  atualizarItem,
  atualizarMeuRestaurante,
  buscarMeuRestaurante,
  criarItem,
  definirAbertura,
  listarMeusItens,
} from './meuRestauranteApi'
import styles from './PainelRestaurantePage.module.css'

const ITEM_VAZIO = { nome: '', descricao: '', preco: '', disponivel: true }

type FormItem = typeof ITEM_VAZIO

function mensagemDe(error: unknown, padrao: string) {
  return error instanceof ApiError ? error.message : padrao
}

export function PainelRestaurantePage() {
  const { token } = useAuth()
  const [restaurante, setRestaurante] = useState<Restaurante | null>(null)
  const [itens, setItens] = useState<ItemCardapio[]>([])
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')

  const [editandoId, setEditandoId] = useState<number | null>(null)
  const [formItem, setFormItem] = useState<FormItem>(ITEM_VAZIO)
  const [erroItem, setErroItem] = useState('')

  useEffect(() => {
    if (!token) return
    Promise.all([buscarMeuRestaurante(token), listarMeusItens(token)])
      .then(([dados, lista]) => {
        setRestaurante(dados)
        setItens(lista)
      })
      .catch((error) => setErro(mensagemDe(error, 'Não foi possível carregar o restaurante.')))
  }, [token])

  if (!token) return null

  async function alternarAbertura() {
    if (!restaurante || !token) return
    setErro('')
    try {
      setRestaurante(await definirAbertura(token, !restaurante.aberto))
    } catch (error) {
      setErro(mensagemDe(error, 'Não foi possível mudar o status.'))
    }
  }

  async function salvarDados(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!restaurante || !token) return
    const form = new FormData(event.currentTarget)
    setErro('')
    setAviso('')

    try {
      const atualizado = await atualizarMeuRestaurante(token, {
        nome: String(form.get('nome')),
        descricao: String(form.get('descricao')),
        categoria: form.get('categoria') as CategoriaRestaurante,
        taxaEntrega: Number(form.get('taxaEntrega')),
        tempoEntregaMinutos: Number(form.get('tempoEntregaMinutos')),
      })
      setRestaurante(atualizado)
      setAviso('Dados salvos.')
    } catch (error) {
      setErro(mensagemDe(error, 'Não foi possível salvar os dados.'))
    }
  }

  function editar(item: ItemCardapio) {
    setEditandoId(item.id)
    setErroItem('')
    setFormItem({
      nome: item.nome,
      descricao: item.descricao ?? '',
      preco: item.preco.toFixed(2),
      disponivel: item.disponivel,
    })
  }

  function cancelarEdicao() {
    setEditandoId(null)
    setErroItem('')
    setFormItem(ITEM_VAZIO)
  }

  async function salvarItem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) return
    setErroItem('')

    const dados: DadosItemCardapio = {
      nome: formItem.nome.trim(),
      descricao: formItem.descricao.trim(),
      preco: Number(formItem.preco),
      disponivel: formItem.disponivel,
    }

    try {
      if (editandoId) {
        const salvo = await atualizarItem(token, editandoId, dados)
        setItens((atuais) => atuais.map((i) => (i.id === salvo.id ? salvo : i)))
      } else {
        const criado = await criarItem(token, dados)
        setItens((atuais) => [...atuais, criado].sort((a, b) => a.nome.localeCompare(b.nome)))
      }
      cancelarEdicao()
    } catch (error) {
      setErroItem(mensagemDe(error, 'Não foi possível salvar o item.'))
    }
  }

  return (
    <>
      <Topo />
      <main className={styles.pagina}>
        {erro && <p className={styles.erro} role="alert">{erro}</p>}
        {!restaurante && !erro && <p role="status">Carregando...</p>}

        {restaurante && (
          <>
            <header className={styles.cabecalho}>
              <div>
                <h1>{restaurante.nome}</h1>
                <span className={restaurante.aberto ? styles.aberto : styles.fechado}>
                  {restaurante.aberto ? 'Aberto' : 'Fechado'}
                </span>
              </div>
              <button className="botao" onClick={alternarAbertura} type="button">
                {restaurante.aberto ? 'Fechar restaurante' : 'Abrir restaurante'}
              </button>
            </header>

            <div className={styles.colunas}>
              <section className={styles.bloco}>
                <h2>Cardápio</h2>
                <ul className={styles.itens}>
                  {itens.map((item) => (
                    <li key={item.id} className={item.disponivel ? '' : styles.indisponivel}>
                      <div>
                        <strong>{item.nome}</strong>
                        <span>{formatarPreco(item.preco)}{item.disponivel ? '' : ' · indisponível'}</span>
                      </div>
                      <button className="botao botao-secundario" onClick={() => editar(item)} type="button">
                        Editar
                      </button>
                    </li>
                  ))}
                  {itens.length === 0 && <li>Nenhum item ainda. Cadastre o primeiro ao lado.</li>}
                </ul>
              </section>

              <section className={styles.bloco}>
                <h2>{editandoId ? 'Editar item' : 'Novo item'}</h2>
                <form className={styles.formulario} onSubmit={salvarItem}>
                  {erroItem && <p className={styles.erro} role="alert">{erroItem}</p>}
                  <label>
                    Nome
                    <input maxLength={120} required value={formItem.nome}
                      onChange={(e) => setFormItem({ ...formItem, nome: e.target.value })} />
                  </label>
                  <label>
                    Descrição
                    <input maxLength={500} value={formItem.descricao}
                      onChange={(e) => setFormItem({ ...formItem, descricao: e.target.value })} />
                  </label>
                  <label>
                    Preço (R$)
                    <input min="0.01" required step="0.01" type="number" value={formItem.preco}
                      onChange={(e) => setFormItem({ ...formItem, preco: e.target.value })} />
                  </label>
                  <label className={styles.checkbox}>
                    <input checked={formItem.disponivel} type="checkbox"
                      onChange={(e) => setFormItem({ ...formItem, disponivel: e.target.checked })} />
                    Disponível para pedido
                  </label>
                  <div className={styles.acoes}>
                    <button className="botao" type="submit">{editandoId ? 'Salvar item' : 'Adicionar item'}</button>
                    {editandoId && (
                      <button className="botao botao-secundario" onClick={cancelarEdicao} type="button">Cancelar</button>
                    )}
                  </div>
                </form>

                <h2>Dados do restaurante</h2>
                {aviso && <p className={styles.sucesso} role="status">{aviso}</p>}
                {/* key força o formulário a recomeçar com os valores salvos. */}
                <form className={styles.formulario} key={JSON.stringify(restaurante)} onSubmit={salvarDados}>
                  <label>
                    Nome
                    <input defaultValue={restaurante.nome} maxLength={120} name="nome" required />
                  </label>
                  <label>
                    Descrição
                    <input defaultValue={restaurante.descricao ?? ''} maxLength={500} name="descricao" />
                  </label>
                  <label>
                    Categoria
                    <select defaultValue={restaurante.categoria} name="categoria">
                      {CATEGORIAS.map((c) => <option key={c} value={c}>{ROTULO_CATEGORIA[c]}</option>)}
                    </select>
                  </label>
                  <label>
                    Taxa de entrega (R$)
                    <input defaultValue={restaurante.taxaEntrega.toFixed(2)} min="0" name="taxaEntrega"
                      required step="0.01" type="number" />
                  </label>
                  <label>
                    Tempo de entrega (minutos)
                    <input defaultValue={restaurante.tempoEntregaMinutos} max="180" min="5"
                      name="tempoEntregaMinutos" required type="number" />
                  </label>
                  <button className="botao" type="submit">Salvar dados</button>
                </form>
              </section>
            </div>
          </>
        )}
      </main>
    </>
  )
}
