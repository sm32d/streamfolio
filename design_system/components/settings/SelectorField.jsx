import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// SettingsComponents.kt SettingsSelectorField — 56dp tall, 16dp radius, surfaceVariant @50%,
// primary leading icon, 15sp medium value, trailing dropdown caret.
export function SelectorField({ label, value, icon = 'tune', onClick, style }) {
  return (
    <div style={{ ...style }}>
      {label && <div style={{
        marginBottom: 8, font: 'var(--sf-weight-semibold) 11px/1 var(--sf-font-sans)',
        letterSpacing: 'var(--sf-tracking-label)', textTransform: 'uppercase', color: 'var(--sf-text-label)',
      }}>{label}</div>}
      <div onClick={onClick} style={{
        height: 56, display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '0 16px', borderRadius: 16, cursor: 'pointer',
        background: 'var(--sf-surface-variant-soft)', border: '1px solid var(--sf-border-soft)',
      }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Icon name={icon} size={20} color="var(--sf-primary)" />
          <span style={{ font: 'var(--sf-weight-medium) 15px/1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>{value}</span>
        </span>
        <Icon name="arrow_drop_down" size={24} color="var(--sf-text-faint)" />
      </div>
    </div>
  );
}
