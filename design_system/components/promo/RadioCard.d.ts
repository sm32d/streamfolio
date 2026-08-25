/**
 * "StreamFolio FM 99.9" — the AI radio-DJ card, unlocked by tapping the version row five times.
 */
export interface RadioCardProps {
  /** The DJ's current line. Falls back to the station ident. */
  broadcast?: string;
  /** Animates the equalizer bars; false flattens them to 6px. Default true. */
  playing?: boolean;
  onOpenArticle?: () => void;
  onTuneOff?: () => void;
  style?: React.CSSProperties;
}
export function RadioCard(props: RadioCardProps): JSX.Element;
