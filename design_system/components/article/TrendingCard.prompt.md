Hero card for the top three stories of a category, shown in a horizontal pager with 24px content padding and 16px page spacing.

```jsx
<TrendingCard source="The Verge" category="Technology" title="…" thumbnail={url} />
```

Fixed 280px height. Off-centre pages scale to 0.85 and fade to 0.5 alpha; below the pager sits a row of dot indicators where the active dot stretches to a 24×8 pill. Only the play button is primary-filled — queue and bookmark stay as black-scrim circles.
