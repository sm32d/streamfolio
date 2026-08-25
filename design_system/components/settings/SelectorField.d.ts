/** Dropdown-backed value field used for language, region and AI-provider choices. */
export interface SelectorFieldProps {
  /** Uppercase label above the field. Omit for an unlabelled field. */
  label?: string;
  /** Current selection, e.g. "United Kingdom". */
  value: string;
  /** Leading Material Symbols glyph, tinted primary. Default "tune". */
  icon?: string;
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function SelectorField(props: SelectorFieldProps): JSX.Element;
