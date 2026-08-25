/**
 * The feed's standard article row: 96px thumbnail with an overlaid play button, source, title, meta, actions.
 */
export interface ArticleCardProps {
  /** Publisher name — rendered 11sp bold in primary above the title. */
  source: string;
  title: string;
  /** Thumbnail URL. Omit to get the blue→green fallback tile with the source initial. */
  thumbnail?: string;
  /** Relative or short date, e.g. "2h ago". */
  publishedAt?: string;
  /** e.g. "4 min read". */
  readingTime?: string;
  bookmarked?: boolean;
  /** Read articles render at 55% opacity. */
  read?: boolean;
  /** >0 attaches the "Also covered by N other sources" expander to the card's bottom edge. */
  coverageCount?: number;
  onClick?: () => void;
  onBookmark?: () => void;
  onQueue?: () => void;
  onPlay?: () => void;
  style?: React.CSSProperties;
}
export function ArticleCard(props: ArticleCardProps): JSX.Element;
