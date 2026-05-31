import styles from '../../style/ui/SectionHeader.module.css';

type SectionHeaderProps = {
  title: string;
  subtitle?: string;
  centered?: boolean;
  className?: string;
};

export default function SectionHeader({ title, subtitle, centered = false, className }: SectionHeaderProps) {
  const rootClass = [styles.header, centered ? styles.centered : '', className].filter(Boolean).join(' ');

  return (
    <div className={rootClass}>
      <h2 className={styles.title}>{title}</h2>
      {subtitle ? <p className={styles.subtitle}>{subtitle}</p> : null}
    </div>
  );
}
