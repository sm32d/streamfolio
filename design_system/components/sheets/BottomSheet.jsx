import React from 'react';

// The app's only modal surface: ModalBottomSheet with 28dp top corners, a drag handle,
// solid surface fill and 24dp horizontal padding.
export function BottomSheet({ children, style }) {
  return (
    <div style={{
      borderRadius: '28px 28px 0 0', background: 'var(--sf-surface)',
      boxShadow: 'var(--sf-shadow-16)', paddingBottom: 32, ...style,
    }}>
      <div style={{ display: 'grid', placeItems: 'center', padding: '16px 0 8px' }}>
        <div style={{ width: 32, height: 4, borderRadius: 2, background: 'rgb(var(--sf-on-surface-rgb)/.4)' }} />
      </div>
      <div style={{ padding: '0 24px' }}>{children}</div>
    </div>
  );
}
