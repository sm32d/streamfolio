/**
 * Grouped container for settings rows — the only card shape in Settings.
 */
export interface SettingsCardProps { children: React.ReactNode; style?: React.CSSProperties }
export function SettingsCard(props: SettingsCardProps): JSX.Element;
/** Inset 1px divider placed between SettingsRow children. */
export function CardDivider(): JSX.Element;
