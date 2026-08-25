import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// BookmarkScreen.kt CollectionGridCard — 145dp tall, 20dp radius, cover image under a dark
// gradient (or a primary/secondary container wash), folder tile + count chip, title + item count.
export function CollectionGridCard({ title, count = 0, thumbnail, isDefault = false, onClick, style }) {
  return (
    <div onClick={onClick} style={{
      position: 'relative', height: 145, borderRadius: 20, overflow: 'hidden', cursor: 'pointer',
      border: '1px solid rgb(var(--sf-on-surface-rgb)/.25)', ...style,
    }}>
      {thumbnail
        ? <><img src={thumbnail} alt="" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }} />
            <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg,rgb(0 0 0/.25),rgb(0 0 0/.85))' }} /></>
        : <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(135deg,rgb(var(--sf-primary-rgb)/.85),rgb(21 101 192/.7))' }} />}
      <div style={{ position: 'absolute', inset: 0, padding: 14, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ width: 36, height: 36, borderRadius: 10, background: 'rgb(255 255 255/.22)', display: 'grid', placeItems: 'center' }}>
            <Icon name={isDefault ? 'folder_special' : 'folder'} fill={!isDefault} size={20} color="var(--sf-white)" />
          </div>
          <span style={{ borderRadius: 12, padding: '3px 8px', background: 'rgb(0 0 0/.45)', font: 'var(--sf-weight-bold) 12px/1 var(--sf-font-sans)', color: 'var(--sf-white)' }}>{count}</span>
        </div>
        <div>
          <div style={{ font: 'var(--sf-weight-bold) 16px/1.25 var(--sf-font-sans)', color: 'var(--sf-white)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{title}</div>
          <div style={{ font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)', color: 'rgb(255 255 255/.8)' }}>{count === 1 ? '1 item' : `${count} items`}</div>
        </div>
      </div>
    </div>
  );
}

// AddCollectionGridCard — outlined primary tile that opens the create-collection sheet.
export function AddCollectionGridCard({ onClick, style }) {
  return (
    <div onClick={onClick} style={{
      height: 145, borderRadius: 20, cursor: 'pointer',
      background: 'rgb(255 255 255/.85)', border: '1.5px solid rgb(var(--sf-primary-rgb)/.5)',
      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 10, ...style,
    }}>
      <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'var(--sf-primary-wash)', display: 'grid', placeItems: 'center' }}>
        <Icon name="add" size={24} color="var(--sf-primary)" />
      </div>
      <span style={{ font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)', color: 'var(--sf-primary)' }}>New Collection</span>
    </div>
  );
}
