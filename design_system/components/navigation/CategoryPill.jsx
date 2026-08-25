import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// CategoryPill.kt — 20dp radius, 14/8 padding, 13sp label, optional leading icon + count badge.
export function CategoryPill({ text, selected = false, icon, badgeCount, onClick, style }) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        display: 'inline-flex', alignItems: 'center', gap: 6,
        padding: '8px 14px', borderRadius: 20, cursor: 'pointer',
        font: `${selected ? 'var(--sf-weight-bold)' : 'var(--sf-weight-medium)'} 13px/1.2 var(--sf-font-sans)`,
        background: selected ? 'var(--sf-primary)' : 'var(--sf-surface-pill)',
        color: selected ? 'var(--sf-white)' : 'var(--sf-text-body)',
        border: selected ? '1px solid transparent' : '1px solid var(--sf-border)',
        whiteSpace: 'nowrap', transition: 'background var(--sf-dur-150) var(--sf-ease-standard)',
        ...style,
      }}
    >
      {icon && <Icon name={icon} size={16} color={selected ? 'var(--sf-white)' : 'var(--sf-text-secondary)'} />}
      {text}
      {badgeCount > 0 && (
        <span style={{
          borderRadius: 10, padding: '2px 6px',
          font: 'var(--sf-weight-bold) 10px/1 var(--sf-font-sans)',
          background: selected ? 'rgb(255 255 255/.25)' : 'var(--sf-primary-tint)',
          color: selected ? 'var(--sf-white)' : 'var(--sf-primary)',
        }}>{badgeCount}</span>
      )}
    </button>
  );
}
