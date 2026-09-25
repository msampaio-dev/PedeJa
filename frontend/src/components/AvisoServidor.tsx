import { useEffect, useState } from 'react'
import { ouvirServidor, servidorAcordando } from '../services/estadoServidor'
import styles from './AvisoServidor.module.css'

export function AvisoServidor() {
  const [acordando, setAcordando] = useState(servidorAcordando)

  useEffect(() => ouvirServidor(setAcordando), [])

  return (
    <div aria-live="polite" role="status">
      {acordando && (
        <p className={styles.aviso}>
          O servidor estava hibernando, porque roda num plano gratuito, e está acordando.
          A primeira resposta pode levar até um minuto.
        </p>
      )}
    </div>
  )
}
