Wraps a group of SettingsRow items. Put a `<CardDivider />` between rows — never around the outside.

```jsx
<SettingsCard>
  <SettingsRow icon="tune" iconBg="var(--sf-primary)" title="Preferences" subtitle="Language & region for your news feed" />
  <CardDivider />
  <SettingsRow icon="swipe" iconBg="var(--sf-secondary)" title="Swipe Gestures" subtitle="Customize left & right swipe actions" />
</SettingsCard>
```
