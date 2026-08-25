import React from 'react';

// Material Symbols Rounded glyph. The Android app uses Compose's Material Icons
// (Icons.Default.* / Icons.Outlined.*); Material Symbols is the same icon program's
// current release, so glyph names map almost 1:1 (bookmark, bookmark_add, play_arrow…).
export function Icon({ name, size = 20, fill = true, weight = 400, color = 'currentColor', style, ...rest }) {
  return (
    <span
      aria-hidden="true"
      {...rest}
      style={{
        fontFamily: 'var(--sf-font-icon)',
        fontSize: size,
        lineHeight: 1,
        color,
        display: 'inline-block',
        flex: '0 0 auto',
        userSelect: 'none',
        fontVariationSettings: `"FILL" ${fill ? 1 : 0}, "wght" ${weight}, "GRAD" 0, "opsz" ${size}`,
        ...style,
      }}
    >
      {name}
    </span>
  );
}
