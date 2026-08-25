/** Explanatory footnote under a settings group or form field. */
export interface InfoNoteProps {
  children: React.ReactNode;
  /** Leading glyph. Default "info". */
  icon?: string;
  style?: React.CSSProperties;
}
export function InfoNote(props: InfoNoteProps): JSX.Element;
