/**
 * Horizontally scrolling feed-category / trending-topic pill.
 */
export interface CategoryPillProps {
  /** Label, e.g. "Top Stories", "#climate". */
  text: string;
  /** Selected fills with primary and switches the label to bold white. */
  selected?: boolean;
  /** Optional leading Material Symbols glyph, 16px. */
  icon?: string;
  /** Unread count chip on the trailing edge; hidden when 0/undefined. */
  badgeCount?: number;
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function CategoryPill(props: CategoryPillProps): JSX.Element;
