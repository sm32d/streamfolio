import React from 'react';

// SettingsComponents.kt HapticSwitch — M3 switch, white thumb on a primary track when on.
export function StreamFolioSwitch({ checked = false, disabled = false, onChange, style }) {
  return (
    <button
      type="button" role="switch" aria-checked={checked} disabled={disabled}
      onClick={() => !disabled && onChange && onChange(!checked)}
      style={{
        width: 52, height: 32, borderRadius: 16, padding: 0, flex: '0 0 auto',
        cursor: disabled ? 'not-allowed' : 'pointer', opacity: disabled ? 0.38 : 1,
        background: checked ? 'var(--sf-primary)' : 'var(--sf-surface-variant)',
        border: checked ? '2px solid var(--sf-primary)' : '2px solid var(--sf-border-strong)',
        position: 'relative', transition: 'background var(--sf-dur-150) var(--sf-ease-standard)', ...style,
      }}
    >
      <span style={{
        position: 'absolute', top: '50%', left: checked ? 26 : 4, transform: 'translateY(-50%)',
        width: checked ? 20 : 16, height: checked ? 20 : 16, borderRadius: '50%',
        background: checked ? 'var(--sf-white)' : 'var(--sf-text-faint)',
        transition: 'left var(--sf-dur-150) var(--sf-ease-standard), width var(--sf-dur-150), height var(--sf-dur-150)',
      }} />
    </button>
  );
}
