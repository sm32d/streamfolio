import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// HomeScreen.kt AiSpotlightBanner — the only gradient-filled card in the app:
// indigo→teal, 20dp radius, white-on-gradient copy, white primary CTA.
export function AiSpotlightBanner({
  title = 'Upgrade to On-Device AI',
  body = 'Translate foreign feeds offline, read dynamic key summaries, and browse semantic tags. Private and run 100% locally.',
  ctaLabel = 'Enable AI', onEnable, onDismiss, style,
}) {
  return (
    <div style={{
      margin: '12px 20px', borderRadius: 20, padding: 20, background: 'var(--sf-gradient-ai)',
      boxShadow: 'var(--sf-shadow-6)', display: 'flex', flexDirection: 'column', gap: 12, ...style,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ width: 36, height: 36, borderRadius: 10, background: 'rgb(255 255 255/.2)', display: 'grid', placeItems: 'center' }}>
          <Icon name="auto_awesome" size={18} color="var(--sf-white)" />
        </div>
        <span style={{ font: 'var(--sf-weight-bold) 16px/1.25 var(--sf-font-sans)', color: 'var(--sf-white)' }}>{title}</span>
      </div>
      <p style={{ margin: 0, font: 'var(--sf-weight-regular) 13px/18px var(--sf-font-sans)', color: 'rgb(255 255 255/.85)', textWrap: 'pretty' }}>{body}</p>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <button type="button" onClick={onEnable} style={{
          padding: '8px 16px', borderRadius: 12, border: 'none', cursor: 'pointer',
          background: 'var(--sf-white)', color: 'var(--sf-indigo-500)',
          font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)',
        }}>{ctaLabel}</button>
        <button type="button" onClick={onDismiss} style={{
          padding: '8px 12px', border: 'none', background: 'none', cursor: 'pointer',
          color: 'rgb(255 255 255/.7)', font: 'var(--sf-weight-medium) 13px/1 var(--sf-font-sans)',
        }}>Dismiss</button>
      </div>
    </div>
  );
}
