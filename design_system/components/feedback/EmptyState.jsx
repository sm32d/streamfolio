import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// EmptyState.kt — centred otter illustration at 55% width, 18sp bold title,
// 14sp/20 secondary description, optional primary CTA.
export function EmptyState({ illustration, icon, title, description, actionLabel, onAction, children, style }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: '24px 32px', ...style }}>
      {illustration
        ? <img src={illustration} alt="" style={{ width: '55%', maxWidth: 260, aspectRatio: '1', objectFit: 'contain', marginBottom: 16 }} />
        : icon ? <div style={{ marginBottom: 20 }}><Icon name={icon} size={64} color="var(--sf-primary)" /></div> : null}
      <div style={{ font: 'var(--sf-weight-bold) 18px/1.3 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>{title}</div>
      {description && <div style={{ marginTop: 8, maxWidth: 380, font: 'var(--sf-weight-regular) 14px/20px var(--sf-font-sans)', color: 'var(--sf-text-secondary)', textWrap: 'pretty' }}>{description}</div>}
      {actionLabel && (
        <button type="button" onClick={onAction} style={{
          marginTop: 24, padding: '12px 20px', borderRadius: 12, border: 'none', cursor: 'pointer',
          background: 'var(--sf-primary)', color: 'var(--sf-on-primary)',
          font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)',
        }}>{actionLabel}</button>
      )}
      {children}
    </div>
  );
}
