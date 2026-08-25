/** Circular publisher identity mark: favicon when available, bold initial otherwise. */
export interface PublisherAvatarProps {
  /** Publisher name — supplies the fallback initial and alt text. */
  sourceName: string;
  /** Bare domain, e.g. "bbc.co.uk". Resolves a favicon via Google s2. */
  domain?: string;
  /** Explicit logo URL, wins over domain. */
  logoUrl?: string;
  /** Diameter. 40 in article rows, 56 in the publisher filter carousel. Default 40. */
  size?: number;
  /** 2px primary ring — the selected state in the publisher filter row. */
  ringed?: boolean;
  style?: React.CSSProperties;
}
export function PublisherAvatar(props: PublisherAvatarProps): JSX.Element;
