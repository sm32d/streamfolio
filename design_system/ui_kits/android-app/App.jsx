const { useState } = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const { GlassmorphicNavBar, TtsMiniPlayer, ToastBanner, OfflineIndicator } = NS;

function App() {
  const [tab, setTab] = useState('home');
  const [article, setArticle] = useState(null);
  const [track, setTrack] = useState(null);
  const [playing, setPlaying] = useState(false);
  const [toast, setToast] = useState(null);

  const showToast = (t) => { setToast(t); setTimeout(() => setToast(null), 3500); };
  const play = (a) => { setTrack(a); setPlaying(true); showToast({ icon: 'hearing', message: 'Added to audio playlist' }); };
  const open = (a) => { setArticle(a); };

  const screen = article
    ? <ReaderScreen article={article} onBack={() => setArticle(null)} onPlay={() => play(article)} />
    : tab === 'home' ? <HomeScreen onOpenArticle={open} />
    : tab === 'search' ? <SearchScreen onOpenArticle={open} />
    : tab === 'bookmarks' ? <BookmarksScreen onOpenArticle={open} />
    : <SettingsScreen />;

  return (
    <div style={{
      position: 'relative', width: 412, height: 892, overflow: 'hidden',
      borderRadius: 36, background: 'var(--sf-gradient-page)',
      display: 'flex', flexDirection: 'column', fontFamily: 'var(--sf-font-sans)',
      boxShadow: '0 24px 70px rgb(6 13 30/.28)',
    }}>
      <StatusBar />
      {screen}

      <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 30, paddingBottom: 8 }}>
        {toast && <div style={{ paddingBottom: 10 }}><ToastBanner {...toast} /></div>}
        {track && <div style={{ paddingBottom: 8 }}>
          <TtsMiniPlayer title={track.title} source={track.source} playing={playing} onToggle={() => setPlaying(!playing)} onNext={() => {}} />
        </div>}
        {!article && <GlassmorphicNavBar selected={tab} onSelect={setTab} />}
      </div>
    </div>
  );
}

Object.assign(window, { App });
