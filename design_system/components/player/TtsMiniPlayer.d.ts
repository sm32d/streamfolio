/**
 * Text-to-speech mini player — the glass pill that sits above the nav bar while an article is read aloud.
 */
export interface TtsMiniPlayerProps {
  /** Current article title, single line. */
  title: string;
  source: string;
  thumbnail?: string;
  playing?: boolean;
  /** Remaining sleep-timer label, e.g. "12m". Shows a bedtime glyph beside the source. */
  sleepTimer?: string;
  /** False greys the skip button (end of queue). Default true. */
  hasNext?: boolean;
  onToggle?: () => void;
  onNext?: () => void;
  /** Tapping the pill opens the full-screen lyrics visualiser. */
  onExpand?: () => void;
  style?: React.CSSProperties;
}
export function TtsMiniPlayer(props: TtsMiniPlayerProps): JSX.Element;
