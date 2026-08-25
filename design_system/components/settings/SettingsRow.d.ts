/** One navigable or toggleable setting inside a SettingsCard. */
export interface SettingsRowProps {
  /** Material Symbols glyph, drawn white on the solid tile. */
  icon: string;
  /** Tile fill — primary, secondary or tertiary; the app rotates them per row. Default primary. */
  iconBg?: string;
  title: string;
  subtitle?: string;
  /** Replaces the chevron — pass a StreamFolioSwitch, a value string, or null to hide it. */
  trailing?: React.ReactNode;
  onClick?: () => void;
  style?: React.CSSProperties;
}
export function SettingsRow(props: SettingsRowProps): JSX.Element;
