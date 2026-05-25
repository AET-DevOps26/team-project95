import type { HTMLAttributes, ReactNode } from 'react';
import styles from '../../style/ui/Card.module.css';

type CardProps = {
  children: ReactNode;
  className?: string;
} & HTMLAttributes<HTMLElement>;

export default function Card({ children, className, ...rest }: CardProps) {
  return (
    <section {...rest} className={[styles.card, className].filter(Boolean).join(' ')}>
      {children}
    </section>
  );
}
