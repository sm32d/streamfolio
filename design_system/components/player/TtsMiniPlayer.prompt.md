Persistent audio control. Renders directly above GlassmorphicNavBar whenever the TTS queue is non-empty; the feed's bottom padding grows from 96px to 160px to make room.

```jsx
<TtsMiniPlayer title="Central banks hold rates steady" source="Reuters" playing sleepTimer="12m" />
```

Same glass treatment as the nav bar: 32px radius, surface at 98%, white 15% hairline, shadow-16. Tapping the body expands the full player; dragging it down past 150px asks for confirmation before clearing the queue.
