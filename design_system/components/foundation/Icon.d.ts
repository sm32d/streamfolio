/**
 * Material Symbols Rounded icon glyph, matched to the Compose Material Icons set used in the app.
 */
export interface IconProps {
  /** Material Symbols glyph name, e.g. "bookmark_add", "play_arrow", "rss_feed". */
  name: string;
  /** Pixel size. App uses 12, 16, 18, 20, 22, 24, 28, 64. Default 20. */
  size?: number;
  /** Filled (Icons.Default.*) vs outlined (Icons.Outlined.*). Default true. */
  fill?: boolean;
  /** Optical weight, 100–700. Default 400. */
  weight?: number;
  /** Glyph colour. Default currentColor. */
  color?: string;
  style?: React.CSSProperties;
}
export function Icon(props: IconProps): JSX.Element;
