import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// HomeScreen.kt BottomSheetActionRow — tinted icon tile, 15sp title, 12sp subtitle.
export function SheetActionRow({ icon, iconTint = 'var(--sf-primary)', iconBg, title, subtitle, onClick, style }) {
  return (
    <div onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 14, padding: '10px 0', cursor: 'pointer', ...style,
    }}>
      <div style={{
        width: 44, height: 44, borderRadius: 14, display: 'grid', placeItems: 'center', flex: '0 0 auto',
        background: iconBg || `color-mix(in srgb, ${iconTint} 10%, transparent)`,
      }}>
        <Icon name={icon} size={20} color={iconTint} />
      </div>
      <div style={{ minWidth: 0 }}>
        <div style={{ font: 'var(--sf-weight-semibold) 15px/1.3 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>{title}</div>
        {subtitle && <div style={{ font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)', color: 'var(--sf-text-muted)' }}>{subtitle}</div>}
      </div>
    </div>
  );
}
