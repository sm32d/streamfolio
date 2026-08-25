import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// BookmarkScreen.kt CollectionListRow — the list-view alternative: 20dp surface row,
// square cover thumb, title + count, trailing chevron.
export function CollectionListRow({ title, count = 0, thumbnail, isDefault = false, onClick, style }) {
  return (
    <div onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 14, padding: 12, borderRadius: 20, cursor: 'pointer',
      background: 'var(--sf-surface-card)', border: '1px solid var(--sf-border-hairline)', ...style,
    }}>
      <div style={{
        width: 56, height: 56, borderRadius: 16, overflow: 'hidden', flex: '0 0 auto',
        display: 'grid', placeItems: 'center',
        background: thumbnail ? 'transparent' : 'linear-gradient(135deg,rgb(var(--sf-primary-rgb)/.85),rgb(21 101 192/.7))',
      }}>
        {thumbnail
          ? <img src={thumbnail} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          : <Icon name={isDefault ? 'folder_special' : 'folder'} fill={!isDefault} size={24} color="var(--sf-white)" />}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ font: 'var(--sf-weight-bold) 15px/1.3 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>{title}</div>
        <div style={{ font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)', color: 'var(--sf-text-muted)' }}>{count === 1 ? '1 item' : `${count} items`}</div>
      </div>
      <Icon name="keyboard_arrow_right" size={20} color="var(--sf-text-disabled)" />
    </div>
  );
}
