/**
 * Destructive / confirming decision sheet. Buttons stack vertically, full width, pill shaped.
 */
export interface ConfirmationSheetProps {
  /** Badge glyph. Default "delete". */
  icon?: string;
  /** Badge colour; the circle behind it is this colour at 12%. Default error red. */
  iconTint?: string;
  title: string;
  message: string;
  primaryLabel?: string;
  primaryColor?: string;
  onPrimary?: () => void;
  /** Outlined secondary. Pass null to omit. Default "Cancel". */
  secondaryLabel?: string | null;
  onSecondary?: () => void;
  /** Optional low-emphasis text action below the pair. */
  tertiaryLabel?: string;
  onTertiary?: () => void;
  style?: React.CSSProperties;
}
export function ConfirmationSheet(props: ConfirmationSheetProps): JSX.Element;
