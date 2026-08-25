Bottom navigation for every top-level screen. Position it absolutely over the feed and give the scroll container 96px of bottom padding (160px when the audio mini player is up).

```jsx
<GlassmorphicNavBar selected="home" onSelect={setTab} style={{ position: 'absolute', bottom: 0, left: 0, right: 0 }} />
```

Four tabs only. Selected tab tints icon and label primary; the rest sit at onSurface 50%. Never add a filled indicator pill behind the active tab — the app uses colour alone.
