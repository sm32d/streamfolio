Every Settings entry. Icon tiles are solid brand fills, never tinted washes.

```jsx
<SettingsRow icon="rss_feed" iconBg="var(--sf-primary)" title="Custom RSS Feeds"
  subtitle="Add and manage private RSS publishers" onClick={go} />
<SettingsRow icon="hearing" iconBg="var(--sf-tertiary)" title="Read aloud"
  subtitle="Auto-play the next article" trailing={<StreamFolioSwitch checked />} />
```

Rotate `iconBg` between `--sf-primary`, `--sf-secondary` and `--sf-tertiary` down a card so adjacent rows differ. Subtitles are short sentence-case phrases with no trailing period.
