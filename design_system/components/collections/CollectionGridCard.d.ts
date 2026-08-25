/**
 * Bookmark collection tile in the Bookmarks grid view.
 */
export interface CollectionGridCardProps {
  title: string;
  /** Saved-article count; drives both the chip and the "N items" line. */
  count?: number;
  /** Cover image — the first saved article's thumbnail. Falls back to a brand gradient. */
  thumbnail?: string;
  /** True for the built-in "All Saved" collection; switches the glyph to folder_special. */
  isDefault?: boolean;
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function CollectionGridCard(props: CollectionGridCardProps): JSX.Element;
export function AddCollectionGridCard(props: { onClick?: () => void; style?: React.CSSProperties }): JSX.Element;
