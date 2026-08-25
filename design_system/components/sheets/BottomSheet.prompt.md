Container for every modal in StreamFolio: long-press article actions, save-to-collection, create/delete collection, confirmations, region picker.

```jsx
<BottomSheet>
  <h3 style={{margin:0}}>Save to collection</h3>
  <SheetActionRow icon="folder" title="Read later" subtitle="12 items" />
</BottomSheet>
```

28px top corners, drag handle always present, skipPartiallyExpanded (it opens fully). Never build a centred AlertDialog — use ConfirmationSheet.
