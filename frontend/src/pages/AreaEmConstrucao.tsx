import { Topo } from '../components/Topo'

type Props = {
  titulo: string
  descricao: string
}

// Ocupa as rotas de cliente e de restaurante até as próximas fases trazerem as
// telas de verdade (catálogo na fase 2, painel de pedidos na fase 6).
export function AreaEmConstrucao({ titulo, descricao }: Props) {
  return (
    <>
      <Topo />
      <main style={{ width: 'min(100% - 32px, var(--content-width))', margin: '32px auto' }}>
        <h1>{titulo}</h1>
        <p style={{ color: 'var(--color-text-muted)' }}>{descricao}</p>
      </main>
    </>
  )
}
