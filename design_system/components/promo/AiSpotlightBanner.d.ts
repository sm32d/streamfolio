/**
 * One-time promotion for the on-device AI features, shown at the top of the Home feed.
 */
export interface AiSpotlightBannerProps {
  title?: string;
  body?: string;
  ctaLabel?: string;
  onEnable?: () => void;
  onDismiss?: () => void;
  style?: React.CSSProperties;
}
export function AiSpotlightBanner(props: AiSpotlightBannerProps): JSX.Element;
