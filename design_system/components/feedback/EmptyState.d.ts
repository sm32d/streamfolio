/** Illustrated empty state — the otter mascot appears here and nowhere else. */
export interface EmptyStateProps {
  /** Path to an otter illustration in assets/ (otter-no-articles, otter-no-bookmarks, otter-no-search, otter-search-welcome). */
  illustration?: string;
  /** Material Symbols glyph at 64px, used when there is no illustration for the case. */
  icon?: string;
  title: string;
  description?: string;
  /** Primary CTA label. */
  actionLabel?: string;
  onAction?: () => void;
  /** Extra stacked actions below the CTA. */
  children?: React.ReactNode;
  style?: React.CSSProperties;
}
export function EmptyState(props: EmptyStateProps): JSX.Element;
