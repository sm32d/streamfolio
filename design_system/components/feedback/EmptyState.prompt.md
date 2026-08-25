Every zero-content surface in the app. Pair each case with its own otter illustration.

```jsx
<EmptyState illustration="assets/otter-no-bookmarks.png"
  title="No saved bookmarks"
  description="Swipe right on articles in the Home feed to save them to collections." />
```

Illustration cases: no articles → otter-no-articles, no bookmarks → otter-no-bookmarks, no search results → otter-no-search, pre-search prompt → otter-search-welcome. Use `icon` only when no illustration fits (e.g. "No feeds active" uses `newspaper`). Leave 96px of bottom padding for the nav pill (160px with the mini player up).
