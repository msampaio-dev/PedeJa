import { Component, type ErrorInfo, type ReactNode } from 'react'
import styles from './LimiteDeErro.module.css'

type Props = { children: ReactNode }
type Estado = { falhou: boolean }

/**
 * Última proteção da interface. Sem ela, um erro durante a renderização
 * desmonta a árvore inteira e deixa a tela em branco, sem explicação e sem
 * saída. Aqui a pessoa entende o que houve e consegue voltar.
 */
export class LimiteDeErro extends Component<Props, Estado> {
  state: Estado = { falhou: false }

  static getDerivedStateFromError(): Estado {
    return { falhou: true }
  }

  componentDidCatch(erro: Error, info: ErrorInfo) {
    // Sem serviço de monitoramento, o console é onde dá para investigar depois.
    console.error('Falha não tratada na interface:', erro, info.componentStack)
  }

  render() {
    if (!this.state.falhou) return this.props.children

    return (
      <main className={styles.pagina} role="alert">
        <div className={styles.cartao}>
          <span className={styles.marca}>PedeJá</span>
          <h1>Esta tela não pôde ser exibida.</h1>
          <p>
            Recarregar costuma resolver. Se o erro continuar, volte ao início e tente por outro caminho.
            Sua sacola e seu login continuam salvos.
          </p>
          <div className={styles.acoes}>
            <button className="botao" onClick={() => window.location.reload()} type="button">Recarregar a página</button>
            <a className="botao botao-secundario" href="/">Voltar ao início</a>
          </div>
        </div>
      </main>
    )
  }
}
