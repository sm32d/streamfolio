const { useState } = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const { Icon, PublisherAvatar, SkeletonLoader } = NS;

function ReaderScreen({ article, onBack, onPlay }) {
  const D = window.SF_DATA;
  const [tab, setTab] = useState('Reader');
  const [summary, setSummary] = useState('idle');
  const [bookmarked, setBookmarked] = useState(false);
  const [serif, setSerif] = useState(false);
  const [size, setSize] = useState(16);
  const [panel, setPanel] = useState(false);
  const initial = (article.source || '?')[0];

  const gen = () => { setSummary('loading'); setTimeout(() => setSummary('done'), 1400); };

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 16px', flex: '0 0 auto' }}>
        <button type="button" aria-label="Back" onClick={onBack} style={{ width: 44, height: 44, border: 'none', background: 'none', cursor: 'pointer', display: 'grid', placeItems: 'center' }}>
          <Icon name="arrow_back" size={24} color="var(--sf-text-strong)" />
        </button>
        <div style={{ display: 'flex', gap: 0, padding: 4, borderRadius: 20, background: 'var(--sf-surface-variant-soft)' }}>
          {['Reader', 'Web'].map((t) => (
            <button key={t} type="button" onClick={() => setTab(t)} style={{
              border: 'none', cursor: 'pointer', borderRadius: 16, padding: '6px 16px',
              font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)',
              background: tab === t ? 'var(--sf-primary)' : 'transparent',
              color: tab === t ? 'var(--sf-on-primary)' : 'var(--sf-text-secondary)',
            }}>{t}</button>
          ))}
        </div>
        <div style={{ display: 'flex' }}>
          <button type="button" aria-label="Typography" onClick={() => setPanel(!panel)} style={{ width: 44, height: 44, border: 'none', background: 'none', cursor: 'pointer', display: 'grid', placeItems: 'center' }}>
            <Icon name="format_size" size={22} color="var(--sf-text-strong)" />
          </button>
          <button type="button" aria-label="Bookmark" onClick={() => setBookmarked(!bookmarked)} style={{ width: 44, height: 44, border: 'none', background: 'none', cursor: 'pointer', display: 'grid', placeItems: 'center' }}>
            <Icon name={bookmarked ? 'bookmark' : 'bookmark_add'} fill={bookmarked} size={22} color={bookmarked ? 'var(--sf-primary)' : 'var(--sf-text-strong)'} />
          </button>
        </div>
      </div>

      {panel && (
        <div style={{ margin: '0 16px 8px', padding: 16, borderRadius: 16, background: 'var(--sf-surface-raised)', border: '1px solid var(--sf-border-soft)', flex: '0 0 auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
            <span style={{ font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>Font Family</span>
            <div style={{ display: 'flex', gap: 6 }}>
              {[['Sans-Serif', false], ['Serif', true]].map(([l, v]) => (
                <button key={l} type="button" onClick={() => setSerif(v)} style={{
                  border: serif === v ? 'none' : '1px solid var(--sf-border)', cursor: 'pointer', borderRadius: 8, padding: '5px 10px',
                  font: 'var(--sf-weight-medium) 12px/1 var(--sf-font-sans)',
                  background: serif === v ? 'var(--sf-primary-tint)' : 'transparent',
                  color: serif === v ? 'var(--sf-primary)' : 'var(--sf-text-secondary)',
                }}>{l}</button>
              ))}
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>Text Size ({size}sp)</span>
            <div style={{ display: 'flex', gap: 4 }}>
              <button type="button" onClick={() => setSize(Math.max(12, size - 1))} style={{ width: 36, height: 36, border: 'none', background: 'none', cursor: 'pointer', font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>A-</button>
              <button type="button" onClick={() => setSize(Math.min(30, size + 1))} style={{ width: 36, height: 36, border: 'none', background: 'none', cursor: 'pointer', font: 'var(--sf-weight-bold) 18px/1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>A+</button>
            </div>
          </div>
        </div>
      )}

      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 168 }}>
        <div style={{ margin: '0 24px', height: 200, borderRadius: 24, background: 'var(--sf-gradient-thumb-fallback)', display: 'grid', placeItems: 'center' }}>
          <span style={{ font: 'var(--sf-weight-bold) 64px/1 var(--sf-font-sans)', color: 'rgb(255 255 255/.85)' }}>{initial}</span>
        </div>

        <div style={{ padding: '20px 24px 0' }}>
          <div style={{ font: 'var(--sf-weight-bold) 11px/1 var(--sf-font-sans)', letterSpacing: '.5px', color: 'var(--sf-primary)' }}>{(article.category || 'Top Stories').toUpperCase()}</div>
          <h1 style={{ margin: '8px 0 0', font: `var(--sf-weight-extrabold) 26px/1.25 var(--sf-font-sans)`, color: 'var(--sf-text-strong)', textWrap: 'pretty' }}>{article.title}</h1>
          <div style={{ display: 'flex', gap: 8, marginTop: 14, flexWrap: 'wrap' }}>
            {D.tags.slice(0, 2).map((t) => (
              <span key={t} style={{ borderRadius: 8, padding: '4px 8px', background: 'var(--sf-primary-ghost)', font: 'var(--sf-weight-bold) 12px/1.3 var(--sf-font-sans)', color: 'var(--sf-primary)' }}>{t}</span>
            ))}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 16 }}>
            <PublisherAvatar sourceName={article.source} size={40} />
            <div>
              <div style={{ font: 'var(--sf-weight-semibold) 13px/1.3 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>{article.source}</div>
              <div style={{ display: 'flex', gap: 6, font: 'var(--sf-weight-regular) 11px/1 var(--sf-font-sans)', color: 'var(--sf-text-muted)' }}>
                <span>{article.publishedAt}</span><span style={{ color: 'var(--sf-text-disabled)' }}>•</span><span>{article.readingTime}</span>
              </div>
            </div>
            <button type="button" onClick={onPlay} style={{
              marginLeft: 'auto', display: 'inline-flex', alignItems: 'center', gap: 6, height: 40, padding: '0 16px',
              borderRadius: 20, border: 'none', cursor: 'pointer', background: 'var(--sf-primary)', color: 'var(--sf-on-primary)',
              font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)',
            }}><Icon name="play_arrow" size={18} color="var(--sf-on-primary)" />Listen</button>
          </div>
        </div>

        <div style={{ margin: '20px 24px', padding: 18, borderRadius: 24, background: 'var(--sf-surface-card)', border: '1px solid var(--sf-border-soft)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8, font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>
              <Icon name="auto_awesome" size={18} color="var(--sf-primary)" />AI Key Insights
            </span>
            <span style={{ borderRadius: 8, padding: '3px 8px', background: 'var(--sf-primary-tint)', border: '1px solid rgb(var(--sf-primary-rgb)/.3)', font: 'var(--sf-weight-semibold) 10px/1.3 var(--sf-font-sans)', color: 'var(--sf-primary)' }}>On-Device</span>
          </div>
          {summary === 'idle' && (
            <button type="button" onClick={gen} style={{ width: '100%', height: 44, borderRadius: 16, border: 'none', cursor: 'pointer', background: 'var(--sf-primary)', color: 'var(--sf-on-primary)', font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
              <Icon name="auto_awesome" size={16} color="var(--sf-on-primary)" />Generate Summary
            </button>
          )}
          {summary === 'loading' && <div style={{ padding: '12px 0', textAlign: 'center', font: 'var(--sf-weight-regular) 13px/1 var(--sf-font-sans)', color: 'var(--sf-text-secondary)' }}>Gemini is summarizing…</div>}
          {summary === 'done' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {D.summary.map((s) => (
                <div key={s} style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
                  <span style={{ width: 6, height: 6, borderRadius: '50%', background: 'rgb(var(--sf-primary-rgb)/.2)', marginTop: 6, flex: '0 0 auto' }} />
                  <span style={{ font: 'var(--sf-weight-regular) 13px/19px var(--sf-font-sans)', color: 'var(--sf-text-body)' }}>{s}</span>
                </div>
              ))}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 4, marginTop: 4 }}>
                <button type="button" style={{ border: 'none', background: 'none', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 4, padding: '2px 8px', font: 'var(--sf-weight-medium) 11px/1 var(--sf-font-sans)', color: 'var(--sf-primary)' }}><Icon name="content_copy" size={14} color="var(--sf-primary)" />Copy</button>
                <button type="button" onClick={gen} style={{ border: 'none', background: 'none', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 4, padding: '2px 8px', font: 'var(--sf-weight-medium) 11px/1 var(--sf-font-sans)', color: 'var(--sf-text-secondary)' }}><Icon name="refresh" size={14} color="var(--sf-text-secondary)" />Regenerate</button>
              </div>
            </div>
          )}
        </div>

        {tab === 'Reader' ? (
          <div style={{ padding: '0 24px' }}>
            {D.readerBody.map((p, i) => (
              <p key={i} style={{
                margin: '0 0 18px',
                font: `var(--sf-weight-regular) ${size}px/${Math.round(size * 1.5)}px ${serif ? 'var(--sf-font-serif)' : 'var(--sf-font-sans)'}`,
                letterSpacing: '.5px', color: 'var(--sf-text-strong)', textWrap: 'pretty',
              }}>{p}</p>
            ))}
          </div>
        ) : (
          <div style={{ padding: '0 24px' }}><SkeletonLoader variant="text" rows={3} /></div>
        )}
      </div>
    </>
  );
}

Object.assign(window, { ReaderScreen });
