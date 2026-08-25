import React from 'react';
import { Icon } from '../foundation/Icon.jsx';

// StreamFolioRadioCard.kt — the "StreamFolio FM 99.9" easter egg player:
// 24dp card, primary hairline, live equalizer bars, broadcast text box, split CTA row.
export function RadioCard({
  broadcast = "Yo! You're locked into StreamFolio FM 99.9! All the top headlines, zero noise.",
  playing = true, onOpenArticle, onTuneOff, style,
}) {
  const bars = [[6, 24, 350], [18, 8, 280], [10, 28, 420], [22, 10, 310], [8, 20, 390]];
  const pill = { flex: 1, height: 40, borderRadius: 14, cursor: 'pointer', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 6, font: 'var(--sf-weight-semibold) 13px/1 var(--sf-font-sans)' };
  return (
    <div style={{
      margin: '12px 16px', padding: 18, borderRadius: 24,
      background: 'var(--sf-surface-chrome)', border: '1px solid rgb(var(--sf-primary-rgb)/.3)',
      boxShadow: 'var(--sf-shadow-16)', ...style,
    }}>
      <style>{'@keyframes sf-eq{from{height:var(--a)}to{height:var(--b)}}'}</style>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0 }}>
          <div style={{ width: 38, height: 38, borderRadius: '50%', background: 'var(--sf-primary-wash)', display: 'grid', placeItems: 'center', flex: '0 0 auto' }}>
            <Icon name="radio" size={22} color="var(--sf-primary)" />
          </div>
          <div style={{ minWidth: 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ font: 'var(--sf-weight-extrabold) 15px/1 var(--sf-font-sans)', letterSpacing: 'var(--sf-tracking-brand)', color: 'var(--sf-text-strong)' }}>STREAMFOLIO FM 99.9</span>
              <span style={{ borderRadius: 6, padding: '2px 6px', background: 'var(--sf-primary-tint)', font: 'var(--sf-weight-bold) 9px/1 var(--sf-font-sans)', color: 'var(--sf-primary)' }}>LIVE DJ</span>
            </div>
            <div style={{ font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)', color: 'var(--sf-text-muted)' }}>AI News Broadcast Persona</div>
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: 3, height: 28, flex: '0 0 auto' }}>
          {bars.map(([a, b, d], i) => (
            <span key={i} style={{
              width: 4, borderRadius: 2, background: 'var(--sf-primary)',
              height: playing ? a : 6, '--a': a + 'px', '--b': b + 'px',
              animation: playing ? `sf-eq ${d}ms var(--sf-ease-linear) infinite alternate` : 'none',
            }} />
          ))}
        </div>
      </div>

      <div style={{
        marginTop: 14, padding: 14, borderRadius: 14,
        background: 'var(--sf-surface-variant-soft)', border: '1px solid var(--sf-border-soft)',
        font: 'var(--sf-weight-medium) 13px/18px var(--sf-font-sans)', color: 'var(--sf-text-strong)', textWrap: 'pretty',
      }}>{broadcast}</div>

      <div style={{ marginTop: 14, display: 'flex', gap: 10 }}>
        <button type="button" onClick={onTuneOff} style={{ ...pill, background: 'transparent', border: '1px solid var(--sf-border-strong)', color: 'var(--sf-text-body)' }}>
          <Icon name="close" size={16} />Tune Off
        </button>
        <button type="button" onClick={onOpenArticle} style={{ ...pill, background: 'var(--sf-primary)', border: 'none', color: 'var(--sf-on-primary)' }}>
          <Icon name="article" size={16} />Open Article
        </button>
      </div>
    </div>
  );
}
