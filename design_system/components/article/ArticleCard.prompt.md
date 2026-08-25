The workhorse of the Home and Bookmarks feeds — one article per row.

```jsx
<ArticleCard source="Reuters" title="Central banks hold rates steady as inflation cools"
  thumbnail={url} publishedAt="2h ago" readingTime="4 min read" coverageCount={3} />
```

Stack these in a column with 24px side gutters and 8px vertical gaps. `read` dims the whole card to 55%. When `coverageCount > 0` the card's bottom corners square off and the grouped-coverage bar sits flush beneath it. Swipe actions (save / share / mark read) are configurable in Settings — represent them with the swipe background colours, not extra buttons.
