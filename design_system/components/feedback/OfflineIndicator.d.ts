/** Inline connectivity banner. Expands/collapses vertically; it never covers content. */
export interface OfflineIndicatorProps {
  /** Defaults to "You're offline. Some content may be unavailable." */
  message?: string;
  style?: React.CSSProperties;
}
export function OfflineIndicator(props: OfflineIndicatorProps): JSX.Element;
