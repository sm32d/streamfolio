/** One-time coach mark above the feed explaining the user's configured swipe gestures. */
export interface SwipeHintBannerProps {
  /** Built from the two configured actions, e.g. "Swipe cards to act quickly: left to save, right to share". */
  message?: string;
  onDismiss?: () => void;
  style?: React.CSSProperties;
}
export function SwipeHintBanner(props: SwipeHintBannerProps): JSX.Element;
