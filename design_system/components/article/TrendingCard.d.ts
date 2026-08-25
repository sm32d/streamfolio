/**
 * Full-bleed hero card for the Home "Trending Highlights" pager.
 */
export interface TrendingCardProps {
  source: string;
  title: string;
  /** Feed category shown next to the "Trending" chip. */
  category?: string;
  thumbnail?: string;
  publishedAt?: string;
  readingTime?: string;
  bookmarked?: boolean;
  onClick?: () => void;
  onBookmark?: () => void;
  onQueue?: () => void;
  onPlay?: () => void;
  style?: React.CSSProperties;
}
export function TrendingCard(props: TrendingCardProps): JSX.Element;
