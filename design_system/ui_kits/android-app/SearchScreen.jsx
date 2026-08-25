const { useState, useEffect } = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const { CategoryPill, ArticleCard, EmptyState, Icon } = NS;

const SUGGESTIONS = ['Artificial Intelligence', 'Climate Change', 'Global Economy', 'Space Exploration'];

function SearchScreen({ onOpenArticle }) {
  const D = window.SF_DATA;
  const [query, setQuery] = useState('');
  const [recents, setRecents] = useState(D.recentSearches);
  const [placeholder, setPlaceholder] = useState('Search articles...');

  // The app types its own placeholder suggestions in and out on an 80ms/40ms cadence.
  useEffect(() => {
    let i = 0, n = 0, dir = 1, timer;
    const tick = () => {
      const full = `Search "${SUGGESTIONS[i]}"...`;
      n += dir;
      setPlaceholder(full.slice(0, n));
      if (n >= full.length) { dir = -1; timer = setTimeout(tick, 1800); return; }
      if (n <= 0) { dir = 1; i = (i + 1) % SUGGESTIONS.length; timer = setTimeout(tick, 400); return; }
      timer = setTimeout(tick, dir > 0 ? 80 : 40);
    };
    timer = setTimeout(tick, 400);
    return () => clearTimeout(timer);
  }, []);

  const results = query.trim()
    ? [...D.latest, ...D.trending].filter((a) => (a.title + a.source).toLowerCase().includes(query.toLowerCase()))
    : [];

  return (
    <>
      <div style={{ padding: '24px 24px 20px', flex: '0 0 auto' }}>
        <h1 style={{ margin: 0, font: 'var(--sf-weight-bold) 34px/1.1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>Search</h1>
        <div style={{ marginTop: 4, font: 'var(--sf-weight-regular) 14px/1.4 var(--sf-font-sans)', color: 'var(--sf-text-muted)' }}>Search articles and topics</div>
      </div>

      <div style={{ padding: '0 24px 16px', flex: '0 0 auto' }}>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 12, height: 56, padding: '0 16px', borderRadius: 18,
          background: query ? 'rgb(255 255 255/.8)' : 'var(--sf-surface-variant-soft)',
          border: query ? '2px solid var(--sf-primary)' : '1px solid rgb(var(--sf-on-surface-rgb)/.1)',
        }}>
          <Icon name="search" size={22} color="var(--sf-primary)" />
          <input
            value={query} onChange={(e) => setQuery(e.target.value)} placeholder={placeholder}
            style={{ flex: 1, border: 'none', outline: 'none', background: 'transparent', font: 'var(--sf-weight-regular) 15px/1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}
          />
          {query && (
            <button type="button" aria-label="Clear" onClick={() => setQuery('')} style={{ border: 'none', background: 'none', cursor: 'pointer', display: 'grid' }}>
              <Icon name="close" size={20} color="var(--sf-text-muted)" />
            </button>
          )}
        </div>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 168 }}>
        {!query.trim() ? (
          <>
            <div style={{ padding: '4px 24px 12px', font: 'var(--sf-weight-bold) 15px/1 var(--sf-font-sans)', color: 'var(--sf-text-secondary)' }}>Trending Topics</div>
            <Strip>
              {D.trendingTopics.map((t) => <CategoryPill key={t} text={t} icon="trending_up" onClick={() => setQuery(t)} />)}
            </Strip>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '24px 24px 8px' }}>
              <span style={{ font: 'var(--sf-weight-bold) 15px/1 var(--sf-font-sans)', color: 'var(--sf-text-secondary)' }}>Recent Searches</span>
              <button type="button" onClick={() => setRecents([])} style={{ border: 'none', background: 'none', cursor: 'pointer', font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)', color: 'var(--sf-primary)' }}>Clear All</button>
            </div>
            {recents.length ? recents.map((r) => (
              <div key={r} style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '12px 24px', cursor: 'pointer' }} onClick={() => setQuery(r)}>
                <Icon name="history" fill={false} size={18} color="var(--sf-text-faint)" />
                <span style={{ flex: 1, font: 'var(--sf-weight-regular) 14px/1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>{r}</span>
                <button type="button" aria-label="Remove" onClick={(e) => { e.stopPropagation(); setRecents(recents.filter((x) => x !== r)); }} style={{ border: 'none', background: 'none', cursor: 'pointer', display: 'grid' }}>
                  <Icon name="close" size={16} color="var(--sf-text-faint)" />
                </button>
              </div>
            )) : (
              <EmptyState illustration="../../assets/otter-search-welcome.png" title="Start searching" description="Type a keyword to find articles and topics." />
            )}
          </>
        ) : results.length ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, padding: '0 24px' }}>
            {results.map((a) => <ArticleCard key={a.id} {...a} onClick={() => onOpenArticle(a)} />)}
          </div>
        ) : (
          <EmptyState illustration="../../assets/otter-no-search.png" title="No results found" description={`Nothing matched "${query}". Try a different keyword or check your spelling.`} />
        )}
      </div>
    </>
  );
}

Object.assign(window, { SearchScreen });
