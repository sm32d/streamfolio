The long-press action menu on an article card. Unlike SettingsRow, the icon tile is a 10% tint of the action's own colour, not a solid brand fill.

```jsx
<SheetActionRow icon="play_arrow" title="Play Next" subtitle="Insert next in the audio playlist queue" />
<SheetActionRow icon="share" iconTint="var(--sf-amber-500)" title="Share Article" subtitle="Send this article link to other apps" />
```

The app's fixed action colours: Play Next → primary, Add to Queue → `--sf-green-500`, Bookmark → secondary (or `--sf-red-400` when removing), Read status → `--sf-cyan-400`, Share → `--sf-amber-500`. Rows are 12px apart.
