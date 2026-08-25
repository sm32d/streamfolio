const { useState } = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const { CategoryPill, ArticleCard, TrendingCard, PublisherAvatar, AiSpotlightBanner, SwipeHintBanner, OfflineIndicator, Icon } = NS;

function HomeScreen({ onOpenArticle }) {
  const D = window.SF_DATA;
  const [category, setCategory] = useState('Top Stories');
  const [publisher, setPublisher] = useState(null);
  const [page, setPage] = useState(0);
  const [seenAi, setSeenAi] = useState(false);
  const [seenHint, setSeenHint] = useState(false);
  const [bookmarks, setBookmarks] = useState({ a2: true });
  const [tag, setTag] = useState(null);

  const toggle = (id) => setBookmarks((b) => ({ ...b, [id]: !b[id] }));
  const list = publisher ? D.latest.filter((a) => a.source === publisher) : D.latest;
  const today = 'Tuesday, Aug 25';

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 24px', flex: '0 0 auto' }}>
        <span style={{ font: 'var(--sf-weight-black) 28px/1 var(--sf-font-sans)', color: 'var(--sf-primary)' }}>StreamFolio</span>
        <span style={{ font: 'var(--sf-weight-regular) 13px/1 var(--sf-font-sans)', color: 'var(--sf-text-muted)' }}>{today}</span>
      </div>

      <Strip style={{ paddingBottom: 12, flex: '0 0 auto' }}>
        {D.categories.map((c) => <CategoryPill key={c} text={c} selected={c === category} onClick={() => setCategory(c)} />)}
      </Strip>

      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 168 }}>
        {!seenAi && <AiSpotlightBanner onEnable={() => setSeenAi(true)} onDismiss={() => setSeenAi(true)} />}

        <div style={{ padding: '8px 0' }}>
          <div style={{ padding: '0 24px 8px', font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)', color: 'var(--sf-text-secondary)' }}>Smart Tags</div>
          <Strip>
            {D.tags.map((t) => (
              <button key={t} type="button" onClick={() => setTag(tag === t ? null : t)} style={{
                border: 'none', cursor: 'pointer', borderRadius: 12, padding: '6px 12px', whiteSpace: 'nowrap',
                font: 'var(--sf-weight-medium) 12px/1 var(--sf-font-sans)',
                background: tag === t ? 'var(--sf-primary)' : 'var(--sf-surface-variant-soft)',
                color: tag === t ? 'var(--sf-on-primary)' : 'var(--sf-text-body)',
              }}>{t}</button>
            ))}
          </Strip>
        </div>

        {!seenHint && <SwipeHintBanner onDismiss={() => setSeenHint(true)} />}

        <div style={{ padding: '8px 0' }}>
          <SectionHeading style={{ padding: '8px 24px' }}>Publishers</SectionHeading>
          <Strip gap={16}>
            {D.publishers.map((p) => (
              <button key={p.name} type="button" onClick={() => setPublisher(publisher === p.name ? null : p.name)} style={{
                border: 'none', background: 'none', padding: 0, cursor: 'pointer', width: 64,
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
              }}>
                <PublisherAvatar sourceName={p.name} domain={p.domain} size={56} ringed={publisher === p.name} />
                <span style={{ font: 'var(--sf-weight-regular) 11px/1.2 var(--sf-font-sans)', color: 'var(--sf-text-secondary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 64 }}>{p.name}</span>
              </button>
            ))}
          </Strip>
        </div>

        <SectionHeading>Trending Highlights</SectionHeading>
        <div style={{ overflowX: 'auto', display: 'flex', gap: 16, padding: '0 24px', scrollbarWidth: 'none' }}>
          {D.trending.map((a, i) => (
            <div key={a.id} style={{ flex: '0 0 calc(100% - 48px)', transform: `scale(${i === page ? 1 : 0.9})`, opacity: i === page ? 1 : 0.6, transition: 'transform var(--sf-dur-200), opacity var(--sf-dur-200)' }}>
              <TrendingCard {...a} bookmarked={!!bookmarks[a.id]} onBookmark={() => toggle(a.id)} onClick={() => onOpenArticle(a)} />
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', justifyContent: 'center', gap: 8, padding: '16px 0 0' }}>
          {D.trending.map((_, i) => (
            <button key={i} type="button" aria-label={`Page ${i + 1}`} onClick={() => setPage(i)} style={{
              border: 'none', padding: 0, cursor: 'pointer', height: 8, borderRadius: 4,
              width: i === page ? 24 : 8,
              background: i === page ? 'var(--sf-primary)' : 'rgb(var(--sf-on-surface-rgb)/.4)',
              transition: 'width var(--sf-dur-200) var(--sf-ease-standard)',
            }} />
          ))}
        </div>

        <SectionHeading>Latest Stories</SectionHeading>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, padding: '0 24px' }}>
          {list.map((a) => (
            <ArticleCard key={a.id} {...a} bookmarked={!!bookmarks[a.id]} onBookmark={() => toggle(a.id)} onClick={() => onOpenArticle(a)} />
          ))}
        </div>
      </div>
    </>
  );
}

Object.assign(window, { HomeScreen });
