const { useState } = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const { Icon } = NS;

// Android status bar — 24px, transparent over the page gradient (the app draws edge to edge).
function StatusBar({ dark }) {
  const c = dark ? 'rgb(240 246 252/.9)' : 'rgb(6 13 30/.75)';
  return (
    <div style={{ height: 28, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 18px', flex: '0 0 auto' }}>
      <span style={{ font: 'var(--sf-weight-semibold) 12px/1 var(--sf-font-sans)', color: c, fontVariantNumeric: 'tabular-nums' }}>9:41</span>
      <span style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
        <Icon name="signal_cellular_alt" size={13} color={c} />
        <Icon name="wifi" size={13} color={c} />
        <Icon name="battery_full" size={13} color={c} />
      </span>
    </div>
  );
}

// Screen header used by the three list tabs: 34sp title + 14sp subtitle.
function ScreenHeader({ title, subtitle, trailing }) {
  return (
    <div style={{ padding: '20px 24px 12px', display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12, flex: '0 0 auto' }}>
      <div>
        <h1 style={{ margin: 0, font: 'var(--sf-weight-bold) 34px/1.1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>{title}</h1>
        {subtitle && <div style={{ marginTop: 4, font: 'var(--sf-weight-regular) 14px/1.4 var(--sf-font-sans)', color: 'var(--sf-text-muted)' }}>{subtitle}</div>}
      </div>
      {trailing}
    </div>
  );
}

// Round icon button used in screen headers (grid/list toggle, add collection).
function HeaderIconButton({ icon, tinted, onClick, label }) {
  return (
    <button type="button" aria-label={label} onClick={onClick} style={{
      width: 44, height: 44, borderRadius: 16, border: 'none', cursor: 'pointer', display: 'grid', placeItems: 'center',
      background: tinted ? 'rgb(var(--sf-primary-rgb)/.16)' : 'var(--sf-surface-variant-soft)',
    }}>
      <Icon name={icon} size={22} color={tinted ? 'var(--sf-primary)' : 'var(--sf-text-strong)'} />
    </button>
  );
}

function SectionHeading({ children, style }) {
  return <div style={{ padding: '16px 24px', font: 'var(--sf-weight-bold) 18px/1.25 var(--sf-font-sans)', color: 'var(--sf-text-strong)', ...style }}>{children}</div>;
}

// Horizontal scroller with the app's 24px edge padding and 8px gap.
function Strip({ children, gap = 8, pad = 24, style }) {
  return (
    <div style={{ display: 'flex', gap, padding: `0 ${pad}px`, overflowX: 'auto', scrollbarWidth: 'none', ...style }}>{children}</div>
  );
}

Object.assign(window, { StatusBar, ScreenHeader, HeaderIconButton, SectionHeading, Strip });
