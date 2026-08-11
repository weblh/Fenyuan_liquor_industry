import styles from './Footer.module.css'

export default function Footer() {
  return (
    <footer className={styles.footer}>
      Copyright © {new Date().getFullYear()} 汾源酒业经营体
    </footer>
  )
}
