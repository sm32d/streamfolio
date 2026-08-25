The feed's category tab — used in the Home tab strip and as a trending-topic chip on Search.

```jsx
<CategoryPill text="Top Stories" selected onClick={() => setCat('Top Stories')} />
<CategoryPill text="Climate Change" icon="trending_up" />
```

Always render a row of these inside a horizontally scrolling flex container with `gap: 8px` and 24px side padding. Selected = primary fill; unselected = translucent surface with a hairline border. `badgeCount` is for unread counts on custom feeds.
