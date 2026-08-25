Shown only on a cold load of a category (no cached articles) and while the reader parses an article body.

```jsx
<SkeletonLoader rows={4} />
<SkeletonLoader variant="text" rows={3} />
```

The skeleton must match the real layout it replaces — 96px thumb, 4 text bars at 35% / 100% / 85% / 25%. Pull-to-refresh over existing content uses a 3px linear progress bar instead, never this.
