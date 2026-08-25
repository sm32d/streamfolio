/** Shimmer placeholder. Two variants mirroring the two things that load: the feed and an article body. */
export interface SkeletonLoaderProps {
  /** Repeat count. 4 for the feed, 3 for article paragraphs. Default 4. */
  rows?: number;
  /** "list" mirrors ArticleCard; "text" mirrors reader paragraphs. Default "list". */
  variant?: 'list' | 'text';
  style?: React.CSSProperties;
}
export function SkeletonLoader(props: SkeletonLoaderProps): JSX.Element;
