import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// HomeScreen.kt ArticleListItem — 20dp card, 12dp padding, 96dp thumb @16dp radius,
// 11sp primary source line, 14sp bold 2-line title, meta + queue/bookmark actions.
export function ArticleCard({
  source, title, thumbnail, publishedAt = '2h ago', readingTime = '4 min read',
  bookmarked = false, read = false, coverageCount = 0, onClick, onBookmark, onQueue, onPlay, style,
}) {
  const initial = (source || '?').trim()[0] || '?';
  const stacked = coverageCount > 0;
  return (
    <div style={{ ...style }}>
      <div
        onClick={onClick}
        style={{
          display: 'flex', alignItems: 'center', gap: 16, padding: 12, cursor: 'pointer',
          background: 'var(--sf-surface-card)',
          border: '1px solid var(--sf-border-hairline)',
          borderRadius: stacked ? '20px 20px 0 0' : 20,
          opacity: read ? 'var(--sf-alpha-read)' : 1,
        }}
      >
        <div style={{ position: 'relative', width: 96, height: 96, borderRadius: 16, overflow: 'hidden', flex: '0 0 auto', display: 'grid', placeItems: 'center', background: 'var(--sf-gradient-thumb-fallback)' }}>
          {thumbnail
            ? <img src={thumbnail} alt="" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }} />
            : <span style={{ font: 'var(--sf-weight-bold) 32px/1 var(--sf-font-sans)', color: 'var(--sf-white)' }}>{initial}</span>}
          <button type="button" aria-label="Play now" onClick={(e) => { e.stopPropagation(); onPlay && onPlay(); }} style={{
            position: 'relative', width: 36, height: 36, borderRadius: '50%', border: 'none', cursor: 'pointer',
            background: 'rgb(0 0 0/.5)', display: 'grid', placeItems: 'center',
          }}>
            <Icon name="play_arrow" size={20} color="var(--sf-white)" />
          </button>
        </div>

        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ font: 'var(--sf-weight-bold) 11px/1.2 var(--sf-font-sans)', color: 'var(--sf-primary)' }}>{source}</div>
          <div style={{
            marginTop: 4, font: 'var(--sf-weight-bold) 14px/1.35 var(--sf-font-sans)', color: 'var(--sf-text-strong)',
            display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden', textWrap: 'pretty',
          }}>{title}</div>
          <div style={{ marginTop: 10, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, font: 'var(--sf-weight-regular) 11px/1 var(--sf-font-sans)', color: 'var(--sf-text-faint)' }}>
              <span>{publishedAt}</span><span style={{ color: 'var(--sf-text-disabled)' }}>•</span><span>{readingTime}</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <button type="button" aria-label="Add to queue" onClick={(e) => { e.stopPropagation(); onQueue && onQueue(); }} style={{ border: 'none', background: 'none', padding: 0, cursor: 'pointer', display: 'grid' }}>
                <Icon name="playlist_add" size={18} color="var(--sf-text-faint)" />
              </button>
              <button type="button" aria-label="Bookmark" onClick={(e) => { e.stopPropagation(); onBookmark && onBookmark(); }} style={{ border: 'none', background: 'none', padding: 0, cursor: 'pointer', display: 'grid' }}>
                <Icon name={bookmarked ? 'bookmark' : 'bookmark_add'} fill={bookmarked} size={18} color={bookmarked ? 'var(--sf-primary)' : 'var(--sf-text-faint)'} />
              </button>
            </div>
          </div>
        </div>
      </div>

      {stacked && (
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '13px 16px 10px', borderRadius: '0 0 20px 20px',
          background: 'var(--sf-surface-variant-soft)',
          border: '1px solid rgb(var(--sf-on-surface-rgb)/.05)', borderTop: 'none',
          cursor: 'pointer',
        }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: 8, font: 'var(--sf-weight-bold) 12px/1 var(--sf-font-sans)', color: 'var(--sf-text-body)' }}>
            <Icon name="library_books" size={16} color="rgb(var(--sf-primary-rgb)/.8)" />
            Also covered by {coverageCount} other {coverageCount === 1 ? 'source' : 'sources'}
          </span>
          <Icon name="keyboard_arrow_down" size={18} color="var(--sf-text-faint)" />
        </div>
      )}
    </div>
  );
}
