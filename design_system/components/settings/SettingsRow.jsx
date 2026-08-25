import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// SettingsComponents.kt SettingsRow — 40dp solid icon tile @12dp radius, 15sp semibold title,
// 12sp @50% subtitle, trailing chevron at @30%.
export function SettingsRow({ icon, iconBg = 'var(--sf-primary)', title, subtitle, trailing, onClick, style }) {
  return (
    <div onClick={onClick} style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 14,
      padding: '14px 18px', cursor: onClick ? 'pointer' : 'default', ...style,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 14, flex: 1, minWidth: 0 }}>
        <div style={{ width: 40, height: 40, borderRadius: 12, background: iconBg, display: 'grid', placeItems: 'center', flex: '0 0 auto' }}>
          <Icon name={icon} size={20} color="var(--sf-white)" />
        </div>
        <div style={{ minWidth: 0 }}>
          <div style={{ font: 'var(--sf-weight-semibold) 15px/1.3 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>{title}</div>
          {subtitle && <div style={{ font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)', color: 'var(--sf-text-muted)' }}>{subtitle}</div>}
        </div>
      </div>
      {trailing !== undefined ? trailing : <Icon name="keyboard_arrow_right" size={20} color="var(--sf-text-disabled)" />}
    </div>
  );
}
