import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// GlassmorphicNavBar.kt — floating 32dp pill, 24dp side margin, surface @98%, 1px white hairline.
const TABS = [
  { id: 'home', icon: 'home', label: 'Home' },
  { id: 'search', icon: 'search', label: 'Search' },
  { id: 'bookmarks', icon: 'bookmark', label: 'Saved' },
  { id: 'settings', icon: 'settings', label: 'Settings' },
];

export function GlassmorphicNavBar({ selected = 'home', onSelect, tabs = TABS, style }) {
  return (
    <nav style={{ padding: '8px 24px', ...style }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 0,
        padding: '8px 12px', borderRadius: 32,
        background: 'var(--sf-surface-chrome)',
        backdropFilter: 'var(--sf-blur-chrome)',
        border: '1px solid rgb(255 255 255/.15)',
        boxShadow: 'var(--sf-shadow-16)',
      }}>
        {tabs.map((t) => {
          const on = t.id === selected;
          const tint = on ? 'var(--sf-primary)' : 'var(--sf-text-muted)';
          return (
            <button key={t.id} type="button" onClick={() => onSelect && onSelect(t.id)} style={{
              flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
              padding: '6px 0', border: 'none', background: 'none', borderRadius: 16, cursor: 'pointer',
            }}>
              <Icon name={t.icon} size={24} color={tint} />
              <span style={{ font: `var(--sf-weight-regular) 10px/1.2 var(--sf-font-sans)`, color: tint }}>{t.label}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
