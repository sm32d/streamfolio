/** The app's only toggle. Fires a long-press haptic on every change. */
export interface StreamFolioSwitchProps {
  checked?: boolean;
  disabled?: boolean;
  onChange?: (checked: boolean) => void;
  style?: React.CSSProperties;
}
export function StreamFolioSwitch(props: StreamFolioSwitchProps): JSX.Element;
