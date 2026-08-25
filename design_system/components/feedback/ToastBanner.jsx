import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// BookmarkToastBanner.kt — floating 20dp surfaceVariant banner at 92% width,
// 32dp primary-tinted icon circle, 14sp semibold message, optional text action.
export function ToastBanner({ message, icon = 'info', actionLabel, onAction, style }) {
  return (
    <div style={{
      width: '92%', margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      gap: 12, padding: '10px 16px', borderRadius: 20,
      background: 'var(--sf-surface-variant)', border: '1px solid var(--sf-border)',
      boxShadow: 'var(--sf-shadow-8)', ...style,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0 }}>
        <div style={{ width: 32, height: 32, borderRadius: '50%', background: 'var(--sf-primary-tint)', display: 'grid', placeItems: 'center', flex: '0 0 auto' }}>
          <Icon name={icon} size={16} color="var(--sf-primary)" />
        </div>
        <span style={{ font: 'var(--sf-weight-semibold) 14px/1.35 var(--sf-font-sans)', color: 'var(--sf-text-strong)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{message}</span>
      </div>
      {actionLabel && (
        <button type="button" onClick={onAction} style={{
          border: 'none', background: 'none', cursor: 'pointer', padding: '8px 12px',
          font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)', color: 'var(--sf-primary)', flex: '0 0 auto',
        }}>{actionLabel}</button>
      )}
    </div>
  );
}
