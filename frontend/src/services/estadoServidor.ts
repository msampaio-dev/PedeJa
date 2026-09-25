/**
 * Diz se alguma requisição está demorando além do normal.
 *
 * A API roda no plano gratuito do Render, que hiberna depois de 15 minutos sem
 * acesso e leva perto de um minuto para acordar. Sem aviso, a tela parada
 * parece defeito e a pessoa vai embora antes de a resposta chegar.
 */
export const LIMITE_ESPERA_NORMAL_MS = 4000

type Ouvinte = (acordando: boolean) => void

let lentas = 0
const ouvintes = new Set<Ouvinte>()

function avisar() {
  ouvintes.forEach((ouvinte) => ouvinte(lentas > 0))
}

/**
 * Marca o início de uma requisição. Se ela passar do limite, conta como lenta
 * até a função devolvida ser chamada.
 */
export function acompanharRequisicao(): () => void {
  let marcadaComoLenta = false
  const temporizador = setTimeout(() => {
    marcadaComoLenta = true
    lentas++
    avisar()
  }, LIMITE_ESPERA_NORMAL_MS)

  return () => {
    clearTimeout(temporizador)
    if (marcadaComoLenta) {
      lentas--
      avisar()
    }
  }
}

export function servidorAcordando() {
  return lentas > 0
}

export function ouvirServidor(ouvinte: Ouvinte) {
  ouvintes.add(ouvinte)
  return () => { ouvintes.delete(ouvinte) }
}
