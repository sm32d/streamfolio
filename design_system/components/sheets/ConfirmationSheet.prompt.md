Used for delete-collection, clear-recent-searches, stop-audio-playback and similar decisions.

```jsx
<ConfirmationSheet icon="stop" title="Stop audio playback?"
  message="Dismissing the player will stop the current article and clear the audio queue."
  primaryLabel="Stop" secondaryLabel="Keep Playing" />
```

Titles are questions ending in "?"; the body explains the consequence in one sentence. Destructive primary uses `--sf-error`. Buttons stack vertically at 48px with 10px gaps — side-by-side pairs appear only in the audio-stop variant.
