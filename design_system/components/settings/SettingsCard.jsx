import React from 'react';

// SettingsComponents.kt SettingsCard — 20dp radius, surface @60%, 1px onSurface @7% border.
export function SettingsCard({ children, style }) {
  return (
    <div style={{
      borderRadius: 20, overflow: 'hidden',
      background: 'var(--sf-surface-card)',
      border: '1px solid rgb(var(--sf-on-surface-rgb)/.07)',
      display: 'flex', flexDirection: 'column', ...style,
    }}>{children}</div>
  );
}

// CardDivider — inset hairline between rows.
export function CardDivider() {
  return <div style={{ height: 1, margin: '0 18px', background: 'rgb(var(--sf-on-surface-rgb)/.06)' }} />;
}
