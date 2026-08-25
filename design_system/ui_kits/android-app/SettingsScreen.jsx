const { useState } = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const { SettingsCard, CardDivider, SettingsRow, SectionLabel, StreamFolioSwitch, SelectorField, InfoNote, RadioCard, SubPageTopBar, Icon } = NS;

function SettingsScreen() {
  const [page, setPage] = useState(null);
  const [taps, setTaps] = useState(0);
  const [fm, setFm] = useState(false);
  const [autoplay, setAutoplay] = useState(true);
  const [tags, setTags] = useState(true);
  const [translate, setTranslate] = useState(false);

  if (page === 'preferences') {
    return (
      <div style={{ flex: 1, overflowY: 'auto', padding: '0 24px 168px' }}>
        <SubPageTopBar title="Preferences" onBack={() => setPage(null)} />
        <SectionLabel>Feed</SectionLabel>
        <div style={{ height: 10 }} />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <SelectorField label="Region" value="United Kingdom" icon="public" />
          <SelectorField label="Language" value="English" icon="translate" />
          <InfoNote>Region decides which curated publishers appear by default. You can enable sources from other regions at any time.</InfoNote>
        </div>
      </div>
    );
  }

  return (
    <>
      <div style={{ flex: 1, overflowY: 'auto', padding: '24px 24px 168px', filter: fm ? 'blur(16px)' : 'none' }}>
        <h1 style={{ margin: 0, font: 'var(--sf-weight-bold) 34px/1.1 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>Settings</h1>
        <div style={{ margin: '8px 0 32px', font: 'var(--sf-weight-regular) 14px/1.4 var(--sf-font-sans)', color: 'var(--sf-text-muted)' }}>Customise your StreamFolio experience.</div>

        <SectionLabel>Personalisation</SectionLabel>
        <div style={{ height: 10 }} />
        <SettingsCard>
          <SettingsRow icon="tune" iconBg="var(--sf-primary)" title="Preferences" subtitle="Language & region for your news feed" onClick={() => setPage('preferences')} />
          <CardDivider />
          <SettingsRow icon="swipe" iconBg="var(--sf-secondary)" title="Swipe Gestures" subtitle="Customize left & right swipe actions" onClick={() => {}} />
        </SettingsCard>

        <div style={{ height: 32 }} />
        <SectionLabel>Data Management</SectionLabel>
        <div style={{ height: 10 }} />
        <SettingsCard>
          <SettingsRow icon="list" iconBg="var(--sf-tertiary)" title="Manage Content & Sources" subtitle="Customise topics and curation channels" onClick={() => {}} />
          <CardDivider />
          <SettingsRow icon="rss_feed" iconBg="var(--sf-primary)" title="Custom RSS Feeds" subtitle="Add and manage private RSS publishers" onClick={() => {}} />
          <CardDivider />
          <SettingsRow icon="backup" iconBg="var(--sf-secondary)" title="Backup & Restore" subtitle="Import/export feeds and settings backup" onClick={() => {}} />
        </SettingsCard>

        <div style={{ height: 32 }} />
        <SectionLabel>Audio</SectionLabel>
        <div style={{ height: 10 }} />
        <SettingsCard>
          <SettingsRow icon="hearing" iconBg="var(--sf-tertiary)" title="Auto-play next article" subtitle="Continue the audio queue without tapping" trailing={<StreamFolioSwitch checked={autoplay} onChange={setAutoplay} />} />
        </SettingsCard>

        <div style={{ height: 32 }} />
        <SectionLabel>AI Services</SectionLabel>
        <div style={{ height: 10 }} />
        <SettingsCard>
          <SettingsRow icon="auto_awesome" iconBg="var(--sf-tertiary)" title="Smart tags" subtitle="Semantic tags across your feed" trailing={<StreamFolioSwitch checked={tags} onChange={setTags} />} />
          <CardDivider />
          <SettingsRow icon="translate" iconBg="var(--sf-primary)" title="Offline translation" subtitle="Translate foreign feeds on-device" trailing={<StreamFolioSwitch checked={translate} onChange={setTranslate} />} />
        </SettingsCard>
        <div style={{ height: 12 }} />
        <InfoNote>AI insights require Gemini Nano (AICore). Everything runs locally — nothing you read leaves the device.</InfoNote>

        <div style={{ height: 32 }} />
        <SectionLabel>About</SectionLabel>
        <div style={{ height: 10 }} />
        <SettingsCard>
          <div onClick={() => { const n = taps + 1; setTaps(n); if (n >= 5) { setTaps(0); setFm(true); } }}
            style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '16px 18px', cursor: 'pointer' }}>
            <div style={{ width: 44, height: 44, borderRadius: 14, background: 'var(--sf-gradient-icon)', display: 'grid', placeItems: 'center' }}>
              <Icon name="settings" size={22} color="var(--sf-white)" />
            </div>
            <div>
              <div style={{ font: 'var(--sf-weight-semibold) 15px/1.3 var(--sf-font-sans)', color: 'var(--sf-text-strong)' }}>StreamFolio</div>
              <div style={{ font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)', color: 'var(--sf-text-label)' }}>Version 1.3.0 · Free forever{taps >= 3 ? ` · ${5 - taps} more taps for StreamFolio FM` : ''}</div>
            </div>
          </div>
          <CardDivider />
          <SettingsRow icon="email" iconBg="var(--sf-primary)" title="Contact Us" subtitle="sfolio@sume.uk" onClick={() => {}} />
        </SettingsCard>
      </div>

      {fm && (
        <div onClick={() => setFm(false)} style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', zIndex: 40 }}>
          <div onClick={(e) => e.stopPropagation()} style={{ width: '100%' }}>
            <RadioCard broadcast="Now spinning on 99.9 FM: Central banks hold rates steady as inflation cools. From Reuters." onTuneOff={() => setFm(false)} />
          </div>
        </div>
      )}
    </>
  );
}

Object.assign(window, { SettingsScreen });
