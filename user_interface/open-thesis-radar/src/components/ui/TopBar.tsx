import styles from '../../style/ui/TopBar.module.css';

type TopBarProps = {
  brand?: string;
};

export default function TopBar({ brand = 'Thesis Radar' }: TopBarProps) {
  return (
    <header className={styles.topBar}>
      <div className={styles.topBarInner}>
        <div className={styles.logo}>{brand}</div>
      </div>
    </header>
  );
}
