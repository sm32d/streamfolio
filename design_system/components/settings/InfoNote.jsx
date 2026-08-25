import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// SettingsComponents.kt InfoNote — 12dp radius surfaceVariant @50% tip row, 12sp/18 copy.
export function InfoNote({ children, icon = 'info', style }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'flex-start', gap: 8, padding: '12px 14px', borderRadius: 12,
      background: 'var(--sf-surface-variant-soft)', ...style,
    }}>
      <Icon name={icon} size={16} color="var(--sf-primary)" style={{ marginTop: 1 }} />
      <span style={{ font: 'var(--sf-weight-regular) 12px/18px var(--sf-font-sans)', color: 'var(--sf-text-secondary)', textWrap: 'pretty' }}>{children}</span>
    </div>
  );
}
