import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// HomeScreen.kt SwipeHintBanner — dismissible 16dp card with a primary hairline,
// teaching the configured swipe actions.
export function SwipeHintBanner({ message = 'Swipe cards to act quickly: left to save, right to share', onDismiss, style }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12, margin: '8px 24px', padding: '10px 14px', borderRadius: 16,
      background: 'var(--sf-surface-raised)', border: '1px solid rgb(var(--sf-primary-rgb)/.15)', ...style,
    }}>
      <div style={{ width: 32, height: 32, borderRadius: '50%', background: 'var(--sf-primary-wash)', display: 'grid', placeItems: 'center', flex: '0 0 auto' }}>
        <Icon name="gesture" size={18} fill={false} color="var(--sf-primary)" />
      </div>
      <span style={{ flex: 1, font: 'var(--sf-weight-regular) 13px/18px var(--sf-font-sans)', color: 'var(--sf-text-body)' }}>{message}</span>
      <button type="button" aria-label="Dismiss" onClick={onDismiss} style={{ border: 'none', background: 'none', cursor: 'pointer', display: 'grid', padding: 8 }}>
        <Icon name="close" size={20} color="var(--sf-text-muted)" />
      </button>
    </div>
  );
}
