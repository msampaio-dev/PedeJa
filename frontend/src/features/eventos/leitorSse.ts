export type EventoSse = {
  nome: string
  dados: string
}

/**
 * Separa um trecho de texto SSE em eventos completos. Devolve também o resto
 * incompleto, que fica esperando o próximo pedaço do fluxo: a rede entrega os
 * bytes em pedaços que não respeitam o fim do evento.
 *
 * Formato: linhas "event:" e "data:", e uma linha em branco fecha o evento.
 * Linhas que começam com ":" são comentários (o ping do servidor).
 */
export function separarEventos(buffer: string): { eventos: EventoSse[]; resto: string } {
  const normalizado = buffer.replace(/\r\n/g, '\n')
  const blocos = normalizado.split('\n\n')
  const resto = blocos.pop() ?? ''
  const eventos: EventoSse[] = []

  for (const bloco of blocos) {
    let nome = 'message'
    const dados: string[] = []

    for (const linha of bloco.split('\n')) {
      if (linha.startsWith(':')) continue
      const separador = linha.indexOf(':')
      const campo = separador === -1 ? linha : linha.slice(0, separador)
      const valor = separador === -1 ? '' : linha.slice(separador + 1).replace(/^ /, '')

      if (campo === 'event') nome = valor
      if (campo === 'data') dados.push(valor)
    }

    if (dados.length > 0) eventos.push({ nome, dados: dados.join('\n') })
  }

  return { eventos, resto }
}

/**
 * Abre a conexão SSE com fetch, e não com EventSource: o EventSource não manda
 * o header Authorization, e o token na URL apareceria em log de servidor.
 * A promessa termina quando o servidor fecha o fluxo ou o sinal é abortado.
 */
export async function escutarEventos(
  url: string,
  token: string,
  aoReceber: (evento: EventoSse) => void,
  sinal: AbortSignal,
) {
  const resposta = await fetch(url, {
    headers: { Accept: 'text/event-stream', Authorization: `Bearer ${token}` },
    signal: sinal,
  })

  if (!resposta.ok || !resposta.body) {
    throw new Error(`Conexão de eventos recusada (${resposta.status})`)
  }

  const leitor = resposta.body.pipeThrough(new TextDecoderStream()).getReader()
  let buffer = ''

  for (;;) {
    const { value, done } = await leitor.read()
    if (done) return
    const { eventos, resto } = separarEventos(buffer + value)
    buffer = resto
    eventos.forEach(aoReceber)
  }
}
