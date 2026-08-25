import React from 'react';

// PublisherAvatar.kt — circular, primary @15% fill, hairline ring, favicon or bold initial.
export function PublisherAvatar({ sourceName = '', domain, size = 40, logoUrl, ringed = false, style }) {
  const src = logoUrl || (domain ? `https://www.google.com/s2/favicons?sz=64&domain=${domain}` : null);
  const initial = (sourceName.trim()[0] || 'N').toUpperCase();
  return (
    <div style={{
      width: size, height: size, borderRadius: '50%', overflow: 'hidden',
      display: 'grid', placeItems: 'center', flex: '0 0 auto',
      background: 'var(--sf-primary-tint)',
      border: ringed ? '2px solid var(--sf-primary)' : '1px solid var(--sf-border-soft)',
      ...style,
    }}>
      {src
        ? <img src={src} alt={sourceName} width={size} height={size} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        : <span style={{ font: `var(--sf-weight-bold) ${Math.round(size * 0.42)}px/1 var(--sf-font-sans)`, color: 'var(--sf-primary)' }}>{initial}</span>}
    </div>
  );
}
