import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// OfflineIndicator.kt — full-width 12dp pill, surface @95%, 18dp primary icon, 13sp medium copy.
export function OfflineIndicator({ message = "You're offline. Some content may be unavailable.", style }) {
  return (
    <div style={{ padding: '8px 24px', ...style }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 10, padding: '10px 16px', borderRadius: 12,
        background: 'var(--sf-surface-raised)',
      }}>
        <Icon name="wifi_off" size={18} color="var(--sf-primary)" />
        <span style={{ font: 'var(--sf-weight-medium) 13px/1.3 var(--sf-font-sans)', color: 'var(--sf-text-body)' }}>{message}</span>
      </div>
    </div>
  );
}
