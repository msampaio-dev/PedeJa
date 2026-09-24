import { useContext } from 'react'
import { SacolaContext } from './SacolaContext'

export function useSacola() {
  const context = useContext(SacolaContext)

  if (!context) {
    throw new Error('useSacola deve ser utilizado dentro de SacolaProvider')
  }

  return context
}
