/** One action inside a BottomSheet — tinted icon tile, title, explanatory subtitle. */
export interface SheetActionRowProps {
  icon: string;
  /** Glyph colour; the tile fill is this colour at 10%. Default primary. */
  iconTint?: string;
  /** Override the derived 10% tile fill. */
  iconBg?: string;
  title: string;
  subtitle?: string;
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function SheetActionRow(props: SheetActionRowProps): JSX.Element;
