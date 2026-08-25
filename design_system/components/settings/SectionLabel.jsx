import React from 'react';

// SettingsComponents.kt SectionLabel — 11sp semibold uppercase, 1sp tracking, onSurface @45%.
export function SectionLabel({ children, style }) {
  return (
    <div style={{
      font: 'var(--sf-weight-semibold) 11px/1 var(--sf-font-sans)',
      letterSpacing: 'var(--sf-tracking-label)', textTransform: 'uppercase',
      color: 'var(--sf-text-label)', ...style,
    }}>{children}</div>
  );
}
