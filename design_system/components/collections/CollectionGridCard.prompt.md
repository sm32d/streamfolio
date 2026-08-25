The Bookmarks tab's default view: a two-column grid of collections, "All Saved" first and `AddCollectionGridCard` last.

```jsx
<CollectionGridCard title="All Saved" count={42} thumbnail={url} isDefault />
<CollectionGridCard title="Read later" count={7} />
<AddCollectionGridCard onClick={openCreateSheet} />
```

Grid: `gridTemplateColumns: '1fr 1fr'`, 16px gaps, 20px side padding. Fixed 145px height. All text on the tile is white over the scrim — never dark text on the cover.
