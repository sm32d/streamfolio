/**
 * The app's floating bottom tab bar — a translucent pill above the content, not an edge-to-edge bar.
 */
export interface NavTab { id: string; icon: string; label: string }
export interface GlassmorphicNavBarProps {
  /** Active tab id. Defaults to "home". */
  selected?: string;
  onSelect?: (id: string) => void;
  /** Override the four default tabs (Home / Search / Saved / Settings). */
  tabs?: NavTab[];
  style?: React.CSSProperties;
}
export function GlassmorphicNavBar(props: GlassmorphicNavBarProps): JSX.Element;
