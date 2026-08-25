import React from 'react';

// SkeletonLoader.kt — light-grey shimmer sweeping on a 1000ms linear loop,
// mirroring the ArticleListItem layout exactly.
const SHIMMER = 'linear-gradient(110deg,rgb(211 211 211/.5) 0%,rgb(211 211 211/.15) 50%,rgb(211 211 211/.5) 100%)';

function Bar({ w = '100%', h = 16, r = 4 }) {
  return <div style={{ width: w, height: h, borderRadius: r, background: SHIMMER, backgroundSize: '200% 100%', animation: 'sf-shimmer var(--sf-dur-1000) var(--sf-ease-linear) infinite' }} />;
}

export function SkeletonLoader({ rows = 4, variant = 'list', style }) {
  return (
    <div style={{ padding: variant === 'list' ? '8px 24px' : 0, ...style }}>
      <style>{'@keyframes sf-shimmer{from{background-position:200% 0}to{background-position:-200% 0}}'}</style>
      {Array.from({ length: rows }).map((_, i) => variant === 'list' ? (
        <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '12px 0' }}>
          <div style={{ width: 96, height: 96, borderRadius: 16, background: SHIMMER, backgroundSize: '200% 100%', animation: 'sf-shimmer var(--sf-dur-1000) var(--sf-ease-linear) infinite', flex: '0 0 auto' }} />
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
            <Bar w="35%" h={12} /><div style={{ height: 8 }} />
            <Bar h={16} /><div style={{ height: 6 }} />
            <Bar w="85%" h={16} /><div style={{ height: 12 }} />
            <Bar w="25%" h={11} />
          </div>
        </div>
      ) : (
        <div key={i} style={{ display: 'flex', flexDirection: 'column', gap: 8, padding: '12px 0 28px' }}>
          <Bar w="88%" /><Bar /><Bar w="94%" /><Bar w="65%" />
        </div>
      ))}
    </div>
  );
}
