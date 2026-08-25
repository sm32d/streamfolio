const { useState } = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const { CollectionGridCard, AddCollectionGridCard, CollectionListRow, ArticleCard, BottomSheet, Icon } = NS;

function BookmarksScreen({ onOpenArticle }) {
  const D = window.SF_DATA;
  const [grid, setGrid] = useState(true);
  const [open, setOpen] = useState(null);
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const [collections, setCollections] = useState(D.collections);

  if (open) {
    const items = D.latest.slice(0, 3);
    return (
      <>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '8px 16px 16px', flex: '0 0 auto' }}>
          <button type="button" aria-label="Back" onClick={() => setOpen(null)} style={{ width: 48, height: 48, border: 'none', background: 'none', cursor: 'pointer', display: 'grid', placeItems: 'center' }}>
            <Icon name="arrow_back" size={24} color="var(--sf-text-strong)" />
          </button>
          <h2 style={{ margin: 0, font: 'var(--sf-weight-bold) 24px/1.2 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>{open.title}</h2>
        </div>
        <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 168, display: 'flex', flexDirection: 'column', gap: 8, padding: '0 24px 168px' }}>
          {items.map((a) => <ArticleCard key={a.id} {...a} bookmarked onClick={() => onOpenArticle(a)} />)}
        </div>
      </>
    );
  }

  return (
    <>
      <ScreenHeader
        title="Bookmarks" subtitle="Your saved collections and reads"
        trailing={<div style={{ display: 'flex', gap: 8 }}>
          <HeaderIconButton icon={grid ? 'view_list' : 'grid_view'} label="Toggle view" onClick={() => setGrid(!grid)} />
          <HeaderIconButton icon="add" tinted label="Create collection" onClick={() => setCreating(true)} />
        </div>}
      />
      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 168 }}>
        {grid ? (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, padding: '12px 20px' }}>
            {collections.map((c) => <CollectionGridCard key={c.id} {...c} onClick={() => setOpen(c)} />)}
            <AddCollectionGridCard onClick={() => setCreating(true)} />
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, padding: '12px 20px' }}>
            {collections.map((c) => <CollectionListRow key={c.id} {...c} onClick={() => setOpen(c)} />)}
          </div>
        )}
      </div>

      {creating && (
        <div onClick={() => setCreating(false)} style={{ position: 'absolute', inset: 0, background: 'rgb(0 0 0/.4)', display: 'flex', alignItems: 'flex-end', zIndex: 40 }}>
          <div onClick={(e) => e.stopPropagation()} style={{ width: '100%' }}>
            <BottomSheet>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{ width: 40, height: 40, borderRadius: '50%', background: 'var(--sf-primary-wash)', display: 'grid', placeItems: 'center' }}>
                  <Icon name="create_new_folder" size={20} color="var(--sf-primary)" />
                </div>
                <div style={{ font: 'var(--sf-weight-bold) 20px/1.25 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>New collection</div>
              </div>
              <div style={{ marginTop: 8, font: 'var(--sf-weight-regular) 14px/20px var(--sf-font-sans)', color: 'var(--sf-text-secondary)' }}>Group saved articles so you can come back to them together.</div>
              <input
                autoFocus value={name} onChange={(e) => setName(e.target.value)} placeholder="Collection name"
                style={{ marginTop: 20, width: '100%', boxSizing: 'border-box', height: 56, padding: '0 16px', borderRadius: 16, outline: 'none', background: 'var(--sf-surface-variant-soft)', border: '1px solid var(--sf-border-soft)', font: 'var(--sf-weight-medium) 15px/1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}
              />
              <button type="button" disabled={!name.trim()} onClick={() => {
                setCollections([...collections, { id: 'n' + collections.length, title: name.trim(), count: 0 }]);
                setName(''); setCreating(false);
              }} style={{
                marginTop: 20, width: '100%', height: 48, borderRadius: 24, border: 'none',
                cursor: name.trim() ? 'pointer' : 'default', opacity: name.trim() ? 1 : 0.38,
                background: 'var(--sf-primary)', color: 'var(--sf-white)', font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)',
              }}>Create collection</button>
            </BottomSheet>
          </div>
        </div>
      )}
    </>
  );
}

Object.assign(window, { BookmarksScreen });
