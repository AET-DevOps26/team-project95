import type { AnchorHTMLAttributes, ButtonHTMLAttributes, ReactNode } from 'react';
import styles from '../../style/ui/Button.module.css';

type ButtonVariant = 'primary' | 'secondary' | 'ghost';

type BaseProps = {
  children: ReactNode;
  variant?: ButtonVariant;
  className?: string;
};

type ButtonProps = BaseProps &
  Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'className' | 'children'> & {
    href?: undefined;
  };

type AnchorProps = BaseProps &
  Omit<AnchorHTMLAttributes<HTMLAnchorElement>, 'className' | 'children'> & {
    href: string;
  };

function classes(variant: ButtonVariant, className?: string) {
  return [styles.buttonBase, styles[variant], className].filter(Boolean).join(' ');
}

export default function Button(props: ButtonProps | AnchorProps) {
  const variant = props.variant ?? 'primary';

  if (typeof props.href === 'string') {
    const { children, className, variant: _variant, ...rest } = props;
    return (
      <a {...rest} className={classes(variant, className)}>
        {children}
      </a>
    );
  }

  const { children, className, variant: _variant, ...rest } = props;
  return (
    <button {...rest} className={classes(variant, className)}>
      {children}
    </button>
  );
}
