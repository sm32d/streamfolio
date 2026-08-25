import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// ConfirmationModalSheet.kt — icon badge + title, 14sp/20 body, vertically stacked
// 48dp pill CTAs (24dp radius): primary, outlined secondary, optional text tertiary.
export function ConfirmationSheet({
  icon = 'delete', iconTint = 'var(--sf-error)', title, message,
  primaryLabel = 'Delete', primaryColor = 'var(--sf-error)', onPrimary,
  secondaryLabel = 'Cancel', onSecondary, tertiaryLabel, onTertiary, style,
}) {
  const pill = { width: '100%', height: 48, borderRadius: 24, cursor: 'pointer', font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)' };
  return (
    <div style={{ borderRadius: '28px 28px 0 0', background: 'var(--sf-surface)', boxShadow: 'var(--sf-shadow-16)', paddingBottom: 32, ...style }}>
      <div style={{ display: 'grid', placeItems: 'center', padding: '16px 0 8px' }}>
        <div style={{ width: 32, height: 4, borderRadius: 2, background: 'rgb(var(--sf-on-surface-rgb)/.4)' }} />
      </div>
      <div style={{ padding: '0 24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ width: 40, height: 40, borderRadius: '50%', display: 'grid', placeItems: 'center', background: `color-mix(in srgb, ${iconTint} 12%, transparent)` }}>
            <Icon name={icon} size={20} color={iconTint} />
          </div>
          <div style={{ font: 'var(--sf-weight-bold) 20px/1.25 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>{title}</div>
        </div>
        <div style={{ marginTop: 8, font: 'var(--sf-weight-regular) 14px/20px var(--sf-font-sans)', color: 'var(--sf-text-secondary)', textWrap: 'pretty' }}>{message}</div>
        <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 10 }}>
          <button type="button" onClick={onPrimary} style={{ ...pill, border: 'none', background: primaryColor, color: 'var(--sf-white)' }}>{primaryLabel}</button>
          {secondaryLabel && <button type="button" onClick={onSecondary} style={{ ...pill, background: 'transparent', border: '1px solid var(--sf-border-strong)', color: 'var(--sf-text-strong)' }}>{secondaryLabel}</button>}
          {tertiaryLabel && <button type="button" onClick={onTertiary} style={{ ...pill, height: 44, background: 'none', border: 'none', color: 'var(--sf-text-secondary)' }}>{tertiaryLabel}</button>}
        </div>
      </div>
    </div>
  );
}
