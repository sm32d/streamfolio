/** Collection row for the Bookmarks list view (toggled from the header's grid/list button). */
export interface CollectionListRowProps {
  title: string;
  count?: number;
  thumbnail?: string;
  isDefault?: boolean;
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function CollectionListRow(props: CollectionListRowProps): JSX.Element;
