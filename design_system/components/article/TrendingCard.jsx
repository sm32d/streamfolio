import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// HomeScreen.kt TrendingCard — 280dp tall, 24dp radius, full-bleed image under a bottom scrim,
// "Trending" chip, 16sp title, 48dp primary play FAB, floating queue/bookmark buttons top-right.
export function TrendingCard({
  source, title, category, thumbnail, publishedAt = '2h ago', readingTime = '4 min read',
  bookmarked = false, onClick, onBookmark, onQueue, onPlay, style,
}) {
  const initial = (source || '?').trim()[0] || '?';
  return (
    <div onClick={onClick} style={{
      position: 'relative', height: 280, borderRadius: 24, overflow: 'hidden', cursor: 'pointer',
      boxShadow: 'var(--sf-shadow-8)', background: 'var(--sf-gradient-thumb-fallback)', ...style,
    }}>
      {thumbnail
        ? <img src={thumbnail} alt="" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }} />
        : <span style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', font: 'var(--sf-weight-black) 120px/1 var(--sf-font-sans)', color: 'rgb(255 255 255/.15)' }}>{initial}</span>}
      <div style={{ position: 'absolute', inset: 0, background: 'var(--sf-gradient-scrim)' }} />

      <div style={{ position: 'absolute', top: 16, right: 16, display: 'flex', gap: 8 }}>
        {[['playlist_add', 'Add to queue', onQueue], [bookmarked ? 'bookmark' : 'bookmark_add', 'Bookmark', onBookmark]].map(([n, label, fn]) => (
          <button key={label} type="button" aria-label={label} onClick={(e) => { e.stopPropagation(); fn && fn(); }} style={{
            width: 40, height: 40, borderRadius: '50%', border: 'none', cursor: 'pointer',
            background: 'rgb(0 0 0/.4)', display: 'grid', placeItems: 'center',
          }}><Icon name={n} size={20} color="var(--sf-white)" /></button>
        ))}
      </div>

      <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end', padding: 20, gap: 6 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ borderRadius: 8, padding: '4px 8px', background: 'var(--sf-primary)', font: 'var(--sf-weight-bold) 10px/1 var(--sf-font-sans)', color: 'var(--sf-on-primary)' }}>Trending</span>
          {category && <span style={{ font: 'var(--sf-weight-medium) 11px/1 var(--sf-font-sans)', color: 'rgb(255 255 255/.8)' }}>{category}</span>}
        </div>
        <div style={{
          font: 'var(--sf-weight-bold) 16px/1.3 var(--sf-font-sans)', color: 'var(--sf-white)',
          display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden', textWrap: 'pretty',
        }}>{title}</div>
        <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12 }}>
          <div>
            <div style={{ font: 'var(--sf-weight-semibold) 12px/1.4 var(--sf-font-sans)', color: 'rgb(255 255 255/.7)' }}>{source}</div>
            <div style={{ marginTop: 2, display: 'flex', alignItems: 'center', gap: 6, font: 'var(--sf-weight-regular) 11px/1 var(--sf-font-sans)', color: 'rgb(255 255 255/.5)' }}>
              <span>{publishedAt}</span><span style={{ color: 'rgb(255 255 255/.4)' }}>•</span><span>{readingTime}</span>
            </div>
          </div>
          <button type="button" aria-label="Play now" onClick={(e) => { e.stopPropagation(); onPlay && onPlay(); }} style={{
            width: 48, height: 48, borderRadius: '50%', border: 'none', cursor: 'pointer',
            background: 'var(--sf-primary)', display: 'grid', placeItems: 'center',
          }}><Icon name="play_arrow" size={28} color="var(--sf-on-primary)" /></button>
        </div>
      </div>
    </div>
  );
}
