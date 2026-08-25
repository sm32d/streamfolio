/** Top bar for every settings sub-page: back arrow, 24sp bold title, optional trailing actions. */
export interface SubPageTopBarProps {
  title: string;
  onBack?: () => void;
  /** Trailing action row (icon buttons, a text action). */
  trailing?: React.ReactNode;
  style?: React.CSSProperties;
}
export function SubPageTopBar(props: SubPageTopBarProps): JSX.Element;
