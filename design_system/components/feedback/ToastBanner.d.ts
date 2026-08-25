/** Floating bottom toast. Slides up over the nav bar, auto-dismisses after 3.5s. */
export interface ToastBannerProps {
  /** e.g. `Saved to "Read later"`, "Added to audio playlist". */
  message: string;
  /** Material Symbols glyph in the tinted circle. Default "info". */
  icon?: string;
  /** Trailing text action, e.g. "Change". */
  actionLabel?: string;
  onAction?: () => void;
  style?: React.CSSProperties;
}
export function ToastBanner(props: ToastBannerProps): JSX.Element;
