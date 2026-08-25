import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// SettingsComponents.kt SubPageTopBar — back chevron + 24sp bold title, optional trailing slot.
export function SubPageTopBar({ title, onBack, trailing, style }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 0 16px', ...style }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
        <button type="button" onClick={onBack} aria-label="Back" style={{
          display: 'grid', placeItems: 'center', width: 48, height: 48,
          border: 'none', background: 'none', borderRadius: '50%', cursor: 'pointer',
        }}>
          <Icon name="arrow_back" size={24} color="var(--sf-text-strong)" />
        </button>
        <h2 style={{ margin: 0, font: 'var(--sf-weight-bold) 24px/1.2 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>{title}</h2>
      </div>
      {trailing && <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>{trailing}</div>}
    </div>
  );
}
