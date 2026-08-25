import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// TtsMiniPlayer.kt — 64dp glass pill, 24dp side margin, 32dp radius, white hairline,
// 40dp thumb, title/source, play-pause + skip. Drag down to dismiss (confirmed by a sheet).
export function TtsMiniPlayer({ title, source, thumbnail, playing = false, sleepTimer, hasNext = true, onToggle, onNext, onExpand, style }) {
  return (
    <div style={{ padding: '0 24px', ...style }}>
      <div onClick={onExpand} style={{
        height: 64, display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px',
        borderRadius: 32, cursor: 'pointer',
        background: 'var(--sf-surface-chrome)', backdropFilter: 'var(--sf-blur-chrome)',
        border: '1px solid rgb(255 255 255/.15)', boxShadow: 'var(--sf-shadow-16)',
      }}>
        <div style={{ width: 40, height: 40, borderRadius: 12, overflow: 'hidden', flex: '0 0 auto', background: 'var(--sf-primary-ghost)', display: 'grid', placeItems: 'center' }}>
          {thumbnail
            ? <img src={thumbnail} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            : <Icon name="hearing" fill={false} size={20} color="var(--sf-primary)" />}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ font: 'var(--sf-weight-bold) 13px/1.3 var(--sf-font-sans)', color: 'var(--sf-text-strong)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{title}</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, font: 'var(--sf-weight-regular) 11px/1.4 var(--sf-font-sans)', color: 'var(--sf-text-secondary)' }}>
            <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{source}</span>
            {sleepTimer && <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, color: 'var(--sf-primary)', font: 'var(--sf-weight-semibold) 10px/1 var(--sf-font-sans)' }}>
              <Icon name="bedtime" size={12} color="var(--sf-primary)" />{sleepTimer}
            </span>}
          </div>
        </div>
        <button type="button" aria-label={playing ? 'Pause' : 'Play'} onClick={(e) => { e.stopPropagation(); onToggle && onToggle(); }} style={{ width: 36, height: 36, border: 'none', background: 'none', cursor: 'pointer', display: 'grid', placeItems: 'center' }}>
          <Icon name={playing ? 'pause' : 'play_arrow'} size={24} color="var(--sf-primary)" />
        </button>
        <button type="button" aria-label="Next" disabled={!hasNext} onClick={(e) => { e.stopPropagation(); onNext && onNext(); }} style={{ width: 36, height: 36, border: 'none', background: 'none', cursor: hasNext ? 'pointer' : 'default', display: 'grid', placeItems: 'center' }}>
          <Icon name="skip_next" size={24} color={hasNext ? 'var(--sf-text-strong)' : 'var(--sf-text-disabled)'} />
        </button>
      </div>
    </div>
  );
}
