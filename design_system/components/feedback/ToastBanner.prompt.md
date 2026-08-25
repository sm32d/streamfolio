Confirmation for every one-tap action: bookmarking, queueing audio, copying a summary.

```jsx
<ToastBanner icon="bookmark" message={'Saved to "Read later"'} actionLabel="Change" onAction={openSheet} />
```

Sits above the nav pill, 92% width, enters with a 300ms slide-up + fade, exits at 250ms. One toast at a time. Copy is short, sentence case, no trailing period.
