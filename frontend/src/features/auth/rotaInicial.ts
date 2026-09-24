import type { PerfilUsuario } from './types'

export function rotaInicial(perfil: PerfilUsuario) {
  return perfil === 'RESTAURANTE' ? '/restaurante' : '/restaurantes'
}
