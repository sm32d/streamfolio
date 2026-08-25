/* @ds-bundle: {"format":4,"namespace":"StreamFolioDesignSystem_2b0fe2","components":[{"name":"ArticleCard","sourcePath":"components/article/ArticleCard.jsx"},{"name":"PublisherAvatar","sourcePath":"components/article/PublisherAvatar.jsx"},{"name":"TrendingCard","sourcePath":"components/article/TrendingCard.jsx"},{"name":"CollectionGridCard","sourcePath":"components/collections/CollectionGridCard.jsx"},{"name":"AddCollectionGridCard","sourcePath":"components/collections/CollectionGridCard.jsx"},{"name":"CollectionListRow","sourcePath":"components/collections/CollectionListRow.jsx"},{"name":"EmptyState","sourcePath":"components/feedback/EmptyState.jsx"},{"name":"OfflineIndicator","sourcePath":"components/feedback/OfflineIndicator.jsx"},{"name":"SkeletonLoader","sourcePath":"components/feedback/SkeletonLoader.jsx"},{"name":"SwipeHintBanner","sourcePath":"components/feedback/SwipeHintBanner.jsx"},{"name":"ToastBanner","sourcePath":"components/feedback/ToastBanner.jsx"},{"name":"Icon","sourcePath":"components/foundation/Icon.jsx"},{"name":"CategoryPill","sourcePath":"components/navigation/CategoryPill.jsx"},{"name":"GlassmorphicNavBar","sourcePath":"components/navigation/GlassmorphicNavBar.jsx"},{"name":"SubPageTopBar","sourcePath":"components/navigation/SubPageTopBar.jsx"},{"name":"TtsMiniPlayer","sourcePath":"components/player/TtsMiniPlayer.jsx"},{"name":"AiSpotlightBanner","sourcePath":"components/promo/AiSpotlightBanner.jsx"},{"name":"RadioCard","sourcePath":"components/promo/RadioCard.jsx"},{"name":"InfoNote","sourcePath":"components/settings/InfoNote.jsx"},{"name":"SectionLabel","sourcePath":"components/settings/SectionLabel.jsx"},{"name":"SelectorField","sourcePath":"components/settings/SelectorField.jsx"},{"name":"SettingsCard","sourcePath":"components/settings/SettingsCard.jsx"},{"name":"CardDivider","sourcePath":"components/settings/SettingsCard.jsx"},{"name":"SettingsRow","sourcePath":"components/settings/SettingsRow.jsx"},{"name":"StreamFolioSwitch","sourcePath":"components/settings/StreamFolioSwitch.jsx"},{"name":"BottomSheet","sourcePath":"components/sheets/BottomSheet.jsx"},{"name":"ConfirmationSheet","sourcePath":"components/sheets/ConfirmationSheet.jsx"},{"name":"SheetActionRow","sourcePath":"components/sheets/SheetActionRow.jsx"}],"sourceHashes":{"components/article/ArticleCard.jsx":"b071670fc635","components/article/PublisherAvatar.jsx":"7fb124fb16f2","components/article/TrendingCard.jsx":"d517fd7ead3b","components/collections/CollectionGridCard.jsx":"61a4651c3946","components/collections/CollectionListRow.jsx":"4b197bbc849b","components/feedback/EmptyState.jsx":"405d57aedf01","components/feedback/OfflineIndicator.jsx":"f97953d7ab14","components/feedback/SkeletonLoader.jsx":"27ae9edeb6f7","components/feedback/SwipeHintBanner.jsx":"43e1432afcdd","components/feedback/ToastBanner.jsx":"95e267319905","components/foundation/Icon.jsx":"ccabd7a6b81d","components/navigation/CategoryPill.jsx":"02dd4967a426","components/navigation/GlassmorphicNavBar.jsx":"3b3b29fb8ae4","components/navigation/SubPageTopBar.jsx":"407950752e71","components/player/TtsMiniPlayer.jsx":"fef56dcc52a4","components/promo/AiSpotlightBanner.jsx":"9efe04b21d10","components/promo/RadioCard.jsx":"eb5ce9a4dd54","components/settings/InfoNote.jsx":"e300dde1b60d","components/settings/SectionLabel.jsx":"29ca2640400a","components/settings/SelectorField.jsx":"8d986b17cd91","components/settings/SettingsCard.jsx":"9c79af53983f","components/settings/SettingsRow.jsx":"6e966b93512b","components/settings/StreamFolioSwitch.jsx":"b3d159ee91ff","components/sheets/BottomSheet.jsx":"892d01817f96","components/sheets/ConfirmationSheet.jsx":"c7d1a68085ee","components/sheets/SheetActionRow.jsx":"7c2568532028","ui_kits/android-app/App.jsx":"729ec742505e","ui_kits/android-app/BookmarksScreen.jsx":"ddb396d6bfe9","ui_kits/android-app/Chrome.jsx":"360ef723419e","ui_kits/android-app/HomeScreen.jsx":"737751f2ca2b","ui_kits/android-app/ReaderScreen.jsx":"9163d62ba6e9","ui_kits/android-app/SearchScreen.jsx":"768c4ec0f661","ui_kits/android-app/SettingsScreen.jsx":"20823ec8a60d","ui_kits/android-app/data.js":"1ccfb44b100e"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.StreamFolioDesignSystem_2b0fe2 = window.StreamFolioDesignSystem_2b0fe2 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/article/PublisherAvatar.jsx
try { (() => {
// PublisherAvatar.kt — circular, primary @15% fill, hairline ring, favicon or bold initial.
function PublisherAvatar({
  sourceName = '',
  domain,
  size = 40,
  logoUrl,
  ringed = false,
  style
}) {
  const src = logoUrl || (domain ? `https://www.google.com/s2/favicons?sz=64&domain=${domain}` : null);
  const initial = (sourceName.trim()[0] || 'N').toUpperCase();
  return /*#__PURE__*/React.createElement("div", {
    style: {
      width: size,
      height: size,
      borderRadius: '50%',
      overflow: 'hidden',
      display: 'grid',
      placeItems: 'center',
      flex: '0 0 auto',
      background: 'var(--sf-primary-tint)',
      border: ringed ? '2px solid var(--sf-primary)' : '1px solid var(--sf-border-soft)',
      ...style
    }
  }, src ? /*#__PURE__*/React.createElement("img", {
    src: src,
    alt: sourceName,
    width: size,
    height: size,
    style: {
      width: '100%',
      height: '100%',
      objectFit: 'cover'
    }
  }) : /*#__PURE__*/React.createElement("span", {
    style: {
      font: `var(--sf-weight-bold) ${Math.round(size * 0.42)}px/1 var(--sf-font-sans)`,
      color: 'var(--sf-primary)'
    }
  }, initial));
}
Object.assign(__ds_scope, { PublisherAvatar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/article/PublisherAvatar.jsx", error: String((e && e.message) || e) }); }

// components/feedback/SkeletonLoader.jsx
try { (() => {
// SkeletonLoader.kt — light-grey shimmer sweeping on a 1000ms linear loop,
// mirroring the ArticleListItem layout exactly.
const SHIMMER = 'linear-gradient(110deg,rgb(211 211 211/.5) 0%,rgb(211 211 211/.15) 50%,rgb(211 211 211/.5) 100%)';
function Bar({
  w = '100%',
  h = 16,
  r = 4
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      width: w,
      height: h,
      borderRadius: r,
      background: SHIMMER,
      backgroundSize: '200% 100%',
      animation: 'sf-shimmer var(--sf-dur-1000) var(--sf-ease-linear) infinite'
    }
  });
}
function SkeletonLoader({
  rows = 4,
  variant = 'list',
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: variant === 'list' ? '8px 24px' : 0,
      ...style
    }
  }, /*#__PURE__*/React.createElement("style", null, '@keyframes sf-shimmer{from{background-position:200% 0}to{background-position:-200% 0}}'), Array.from({
    length: rows
  }).map((_, i) => variant === 'list' ? /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 16,
      padding: '12px 0'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 96,
      height: 96,
      borderRadius: 16,
      background: SHIMMER,
      backgroundSize: '200% 100%',
      animation: 'sf-shimmer var(--sf-dur-1000) var(--sf-ease-linear) infinite',
      flex: '0 0 auto'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: 'flex',
      flexDirection: 'column'
    }
  }, /*#__PURE__*/React.createElement(Bar, {
    w: "35%",
    h: 12
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(Bar, {
    h: 16
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 6
    }
  }), /*#__PURE__*/React.createElement(Bar, {
    w: "85%",
    h: 16
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 12
    }
  }), /*#__PURE__*/React.createElement(Bar, {
    w: "25%",
    h: 11
  }))) : /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 8,
      padding: '12px 0 28px'
    }
  }, /*#__PURE__*/React.createElement(Bar, {
    w: "88%"
  }), /*#__PURE__*/React.createElement(Bar, null), /*#__PURE__*/React.createElement(Bar, {
    w: "94%"
  }), /*#__PURE__*/React.createElement(Bar, {
    w: "65%"
  }))));
}
Object.assign(__ds_scope, { SkeletonLoader });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/SkeletonLoader.jsx", error: String((e && e.message) || e) }); }

// components/foundation/Icon.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// Material Symbols Rounded glyph. The Android app uses Compose's Material Icons
// (Icons.Default.* / Icons.Outlined.*); Material Symbols is the same icon program's
// current release, so glyph names map almost 1:1 (bookmark, bookmark_add, play_arrow…).
function Icon({
  name,
  size = 20,
  fill = true,
  weight = 400,
  color = 'currentColor',
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("span", _extends({
    "aria-hidden": "true"
  }, rest, {
    style: {
      fontFamily: 'var(--sf-font-icon)',
      fontSize: size,
      lineHeight: 1,
      color,
      display: 'inline-block',
      flex: '0 0 auto',
      userSelect: 'none',
      fontVariationSettings: `"FILL" ${fill ? 1 : 0}, "wght" ${weight}, "GRAD" 0, "opsz" ${size}`,
      ...style
    }
  }), name);
}
Object.assign(__ds_scope, { Icon });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/foundation/Icon.jsx", error: String((e && e.message) || e) }); }

// components/article/ArticleCard.jsx
try { (() => {
// HomeScreen.kt ArticleListItem — 20dp card, 12dp padding, 96dp thumb @16dp radius,
// 11sp primary source line, 14sp bold 2-line title, meta + queue/bookmark actions.
function ArticleCard({
  source,
  title,
  thumbnail,
  publishedAt = '2h ago',
  readingTime = '4 min read',
  bookmarked = false,
  read = false,
  coverageCount = 0,
  onClick,
  onBookmark,
  onQueue,
  onPlay,
  style
}) {
  const initial = (source || '?').trim()[0] || '?';
  const stacked = coverageCount > 0;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 16,
      padding: 12,
      cursor: 'pointer',
      background: 'var(--sf-surface-card)',
      border: '1px solid var(--sf-border-hairline)',
      borderRadius: stacked ? '20px 20px 0 0' : 20,
      opacity: read ? 'var(--sf-alpha-read)' : 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'relative',
      width: 96,
      height: 96,
      borderRadius: 16,
      overflow: 'hidden',
      flex: '0 0 auto',
      display: 'grid',
      placeItems: 'center',
      background: 'var(--sf-gradient-thumb-fallback)'
    }
  }, thumbnail ? /*#__PURE__*/React.createElement("img", {
    src: thumbnail,
    alt: "",
    style: {
      position: 'absolute',
      inset: 0,
      width: '100%',
      height: '100%',
      objectFit: 'cover'
    }
  }) : /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-bold) 32px/1 var(--sf-font-sans)',
      color: 'var(--sf-white)'
    }
  }, initial), /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Play now",
    onClick: e => {
      e.stopPropagation();
      onPlay && onPlay();
    },
    style: {
      position: 'relative',
      width: 36,
      height: 36,
      borderRadius: '50%',
      border: 'none',
      cursor: 'pointer',
      background: 'rgb(0 0 0/.5)',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "play_arrow",
    size: 20,
    color: "var(--sf-white)"
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-bold) 11px/1.2 var(--sf-font-sans)',
      color: 'var(--sf-primary)'
    }
  }, source), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 4,
      font: 'var(--sf-weight-bold) 14px/1.35 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)',
      display: '-webkit-box',
      WebkitLineClamp: 2,
      WebkitBoxOrient: 'vertical',
      overflow: 'hidden',
      textWrap: 'pretty'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 6,
      font: 'var(--sf-weight-regular) 11px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-faint)'
    }
  }, /*#__PURE__*/React.createElement("span", null, publishedAt), /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--sf-text-disabled)'
    }
  }, "\u2022"), /*#__PURE__*/React.createElement("span", null, readingTime)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Add to queue",
    onClick: e => {
      e.stopPropagation();
      onQueue && onQueue();
    },
    style: {
      border: 'none',
      background: 'none',
      padding: 0,
      cursor: 'pointer',
      display: 'grid'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "playlist_add",
    size: 18,
    color: "var(--sf-text-faint)"
  })), /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Bookmark",
    onClick: e => {
      e.stopPropagation();
      onBookmark && onBookmark();
    },
    style: {
      border: 'none',
      background: 'none',
      padding: 0,
      cursor: 'pointer',
      display: 'grid'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: bookmarked ? 'bookmark' : 'bookmark_add',
    fill: bookmarked,
    size: 18,
    color: bookmarked ? 'var(--sf-primary)' : 'var(--sf-text-faint)'
  })))))), stacked && /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '13px 16px 10px',
      borderRadius: '0 0 20px 20px',
      background: 'var(--sf-surface-variant-soft)',
      border: '1px solid rgb(var(--sf-on-surface-rgb)/.05)',
      borderTop: 'none',
      cursor: 'pointer'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8,
      font: 'var(--sf-weight-bold) 12px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-body)'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "library_books",
    size: 16,
    color: "rgb(var(--sf-primary-rgb)/.8)"
  }), "Also covered by ", coverageCount, " other ", coverageCount === 1 ? 'source' : 'sources'), /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "keyboard_arrow_down",
    size: 18,
    color: "var(--sf-text-faint)"
  })));
}
Object.assign(__ds_scope, { ArticleCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/article/ArticleCard.jsx", error: String((e && e.message) || e) }); }

// components/article/TrendingCard.jsx
try { (() => {
// HomeScreen.kt TrendingCard — 280dp tall, 24dp radius, full-bleed image under a bottom scrim,
// "Trending" chip, 16sp title, 48dp primary play FAB, floating queue/bookmark buttons top-right.
function TrendingCard({
  source,
  title,
  category,
  thumbnail,
  publishedAt = '2h ago',
  readingTime = '4 min read',
  bookmarked = false,
  onClick,
  onBookmark,
  onQueue,
  onPlay,
  style
}) {
  const initial = (source || '?').trim()[0] || '?';
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    style: {
      position: 'relative',
      height: 280,
      borderRadius: 24,
      overflow: 'hidden',
      cursor: 'pointer',
      boxShadow: 'var(--sf-shadow-8)',
      background: 'var(--sf-gradient-thumb-fallback)',
      ...style
    }
  }, thumbnail ? /*#__PURE__*/React.createElement("img", {
    src: thumbnail,
    alt: "",
    style: {
      position: 'absolute',
      inset: 0,
      width: '100%',
      height: '100%',
      objectFit: 'cover'
    }
  }) : /*#__PURE__*/React.createElement("span", {
    style: {
      position: 'absolute',
      inset: 0,
      display: 'grid',
      placeItems: 'center',
      font: 'var(--sf-weight-black) 120px/1 var(--sf-font-sans)',
      color: 'rgb(255 255 255/.15)'
    }
  }, initial), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      background: 'var(--sf-gradient-scrim)'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      top: 16,
      right: 16,
      display: 'flex',
      gap: 8
    }
  }, [['playlist_add', 'Add to queue', onQueue], [bookmarked ? 'bookmark' : 'bookmark_add', 'Bookmark', onBookmark]].map(([n, label, fn]) => /*#__PURE__*/React.createElement("button", {
    key: label,
    type: "button",
    "aria-label": label,
    onClick: e => {
      e.stopPropagation();
      fn && fn();
    },
    style: {
      width: 40,
      height: 40,
      borderRadius: '50%',
      border: 'none',
      cursor: 'pointer',
      background: 'rgb(0 0 0/.4)',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: n,
    size: 20,
    color: "var(--sf-white)"
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'flex-end',
      padding: 20,
      gap: 6
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      borderRadius: 8,
      padding: '4px 8px',
      background: 'var(--sf-primary)',
      font: 'var(--sf-weight-bold) 10px/1 var(--sf-font-sans)',
      color: 'var(--sf-on-primary)'
    }
  }, "Trending"), category && /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-medium) 11px/1 var(--sf-font-sans)',
      color: 'rgb(255 255 255/.8)'
    }
  }, category)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-bold) 16px/1.3 var(--sf-font-sans)',
      color: 'var(--sf-white)',
      display: '-webkit-box',
      WebkitLineClamp: 2,
      WebkitBoxOrient: 'vertical',
      overflow: 'hidden',
      textWrap: 'pretty'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'flex-end',
      justifyContent: 'space-between',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-semibold) 12px/1.4 var(--sf-font-sans)',
      color: 'rgb(255 255 255/.7)'
    }
  }, source), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 2,
      display: 'flex',
      alignItems: 'center',
      gap: 6,
      font: 'var(--sf-weight-regular) 11px/1 var(--sf-font-sans)',
      color: 'rgb(255 255 255/.5)'
    }
  }, /*#__PURE__*/React.createElement("span", null, publishedAt), /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'rgb(255 255 255/.4)'
    }
  }, "\u2022"), /*#__PURE__*/React.createElement("span", null, readingTime))), /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Play now",
    onClick: e => {
      e.stopPropagation();
      onPlay && onPlay();
    },
    style: {
      width: 48,
      height: 48,
      borderRadius: '50%',
      border: 'none',
      cursor: 'pointer',
      background: 'var(--sf-primary)',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "play_arrow",
    size: 28,
    color: "var(--sf-on-primary)"
  })))));
}
Object.assign(__ds_scope, { TrendingCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/article/TrendingCard.jsx", error: String((e && e.message) || e) }); }

// components/collections/CollectionGridCard.jsx
try { (() => {
// BookmarkScreen.kt CollectionGridCard — 145dp tall, 20dp radius, cover image under a dark
// gradient (or a primary/secondary container wash), folder tile + count chip, title + item count.
function CollectionGridCard({
  title,
  count = 0,
  thumbnail,
  isDefault = false,
  onClick,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    style: {
      position: 'relative',
      height: 145,
      borderRadius: 20,
      overflow: 'hidden',
      cursor: 'pointer',
      border: '1px solid rgb(var(--sf-on-surface-rgb)/.25)',
      ...style
    }
  }, thumbnail ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("img", {
    src: thumbnail,
    alt: "",
    style: {
      position: 'absolute',
      inset: 0,
      width: '100%',
      height: '100%',
      objectFit: 'cover'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      background: 'linear-gradient(180deg,rgb(0 0 0/.25),rgb(0 0 0/.85))'
    }
  })) : /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      background: 'linear-gradient(135deg,rgb(var(--sf-primary-rgb)/.85),rgb(21 101 192/.7))'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      padding: 14,
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 36,
      height: 36,
      borderRadius: 10,
      background: 'rgb(255 255 255/.22)',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: isDefault ? 'folder_special' : 'folder',
    fill: !isDefault,
    size: 20,
    color: "var(--sf-white)"
  })), /*#__PURE__*/React.createElement("span", {
    style: {
      borderRadius: 12,
      padding: '3px 8px',
      background: 'rgb(0 0 0/.45)',
      font: 'var(--sf-weight-bold) 12px/1 var(--sf-font-sans)',
      color: 'var(--sf-white)'
    }
  }, count)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-bold) 16px/1.25 var(--sf-font-sans)',
      color: 'var(--sf-white)',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)',
      color: 'rgb(255 255 255/.8)'
    }
  }, count === 1 ? '1 item' : `${count} items`))));
}

// AddCollectionGridCard — outlined primary tile that opens the create-collection sheet.
function AddCollectionGridCard({
  onClick,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    style: {
      height: 145,
      borderRadius: 20,
      cursor: 'pointer',
      background: 'rgb(255 255 255/.85)',
      border: '1.5px solid rgb(var(--sf-primary-rgb)/.5)',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 10,
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 44,
      height: 44,
      borderRadius: '50%',
      background: 'var(--sf-primary-wash)',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "add",
    size: 24,
    color: "var(--sf-primary)"
  })), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)',
      color: 'var(--sf-primary)'
    }
  }, "New Collection"));
}
Object.assign(__ds_scope, { CollectionGridCard, AddCollectionGridCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/collections/CollectionGridCard.jsx", error: String((e && e.message) || e) }); }

// components/collections/CollectionListRow.jsx
try { (() => {
// BookmarkScreen.kt CollectionListRow — the list-view alternative: 20dp surface row,
// square cover thumb, title + count, trailing chevron.
function CollectionListRow({
  title,
  count = 0,
  thumbnail,
  isDefault = false,
  onClick,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 14,
      padding: 12,
      borderRadius: 20,
      cursor: 'pointer',
      background: 'var(--sf-surface-card)',
      border: '1px solid var(--sf-border-hairline)',
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 56,
      height: 56,
      borderRadius: 16,
      overflow: 'hidden',
      flex: '0 0 auto',
      display: 'grid',
      placeItems: 'center',
      background: thumbnail ? 'transparent' : 'linear-gradient(135deg,rgb(var(--sf-primary-rgb)/.85),rgb(21 101 192/.7))'
    }
  }, thumbnail ? /*#__PURE__*/React.createElement("img", {
    src: thumbnail,
    alt: "",
    style: {
      width: '100%',
      height: '100%',
      objectFit: 'cover'
    }
  }) : /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: isDefault ? 'folder_special' : 'folder',
    fill: !isDefault,
    size: 24,
    color: "var(--sf-white)"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-bold) 15px/1.3 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)',
      color: 'var(--sf-text-muted)'
    }
  }, count === 1 ? '1 item' : `${count} items`)), /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "keyboard_arrow_right",
    size: 20,
    color: "var(--sf-text-disabled)"
  }));
}
Object.assign(__ds_scope, { CollectionListRow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/collections/CollectionListRow.jsx", error: String((e && e.message) || e) }); }

// components/feedback/EmptyState.jsx
try { (() => {
// EmptyState.kt — centred otter illustration at 55% width, 18sp bold title,
// 14sp/20 secondary description, optional primary CTA.
function EmptyState({
  illustration,
  icon,
  title,
  description,
  actionLabel,
  onAction,
  children,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      textAlign: 'center',
      padding: '24px 32px',
      ...style
    }
  }, illustration ? /*#__PURE__*/React.createElement("img", {
    src: illustration,
    alt: "",
    style: {
      width: '55%',
      maxWidth: 260,
      aspectRatio: '1',
      objectFit: 'contain',
      marginBottom: 16
    }
  }) : icon ? /*#__PURE__*/React.createElement("div", {
    style: {
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 64,
    color: "var(--sf-primary)"
  })) : null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-bold) 18px/1.3 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, title), description && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8,
      maxWidth: 380,
      font: 'var(--sf-weight-regular) 14px/20px var(--sf-font-sans)',
      color: 'var(--sf-text-secondary)',
      textWrap: 'pretty'
    }
  }, description), actionLabel && /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onAction,
    style: {
      marginTop: 24,
      padding: '12px 20px',
      borderRadius: 12,
      border: 'none',
      cursor: 'pointer',
      background: 'var(--sf-primary)',
      color: 'var(--sf-on-primary)',
      font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)'
    }
  }, actionLabel), children);
}
Object.assign(__ds_scope, { EmptyState });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/EmptyState.jsx", error: String((e && e.message) || e) }); }

// components/feedback/OfflineIndicator.jsx
try { (() => {
// OfflineIndicator.kt — full-width 12dp pill, surface @95%, 18dp primary icon, 13sp medium copy.
function OfflineIndicator({
  message = "You're offline. Some content may be unavailable.",
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '8px 24px',
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 10,
      padding: '10px 16px',
      borderRadius: 12,
      background: 'var(--sf-surface-raised)'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "wifi_off",
    size: 18,
    color: "var(--sf-primary)"
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-medium) 13px/1.3 var(--sf-font-sans)',
      color: 'var(--sf-text-body)'
    }
  }, message)));
}
Object.assign(__ds_scope, { OfflineIndicator });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/OfflineIndicator.jsx", error: String((e && e.message) || e) }); }

// components/feedback/SwipeHintBanner.jsx
try { (() => {
// HomeScreen.kt SwipeHintBanner — dismissible 16dp card with a primary hairline,
// teaching the configured swipe actions.
function SwipeHintBanner({
  message = 'Swipe cards to act quickly: left to save, right to share',
  onDismiss,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      margin: '8px 24px',
      padding: '10px 14px',
      borderRadius: 16,
      background: 'var(--sf-surface-raised)',
      border: '1px solid rgb(var(--sf-primary-rgb)/.15)',
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 32,
      height: 32,
      borderRadius: '50%',
      background: 'var(--sf-primary-wash)',
      display: 'grid',
      placeItems: 'center',
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "gesture",
    size: 18,
    fill: false,
    color: "var(--sf-primary)"
  })), /*#__PURE__*/React.createElement("span", {
    style: {
      flex: 1,
      font: 'var(--sf-weight-regular) 13px/18px var(--sf-font-sans)',
      color: 'var(--sf-text-body)'
    }
  }, message), /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Dismiss",
    onClick: onDismiss,
    style: {
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      display: 'grid',
      padding: 8
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "close",
    size: 20,
    color: "var(--sf-text-muted)"
  })));
}
Object.assign(__ds_scope, { SwipeHintBanner });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/SwipeHintBanner.jsx", error: String((e && e.message) || e) }); }

// components/feedback/ToastBanner.jsx
try { (() => {
// BookmarkToastBanner.kt — floating 20dp surfaceVariant banner at 92% width,
// 32dp primary-tinted icon circle, 14sp semibold message, optional text action.
function ToastBanner({
  message,
  icon = 'info',
  actionLabel,
  onAction,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      width: '92%',
      margin: '0 auto',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 12,
      padding: '10px 16px',
      borderRadius: 20,
      background: 'var(--sf-surface-variant)',
      border: '1px solid var(--sf-border)',
      boxShadow: 'var(--sf-shadow-8)',
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 32,
      height: 32,
      borderRadius: '50%',
      background: 'var(--sf-primary-tint)',
      display: 'grid',
      placeItems: 'center',
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 16,
    color: "var(--sf-primary)"
  })), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-semibold) 14px/1.35 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap'
    }
  }, message)), actionLabel && /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onAction,
    style: {
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      padding: '8px 12px',
      font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)',
      color: 'var(--sf-primary)',
      flex: '0 0 auto'
    }
  }, actionLabel));
}
Object.assign(__ds_scope, { ToastBanner });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/ToastBanner.jsx", error: String((e && e.message) || e) }); }

// components/navigation/CategoryPill.jsx
try { (() => {
// CategoryPill.kt — 20dp radius, 14/8 padding, 13sp label, optional leading icon + count badge.
function CategoryPill({
  text,
  selected = false,
  icon,
  badgeCount,
  onClick,
  style
}) {
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onClick,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 6,
      padding: '8px 14px',
      borderRadius: 20,
      cursor: 'pointer',
      font: `${selected ? 'var(--sf-weight-bold)' : 'var(--sf-weight-medium)'} 13px/1.2 var(--sf-font-sans)`,
      background: selected ? 'var(--sf-primary)' : 'var(--sf-surface-pill)',
      color: selected ? 'var(--sf-white)' : 'var(--sf-text-body)',
      border: selected ? '1px solid transparent' : '1px solid var(--sf-border)',
      whiteSpace: 'nowrap',
      transition: 'background var(--sf-dur-150) var(--sf-ease-standard)',
      ...style
    }
  }, icon && /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 16,
    color: selected ? 'var(--sf-white)' : 'var(--sf-text-secondary)'
  }), text, badgeCount > 0 && /*#__PURE__*/React.createElement("span", {
    style: {
      borderRadius: 10,
      padding: '2px 6px',
      font: 'var(--sf-weight-bold) 10px/1 var(--sf-font-sans)',
      background: selected ? 'rgb(255 255 255/.25)' : 'var(--sf-primary-tint)',
      color: selected ? 'var(--sf-white)' : 'var(--sf-primary)'
    }
  }, badgeCount));
}
Object.assign(__ds_scope, { CategoryPill });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/CategoryPill.jsx", error: String((e && e.message) || e) }); }

// components/navigation/GlassmorphicNavBar.jsx
try { (() => {
// GlassmorphicNavBar.kt — floating 32dp pill, 24dp side margin, surface @98%, 1px white hairline.
const TABS = [{
  id: 'home',
  icon: 'home',
  label: 'Home'
}, {
  id: 'search',
  icon: 'search',
  label: 'Search'
}, {
  id: 'bookmarks',
  icon: 'bookmark',
  label: 'Saved'
}, {
  id: 'settings',
  icon: 'settings',
  label: 'Settings'
}];
function GlassmorphicNavBar({
  selected = 'home',
  onSelect,
  tabs = TABS,
  style
}) {
  return /*#__PURE__*/React.createElement("nav", {
    style: {
      padding: '8px 24px',
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 0,
      padding: '8px 12px',
      borderRadius: 32,
      background: 'var(--sf-surface-chrome)',
      backdropFilter: 'var(--sf-blur-chrome)',
      border: '1px solid rgb(255 255 255/.15)',
      boxShadow: 'var(--sf-shadow-16)'
    }
  }, tabs.map(t => {
    const on = t.id === selected;
    const tint = on ? 'var(--sf-primary)' : 'var(--sf-text-muted)';
    return /*#__PURE__*/React.createElement("button", {
      key: t.id,
      type: "button",
      onClick: () => onSelect && onSelect(t.id),
      style: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 2,
        padding: '6px 0',
        border: 'none',
        background: 'none',
        borderRadius: 16,
        cursor: 'pointer'
      }
    }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
      name: t.icon,
      size: 24,
      color: tint
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        font: `var(--sf-weight-regular) 10px/1.2 var(--sf-font-sans)`,
        color: tint
      }
    }, t.label));
  })));
}
Object.assign(__ds_scope, { GlassmorphicNavBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/GlassmorphicNavBar.jsx", error: String((e && e.message) || e) }); }

// components/navigation/SubPageTopBar.jsx
try { (() => {
// SettingsComponents.kt SubPageTopBar — back chevron + 24sp bold title, optional trailing slot.
function SubPageTopBar({
  title,
  onBack,
  trailing,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '8px 0 16px',
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 4
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onBack,
    "aria-label": "Back",
    style: {
      display: 'grid',
      placeItems: 'center',
      width: 48,
      height: 48,
      border: 'none',
      background: 'none',
      borderRadius: '50%',
      cursor: 'pointer'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "arrow_back",
    size: 24,
    color: "var(--sf-text-strong)"
  })), /*#__PURE__*/React.createElement("h2", {
    style: {
      margin: 0,
      font: 'var(--sf-weight-bold) 24px/1.2 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, title)), trailing && /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8
    }
  }, trailing));
}
Object.assign(__ds_scope, { SubPageTopBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/SubPageTopBar.jsx", error: String((e && e.message) || e) }); }

// components/player/TtsMiniPlayer.jsx
try { (() => {
// TtsMiniPlayer.kt — 64dp glass pill, 24dp side margin, 32dp radius, white hairline,
// 40dp thumb, title/source, play-pause + skip. Drag down to dismiss (confirmed by a sheet).
function TtsMiniPlayer({
  title,
  source,
  thumbnail,
  playing = false,
  sleepTimer,
  hasNext = true,
  onToggle,
  onNext,
  onExpand,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '0 24px',
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: onExpand,
    style: {
      height: 64,
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      padding: '8px 16px',
      borderRadius: 32,
      cursor: 'pointer',
      background: 'var(--sf-surface-chrome)',
      backdropFilter: 'var(--sf-blur-chrome)',
      border: '1px solid rgb(255 255 255/.15)',
      boxShadow: 'var(--sf-shadow-16)'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 40,
      height: 40,
      borderRadius: 12,
      overflow: 'hidden',
      flex: '0 0 auto',
      background: 'var(--sf-primary-ghost)',
      display: 'grid',
      placeItems: 'center'
    }
  }, thumbnail ? /*#__PURE__*/React.createElement("img", {
    src: thumbnail,
    alt: "",
    style: {
      width: '100%',
      height: '100%',
      objectFit: 'cover'
    }
  }) : /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "hearing",
    fill: false,
    size: 20,
    color: "var(--sf-primary)"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-bold) 13px/1.3 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8,
      font: 'var(--sf-weight-regular) 11px/1.4 var(--sf-font-sans)',
      color: 'var(--sf-text-secondary)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  }, source), sleepTimer && /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 4,
      color: 'var(--sf-primary)',
      font: 'var(--sf-weight-semibold) 10px/1 var(--sf-font-sans)'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "bedtime",
    size: 12,
    color: "var(--sf-primary)"
  }), sleepTimer))), /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": playing ? 'Pause' : 'Play',
    onClick: e => {
      e.stopPropagation();
      onToggle && onToggle();
    },
    style: {
      width: 36,
      height: 36,
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: playing ? 'pause' : 'play_arrow',
    size: 24,
    color: "var(--sf-primary)"
  })), /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Next",
    disabled: !hasNext,
    onClick: e => {
      e.stopPropagation();
      onNext && onNext();
    },
    style: {
      width: 36,
      height: 36,
      border: 'none',
      background: 'none',
      cursor: hasNext ? 'pointer' : 'default',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "skip_next",
    size: 24,
    color: hasNext ? 'var(--sf-text-strong)' : 'var(--sf-text-disabled)'
  }))));
}
Object.assign(__ds_scope, { TtsMiniPlayer });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/player/TtsMiniPlayer.jsx", error: String((e && e.message) || e) }); }

// components/promo/AiSpotlightBanner.jsx
try { (() => {
// HomeScreen.kt AiSpotlightBanner — the only gradient-filled card in the app:
// indigo→teal, 20dp radius, white-on-gradient copy, white primary CTA.
function AiSpotlightBanner({
  title = 'Upgrade to On-Device AI',
  body = 'Translate foreign feeds offline, read dynamic key summaries, and browse semantic tags. Private and run 100% locally.',
  ctaLabel = 'Enable AI',
  onEnable,
  onDismiss,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      margin: '12px 20px',
      borderRadius: 20,
      padding: 20,
      background: 'var(--sf-gradient-ai)',
      boxShadow: 'var(--sf-shadow-6)',
      display: 'flex',
      flexDirection: 'column',
      gap: 12,
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 36,
      height: 36,
      borderRadius: 10,
      background: 'rgb(255 255 255/.2)',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "auto_awesome",
    size: 18,
    color: "var(--sf-white)"
  })), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-bold) 16px/1.25 var(--sf-font-sans)',
      color: 'var(--sf-white)'
    }
  }, title)), /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      font: 'var(--sf-weight-regular) 13px/18px var(--sf-font-sans)',
      color: 'rgb(255 255 255/.85)',
      textWrap: 'pretty'
    }
  }, body), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onEnable,
    style: {
      padding: '8px 16px',
      borderRadius: 12,
      border: 'none',
      cursor: 'pointer',
      background: 'var(--sf-white)',
      color: 'var(--sf-indigo-500)',
      font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)'
    }
  }, ctaLabel), /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onDismiss,
    style: {
      padding: '8px 12px',
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      color: 'rgb(255 255 255/.7)',
      font: 'var(--sf-weight-medium) 13px/1 var(--sf-font-sans)'
    }
  }, "Dismiss")));
}
Object.assign(__ds_scope, { AiSpotlightBanner });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/promo/AiSpotlightBanner.jsx", error: String((e && e.message) || e) }); }

// components/promo/RadioCard.jsx
try { (() => {
// StreamFolioRadioCard.kt — the "StreamFolio FM 99.9" easter egg player:
// 24dp card, primary hairline, live equalizer bars, broadcast text box, split CTA row.
function RadioCard({
  broadcast = "Yo! You're locked into StreamFolio FM 99.9! All the top headlines, zero noise.",
  playing = true,
  onOpenArticle,
  onTuneOff,
  style
}) {
  const bars = [[6, 24, 350], [18, 8, 280], [10, 28, 420], [22, 10, 310], [8, 20, 390]];
  const pill = {
    flex: 1,
    height: 40,
    borderRadius: 14,
    cursor: 'pointer',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    font: 'var(--sf-weight-semibold) 13px/1 var(--sf-font-sans)'
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      margin: '12px 16px',
      padding: 18,
      borderRadius: 24,
      background: 'var(--sf-surface-chrome)',
      border: '1px solid rgb(var(--sf-primary-rgb)/.3)',
      boxShadow: 'var(--sf-shadow-16)',
      ...style
    }
  }, /*#__PURE__*/React.createElement("style", null, '@keyframes sf-eq{from{height:var(--a)}to{height:var(--b)}}'), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 38,
      height: 38,
      borderRadius: '50%',
      background: 'var(--sf-primary-wash)',
      display: 'grid',
      placeItems: 'center',
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "radio",
    size: 22,
    color: "var(--sf-primary)"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-extrabold) 15px/1 var(--sf-font-sans)',
      letterSpacing: 'var(--sf-tracking-brand)',
      color: 'var(--sf-text-strong)'
    }
  }, "STREAMFOLIO FM 99.9"), /*#__PURE__*/React.createElement("span", {
    style: {
      borderRadius: 6,
      padding: '2px 6px',
      background: 'var(--sf-primary-tint)',
      font: 'var(--sf-weight-bold) 9px/1 var(--sf-font-sans)',
      color: 'var(--sf-primary)'
    }
  }, "LIVE DJ")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)',
      color: 'var(--sf-text-muted)'
    }
  }, "AI News Broadcast Persona"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'flex-end',
      gap: 3,
      height: 28,
      flex: '0 0 auto'
    }
  }, bars.map(([a, b, d], i) => /*#__PURE__*/React.createElement("span", {
    key: i,
    style: {
      width: 4,
      borderRadius: 2,
      background: 'var(--sf-primary)',
      height: playing ? a : 6,
      '--a': a + 'px',
      '--b': b + 'px',
      animation: playing ? `sf-eq ${d}ms var(--sf-ease-linear) infinite alternate` : 'none'
    }
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      padding: 14,
      borderRadius: 14,
      background: 'var(--sf-surface-variant-soft)',
      border: '1px solid var(--sf-border-soft)',
      font: 'var(--sf-weight-medium) 13px/18px var(--sf-font-sans)',
      color: 'var(--sf-text-strong)',
      textWrap: 'pretty'
    }
  }, broadcast), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      display: 'flex',
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onTuneOff,
    style: {
      ...pill,
      background: 'transparent',
      border: '1px solid var(--sf-border-strong)',
      color: 'var(--sf-text-body)'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "close",
    size: 16
  }), "Tune Off"), /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onOpenArticle,
    style: {
      ...pill,
      background: 'var(--sf-primary)',
      border: 'none',
      color: 'var(--sf-on-primary)'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "article",
    size: 16
  }), "Open Article")));
}
Object.assign(__ds_scope, { RadioCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/promo/RadioCard.jsx", error: String((e && e.message) || e) }); }

// components/settings/InfoNote.jsx
try { (() => {
// SettingsComponents.kt InfoNote — 12dp radius surfaceVariant @50% tip row, 12sp/18 copy.
function InfoNote({
  children,
  icon = 'info',
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'flex-start',
      gap: 8,
      padding: '12px 14px',
      borderRadius: 12,
      background: 'var(--sf-surface-variant-soft)',
      ...style
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 16,
    color: "var(--sf-primary)",
    style: {
      marginTop: 1
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-regular) 12px/18px var(--sf-font-sans)',
      color: 'var(--sf-text-secondary)',
      textWrap: 'pretty'
    }
  }, children));
}
Object.assign(__ds_scope, { InfoNote });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/settings/InfoNote.jsx", error: String((e && e.message) || e) }); }

// components/settings/SectionLabel.jsx
try { (() => {
// SettingsComponents.kt SectionLabel — 11sp semibold uppercase, 1sp tracking, onSurface @45%.
function SectionLabel({
  children,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-semibold) 11px/1 var(--sf-font-sans)',
      letterSpacing: 'var(--sf-tracking-label)',
      textTransform: 'uppercase',
      color: 'var(--sf-text-label)',
      ...style
    }
  }, children);
}
Object.assign(__ds_scope, { SectionLabel });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/settings/SectionLabel.jsx", error: String((e && e.message) || e) }); }

// components/settings/SelectorField.jsx
try { (() => {
// SettingsComponents.kt SettingsSelectorField — 56dp tall, 16dp radius, surfaceVariant @50%,
// primary leading icon, 15sp medium value, trailing dropdown caret.
function SelectorField({
  label,
  value,
  icon = 'tune',
  onClick,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      ...style
    }
  }, label && /*#__PURE__*/React.createElement("div", {
    style: {
      marginBottom: 8,
      font: 'var(--sf-weight-semibold) 11px/1 var(--sf-font-sans)',
      letterSpacing: 'var(--sf-tracking-label)',
      textTransform: 'uppercase',
      color: 'var(--sf-text-label)'
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    style: {
      height: 56,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 16px',
      borderRadius: 16,
      cursor: 'pointer',
      background: 'var(--sf-surface-variant-soft)',
      border: '1px solid var(--sf-border-soft)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 20,
    color: "var(--sf-primary)"
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-medium) 15px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, value)), /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "arrow_drop_down",
    size: 24,
    color: "var(--sf-text-faint)"
  })));
}
Object.assign(__ds_scope, { SelectorField });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/settings/SelectorField.jsx", error: String((e && e.message) || e) }); }

// components/settings/SettingsCard.jsx
try { (() => {
// SettingsComponents.kt SettingsCard — 20dp radius, surface @60%, 1px onSurface @7% border.
function SettingsCard({
  children,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      borderRadius: 20,
      overflow: 'hidden',
      background: 'var(--sf-surface-card)',
      border: '1px solid rgb(var(--sf-on-surface-rgb)/.07)',
      display: 'flex',
      flexDirection: 'column',
      ...style
    }
  }, children);
}

// CardDivider — inset hairline between rows.
function CardDivider() {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: 1,
      margin: '0 18px',
      background: 'rgb(var(--sf-on-surface-rgb)/.06)'
    }
  });
}
Object.assign(__ds_scope, { SettingsCard, CardDivider });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/settings/SettingsCard.jsx", error: String((e && e.message) || e) }); }

// components/settings/SettingsRow.jsx
try { (() => {
// SettingsComponents.kt SettingsRow — 40dp solid icon tile @12dp radius, 15sp semibold title,
// 12sp @50% subtitle, trailing chevron at @30%.
function SettingsRow({
  icon,
  iconBg = 'var(--sf-primary)',
  title,
  subtitle,
  trailing,
  onClick,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 14,
      padding: '14px 18px',
      cursor: onClick ? 'pointer' : 'default',
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 14,
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 40,
      height: 40,
      borderRadius: 12,
      background: iconBg,
      display: 'grid',
      placeItems: 'center',
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 20,
    color: "var(--sf-white)"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-semibold) 15px/1.3 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, title), subtitle && /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)',
      color: 'var(--sf-text-muted)'
    }
  }, subtitle))), trailing !== undefined ? trailing : /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "keyboard_arrow_right",
    size: 20,
    color: "var(--sf-text-disabled)"
  }));
}
Object.assign(__ds_scope, { SettingsRow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/settings/SettingsRow.jsx", error: String((e && e.message) || e) }); }

// components/settings/StreamFolioSwitch.jsx
try { (() => {
// SettingsComponents.kt HapticSwitch — M3 switch, white thumb on a primary track when on.
function StreamFolioSwitch({
  checked = false,
  disabled = false,
  onChange,
  style
}) {
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    role: "switch",
    "aria-checked": checked,
    disabled: disabled,
    onClick: () => !disabled && onChange && onChange(!checked),
    style: {
      width: 52,
      height: 32,
      borderRadius: 16,
      padding: 0,
      flex: '0 0 auto',
      cursor: disabled ? 'not-allowed' : 'pointer',
      opacity: disabled ? 0.38 : 1,
      background: checked ? 'var(--sf-primary)' : 'var(--sf-surface-variant)',
      border: checked ? '2px solid var(--sf-primary)' : '2px solid var(--sf-border-strong)',
      position: 'relative',
      transition: 'background var(--sf-dur-150) var(--sf-ease-standard)',
      ...style
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      position: 'absolute',
      top: '50%',
      left: checked ? 26 : 4,
      transform: 'translateY(-50%)',
      width: checked ? 20 : 16,
      height: checked ? 20 : 16,
      borderRadius: '50%',
      background: checked ? 'var(--sf-white)' : 'var(--sf-text-faint)',
      transition: 'left var(--sf-dur-150) var(--sf-ease-standard), width var(--sf-dur-150), height var(--sf-dur-150)'
    }
  }));
}
Object.assign(__ds_scope, { StreamFolioSwitch });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/settings/StreamFolioSwitch.jsx", error: String((e && e.message) || e) }); }

// components/sheets/BottomSheet.jsx
try { (() => {
// The app's only modal surface: ModalBottomSheet with 28dp top corners, a drag handle,
// solid surface fill and 24dp horizontal padding.
function BottomSheet({
  children,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      borderRadius: '28px 28px 0 0',
      background: 'var(--sf-surface)',
      boxShadow: 'var(--sf-shadow-16)',
      paddingBottom: 32,
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      placeItems: 'center',
      padding: '16px 0 8px'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 32,
      height: 4,
      borderRadius: 2,
      background: 'rgb(var(--sf-on-surface-rgb)/.4)'
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '0 24px'
    }
  }, children));
}
Object.assign(__ds_scope, { BottomSheet });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/sheets/BottomSheet.jsx", error: String((e && e.message) || e) }); }

// components/sheets/ConfirmationSheet.jsx
try { (() => {
// ConfirmationModalSheet.kt — icon badge + title, 14sp/20 body, vertically stacked
// 48dp pill CTAs (24dp radius): primary, outlined secondary, optional text tertiary.
function ConfirmationSheet({
  icon = 'delete',
  iconTint = 'var(--sf-error)',
  title,
  message,
  primaryLabel = 'Delete',
  primaryColor = 'var(--sf-error)',
  onPrimary,
  secondaryLabel = 'Cancel',
  onSecondary,
  tertiaryLabel,
  onTertiary,
  style
}) {
  const pill = {
    width: '100%',
    height: 48,
    borderRadius: 24,
    cursor: 'pointer',
    font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)'
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      borderRadius: '28px 28px 0 0',
      background: 'var(--sf-surface)',
      boxShadow: 'var(--sf-shadow-16)',
      paddingBottom: 32,
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      placeItems: 'center',
      padding: '16px 0 8px'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 32,
      height: 4,
      borderRadius: 2,
      background: 'rgb(var(--sf-on-surface-rgb)/.4)'
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '0 24px'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 40,
      height: 40,
      borderRadius: '50%',
      display: 'grid',
      placeItems: 'center',
      background: `color-mix(in srgb, ${iconTint} 12%, transparent)`
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 20,
    color: iconTint
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-bold) 20px/1.25 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, title)), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8,
      font: 'var(--sf-weight-regular) 14px/20px var(--sf-font-sans)',
      color: 'var(--sf-text-secondary)',
      textWrap: 'pretty'
    }
  }, message), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 24,
      display: 'flex',
      flexDirection: 'column',
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onPrimary,
    style: {
      ...pill,
      border: 'none',
      background: primaryColor,
      color: 'var(--sf-white)'
    }
  }, primaryLabel), secondaryLabel && /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onSecondary,
    style: {
      ...pill,
      background: 'transparent',
      border: '1px solid var(--sf-border-strong)',
      color: 'var(--sf-text-strong)'
    }
  }, secondaryLabel), tertiaryLabel && /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onTertiary,
    style: {
      ...pill,
      height: 44,
      background: 'none',
      border: 'none',
      color: 'var(--sf-text-secondary)'
    }
  }, tertiaryLabel))));
}
Object.assign(__ds_scope, { ConfirmationSheet });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/sheets/ConfirmationSheet.jsx", error: String((e && e.message) || e) }); }

// components/sheets/SheetActionRow.jsx
try { (() => {
// HomeScreen.kt BottomSheetActionRow — tinted icon tile, 15sp title, 12sp subtitle.
function SheetActionRow({
  icon,
  iconTint = 'var(--sf-primary)',
  iconBg,
  title,
  subtitle,
  onClick,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 14,
      padding: '10px 0',
      cursor: 'pointer',
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 44,
      height: 44,
      borderRadius: 14,
      display: 'grid',
      placeItems: 'center',
      flex: '0 0 auto',
      background: iconBg || `color-mix(in srgb, ${iconTint} 10%, transparent)`
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 20,
    color: iconTint
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-semibold) 15px/1.3 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, title), subtitle && /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)',
      color: 'var(--sf-text-muted)'
    }
  }, subtitle)));
}
Object.assign(__ds_scope, { SheetActionRow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/sheets/SheetActionRow.jsx", error: String((e && e.message) || e) }); }

// ui_kits/android-app/App.jsx
try { (() => {
const {
  useState
} = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const {
  GlassmorphicNavBar,
  TtsMiniPlayer,
  ToastBanner,
  OfflineIndicator
} = NS;
function App() {
  const [tab, setTab] = useState('home');
  const [article, setArticle] = useState(null);
  const [track, setTrack] = useState(null);
  const [playing, setPlaying] = useState(false);
  const [toast, setToast] = useState(null);
  const showToast = t => {
    setToast(t);
    setTimeout(() => setToast(null), 3500);
  };
  const play = a => {
    setTrack(a);
    setPlaying(true);
    showToast({
      icon: 'hearing',
      message: 'Added to audio playlist'
    });
  };
  const open = a => {
    setArticle(a);
  };
  const screen = article ? /*#__PURE__*/React.createElement(ReaderScreen, {
    article: article,
    onBack: () => setArticle(null),
    onPlay: () => play(article)
  }) : tab === 'home' ? /*#__PURE__*/React.createElement(HomeScreen, {
    onOpenArticle: open
  }) : tab === 'search' ? /*#__PURE__*/React.createElement(SearchScreen, {
    onOpenArticle: open
  }) : tab === 'bookmarks' ? /*#__PURE__*/React.createElement(BookmarksScreen, {
    onOpenArticle: open
  }) : /*#__PURE__*/React.createElement(SettingsScreen, null);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'relative',
      width: 412,
      height: 892,
      overflow: 'hidden',
      borderRadius: 36,
      background: 'var(--sf-gradient-page)',
      display: 'flex',
      flexDirection: 'column',
      fontFamily: 'var(--sf-font-sans)',
      boxShadow: '0 24px 70px rgb(6 13 30/.28)'
    }
  }, /*#__PURE__*/React.createElement(StatusBar, null), screen, /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 0,
      right: 0,
      bottom: 0,
      zIndex: 30,
      paddingBottom: 8
    }
  }, toast && /*#__PURE__*/React.createElement("div", {
    style: {
      paddingBottom: 10
    }
  }, /*#__PURE__*/React.createElement(ToastBanner, toast)), track && /*#__PURE__*/React.createElement("div", {
    style: {
      paddingBottom: 8
    }
  }, /*#__PURE__*/React.createElement(TtsMiniPlayer, {
    title: track.title,
    source: track.source,
    playing: playing,
    onToggle: () => setPlaying(!playing),
    onNext: () => {}
  })), !article && /*#__PURE__*/React.createElement(GlassmorphicNavBar, {
    selected: tab,
    onSelect: setTab
  })));
}
Object.assign(window, {
  App
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/android-app/App.jsx", error: String((e && e.message) || e) }); }

// ui_kits/android-app/BookmarksScreen.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const {
  useState
} = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const {
  CollectionGridCard,
  AddCollectionGridCard,
  CollectionListRow,
  ArticleCard,
  BottomSheet,
  Icon
} = NS;
function BookmarksScreen({
  onOpenArticle
}) {
  const D = window.SF_DATA;
  const [grid, setGrid] = useState(true);
  const [open, setOpen] = useState(null);
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const [collections, setCollections] = useState(D.collections);
  if (open) {
    const items = D.latest.slice(0, 3);
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        alignItems: 'center',
        gap: 4,
        padding: '8px 16px 16px',
        flex: '0 0 auto'
      }
    }, /*#__PURE__*/React.createElement("button", {
      type: "button",
      "aria-label": "Back",
      onClick: () => setOpen(null),
      style: {
        width: 48,
        height: 48,
        border: 'none',
        background: 'none',
        cursor: 'pointer',
        display: 'grid',
        placeItems: 'center'
      }
    }, /*#__PURE__*/React.createElement(Icon, {
      name: "arrow_back",
      size: 24,
      color: "var(--sf-text-strong)"
    })), /*#__PURE__*/React.createElement("h2", {
      style: {
        margin: 0,
        font: 'var(--sf-weight-bold) 24px/1.2 var(--sf-font-sans)',
        color: 'var(--sf-text-strong)'
      }
    }, open.title)), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        overflowY: 'auto',
        paddingBottom: 168,
        display: 'flex',
        flexDirection: 'column',
        gap: 8,
        padding: '0 24px 168px'
      }
    }, items.map(a => /*#__PURE__*/React.createElement(ArticleCard, _extends({
      key: a.id
    }, a, {
      bookmarked: true,
      onClick: () => onOpenArticle(a)
    })))));
  }
  return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Bookmarks",
    subtitle: "Your saved collections and reads",
    trailing: /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        gap: 8
      }
    }, /*#__PURE__*/React.createElement(HeaderIconButton, {
      icon: grid ? 'view_list' : 'grid_view',
      label: "Toggle view",
      onClick: () => setGrid(!grid)
    }), /*#__PURE__*/React.createElement(HeaderIconButton, {
      icon: "add",
      tinted: true,
      label: "Create collection",
      onClick: () => setCreating(true)
    }))
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflowY: 'auto',
      paddingBottom: 168
    }
  }, grid ? /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: '1fr 1fr',
      gap: 16,
      padding: '12px 20px'
    }
  }, collections.map(c => /*#__PURE__*/React.createElement(CollectionGridCard, _extends({
    key: c.id
  }, c, {
    onClick: () => setOpen(c)
  }))), /*#__PURE__*/React.createElement(AddCollectionGridCard, {
    onClick: () => setCreating(true)
  })) : /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12,
      padding: '12px 20px'
    }
  }, collections.map(c => /*#__PURE__*/React.createElement(CollectionListRow, _extends({
    key: c.id
  }, c, {
    onClick: () => setOpen(c)
  }))))), creating && /*#__PURE__*/React.createElement("div", {
    onClick: () => setCreating(false),
    style: {
      position: 'absolute',
      inset: 0,
      background: 'rgb(0 0 0/.4)',
      display: 'flex',
      alignItems: 'flex-end',
      zIndex: 40
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: e => e.stopPropagation(),
    style: {
      width: '100%'
    }
  }, /*#__PURE__*/React.createElement(BottomSheet, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 40,
      height: 40,
      borderRadius: '50%',
      background: 'var(--sf-primary-wash)',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "create_new_folder",
    size: 20,
    color: "var(--sf-primary)"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-bold) 20px/1.25 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, "New collection")), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8,
      font: 'var(--sf-weight-regular) 14px/20px var(--sf-font-sans)',
      color: 'var(--sf-text-secondary)'
    }
  }, "Group saved articles so you can come back to them together."), /*#__PURE__*/React.createElement("input", {
    autoFocus: true,
    value: name,
    onChange: e => setName(e.target.value),
    placeholder: "Collection name",
    style: {
      marginTop: 20,
      width: '100%',
      boxSizing: 'border-box',
      height: 56,
      padding: '0 16px',
      borderRadius: 16,
      outline: 'none',
      background: 'var(--sf-surface-variant-soft)',
      border: '1px solid var(--sf-border-soft)',
      font: 'var(--sf-weight-medium) 15px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }), /*#__PURE__*/React.createElement("button", {
    type: "button",
    disabled: !name.trim(),
    onClick: () => {
      setCollections([...collections, {
        id: 'n' + collections.length,
        title: name.trim(),
        count: 0
      }]);
      setName('');
      setCreating(false);
    },
    style: {
      marginTop: 20,
      width: '100%',
      height: 48,
      borderRadius: 24,
      border: 'none',
      cursor: name.trim() ? 'pointer' : 'default',
      opacity: name.trim() ? 1 : 0.38,
      background: 'var(--sf-primary)',
      color: 'var(--sf-white)',
      font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)'
    }
  }, "Create collection")))));
}
Object.assign(window, {
  BookmarksScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/android-app/BookmarksScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/android-app/Chrome.jsx
try { (() => {
const {
  useState
} = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const {
  Icon
} = NS;

// Android status bar — 24px, transparent over the page gradient (the app draws edge to edge).
function StatusBar({
  dark
}) {
  const c = dark ? 'rgb(240 246 252/.9)' : 'rgb(6 13 30/.75)';
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: 28,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 18px',
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-semibold) 12px/1 var(--sf-font-sans)',
      color: c,
      fontVariantNumeric: 'tabular-nums'
    }
  }, "9:41"), /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'flex',
      gap: 4,
      alignItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "signal_cellular_alt",
    size: 13,
    color: c
  }), /*#__PURE__*/React.createElement(Icon, {
    name: "wifi",
    size: 13,
    color: c
  }), /*#__PURE__*/React.createElement(Icon, {
    name: "battery_full",
    size: 13,
    color: c
  })));
}

// Screen header used by the three list tabs: 34sp title + 14sp subtitle.
function ScreenHeader({
  title,
  subtitle,
  trailing
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '20px 24px 12px',
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'space-between',
      gap: 12,
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", {
    style: {
      margin: 0,
      font: 'var(--sf-weight-bold) 34px/1.1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, title), subtitle && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 4,
      font: 'var(--sf-weight-regular) 14px/1.4 var(--sf-font-sans)',
      color: 'var(--sf-text-muted)'
    }
  }, subtitle)), trailing);
}

// Round icon button used in screen headers (grid/list toggle, add collection).
function HeaderIconButton({
  icon,
  tinted,
  onClick,
  label
}) {
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": label,
    onClick: onClick,
    style: {
      width: 44,
      height: 44,
      borderRadius: 16,
      border: 'none',
      cursor: 'pointer',
      display: 'grid',
      placeItems: 'center',
      background: tinted ? 'rgb(var(--sf-primary-rgb)/.16)' : 'var(--sf-surface-variant-soft)'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: icon,
    size: 22,
    color: tinted ? 'var(--sf-primary)' : 'var(--sf-text-strong)'
  }));
}
function SectionHeading({
  children,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '16px 24px',
      font: 'var(--sf-weight-bold) 18px/1.25 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)',
      ...style
    }
  }, children);
}

// Horizontal scroller with the app's 24px edge padding and 8px gap.
function Strip({
  children,
  gap = 8,
  pad = 24,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap,
      padding: `0 ${pad}px`,
      overflowX: 'auto',
      scrollbarWidth: 'none',
      ...style
    }
  }, children);
}
Object.assign(window, {
  StatusBar,
  ScreenHeader,
  HeaderIconButton,
  SectionHeading,
  Strip
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/android-app/Chrome.jsx", error: String((e && e.message) || e) }); }

// ui_kits/android-app/HomeScreen.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const {
  useState
} = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const {
  CategoryPill,
  ArticleCard,
  TrendingCard,
  PublisherAvatar,
  AiSpotlightBanner,
  SwipeHintBanner,
  OfflineIndicator,
  Icon
} = NS;
function HomeScreen({
  onOpenArticle
}) {
  const D = window.SF_DATA;
  const [category, setCategory] = useState('Top Stories');
  const [publisher, setPublisher] = useState(null);
  const [page, setPage] = useState(0);
  const [seenAi, setSeenAi] = useState(false);
  const [seenHint, setSeenHint] = useState(false);
  const [bookmarks, setBookmarks] = useState({
    a2: true
  });
  const [tag, setTag] = useState(null);
  const toggle = id => setBookmarks(b => ({
    ...b,
    [id]: !b[id]
  }));
  const list = publisher ? D.latest.filter(a => a.source === publisher) : D.latest;
  const today = 'Tuesday, Aug 25';
  return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '16px 24px',
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-black) 28px/1 var(--sf-font-sans)',
      color: 'var(--sf-primary)'
    }
  }, "StreamFolio"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-regular) 13px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-muted)'
    }
  }, today)), /*#__PURE__*/React.createElement(Strip, {
    style: {
      paddingBottom: 12,
      flex: '0 0 auto'
    }
  }, D.categories.map(c => /*#__PURE__*/React.createElement(CategoryPill, {
    key: c,
    text: c,
    selected: c === category,
    onClick: () => setCategory(c)
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflowY: 'auto',
      paddingBottom: 168
    }
  }, !seenAi && /*#__PURE__*/React.createElement(AiSpotlightBanner, {
    onEnable: () => setSeenAi(true),
    onDismiss: () => setSeenAi(true)
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '8px 0'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '0 24px 8px',
      font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-secondary)'
    }
  }, "Smart Tags"), /*#__PURE__*/React.createElement(Strip, null, D.tags.map(t => /*#__PURE__*/React.createElement("button", {
    key: t,
    type: "button",
    onClick: () => setTag(tag === t ? null : t),
    style: {
      border: 'none',
      cursor: 'pointer',
      borderRadius: 12,
      padding: '6px 12px',
      whiteSpace: 'nowrap',
      font: 'var(--sf-weight-medium) 12px/1 var(--sf-font-sans)',
      background: tag === t ? 'var(--sf-primary)' : 'var(--sf-surface-variant-soft)',
      color: tag === t ? 'var(--sf-on-primary)' : 'var(--sf-text-body)'
    }
  }, t)))), !seenHint && /*#__PURE__*/React.createElement(SwipeHintBanner, {
    onDismiss: () => setSeenHint(true)
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '8px 0'
    }
  }, /*#__PURE__*/React.createElement(SectionHeading, {
    style: {
      padding: '8px 24px'
    }
  }, "Publishers"), /*#__PURE__*/React.createElement(Strip, {
    gap: 16
  }, D.publishers.map(p => /*#__PURE__*/React.createElement("button", {
    key: p.name,
    type: "button",
    onClick: () => setPublisher(publisher === p.name ? null : p.name),
    style: {
      border: 'none',
      background: 'none',
      padding: 0,
      cursor: 'pointer',
      width: 64,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 4
    }
  }, /*#__PURE__*/React.createElement(PublisherAvatar, {
    sourceName: p.name,
    domain: p.domain,
    size: 56,
    ringed: publisher === p.name
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-regular) 11px/1.2 var(--sf-font-sans)',
      color: 'var(--sf-text-secondary)',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      maxWidth: 64
    }
  }, p.name))))), /*#__PURE__*/React.createElement(SectionHeading, null, "Trending Highlights"), /*#__PURE__*/React.createElement("div", {
    style: {
      overflowX: 'auto',
      display: 'flex',
      gap: 16,
      padding: '0 24px',
      scrollbarWidth: 'none'
    }
  }, D.trending.map((a, i) => /*#__PURE__*/React.createElement("div", {
    key: a.id,
    style: {
      flex: '0 0 calc(100% - 48px)',
      transform: `scale(${i === page ? 1 : 0.9})`,
      opacity: i === page ? 1 : 0.6,
      transition: 'transform var(--sf-dur-200), opacity var(--sf-dur-200)'
    }
  }, /*#__PURE__*/React.createElement(TrendingCard, _extends({}, a, {
    bookmarked: !!bookmarks[a.id],
    onBookmark: () => toggle(a.id),
    onClick: () => onOpenArticle(a)
  }))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'center',
      gap: 8,
      padding: '16px 0 0'
    }
  }, D.trending.map((_, i) => /*#__PURE__*/React.createElement("button", {
    key: i,
    type: "button",
    "aria-label": `Page ${i + 1}`,
    onClick: () => setPage(i),
    style: {
      border: 'none',
      padding: 0,
      cursor: 'pointer',
      height: 8,
      borderRadius: 4,
      width: i === page ? 24 : 8,
      background: i === page ? 'var(--sf-primary)' : 'rgb(var(--sf-on-surface-rgb)/.4)',
      transition: 'width var(--sf-dur-200) var(--sf-ease-standard)'
    }
  }))), /*#__PURE__*/React.createElement(SectionHeading, null, "Latest Stories"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 8,
      padding: '0 24px'
    }
  }, list.map(a => /*#__PURE__*/React.createElement(ArticleCard, _extends({
    key: a.id
  }, a, {
    bookmarked: !!bookmarks[a.id],
    onBookmark: () => toggle(a.id),
    onClick: () => onOpenArticle(a)
  }))))));
}
Object.assign(window, {
  HomeScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/android-app/HomeScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/android-app/ReaderScreen.jsx
try { (() => {
const {
  useState
} = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const {
  Icon,
  PublisherAvatar,
  SkeletonLoader
} = NS;
function ReaderScreen({
  article,
  onBack,
  onPlay
}) {
  const D = window.SF_DATA;
  const [tab, setTab] = useState('Reader');
  const [summary, setSummary] = useState('idle');
  const [bookmarked, setBookmarked] = useState(false);
  const [serif, setSerif] = useState(false);
  const [size, setSize] = useState(16);
  const [panel, setPanel] = useState(false);
  const initial = (article.source || '?')[0];
  const gen = () => {
    setSummary('loading');
    setTimeout(() => setSummary('done'), 1400);
  };
  return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '8px 16px',
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Back",
    onClick: onBack,
    style: {
      width: 44,
      height: 44,
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "arrow_back",
    size: 24,
    color: "var(--sf-text-strong)"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 0,
      padding: 4,
      borderRadius: 20,
      background: 'var(--sf-surface-variant-soft)'
    }
  }, ['Reader', 'Web'].map(t => /*#__PURE__*/React.createElement("button", {
    key: t,
    type: "button",
    onClick: () => setTab(t),
    style: {
      border: 'none',
      cursor: 'pointer',
      borderRadius: 16,
      padding: '6px 16px',
      font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)',
      background: tab === t ? 'var(--sf-primary)' : 'transparent',
      color: tab === t ? 'var(--sf-on-primary)' : 'var(--sf-text-secondary)'
    }
  }, t))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex'
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Typography",
    onClick: () => setPanel(!panel),
    style: {
      width: 44,
      height: 44,
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "format_size",
    size: 22,
    color: "var(--sf-text-strong)"
  })), /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Bookmark",
    onClick: () => setBookmarked(!bookmarked),
    style: {
      width: 44,
      height: 44,
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: bookmarked ? 'bookmark' : 'bookmark_add',
    fill: bookmarked,
    size: 22,
    color: bookmarked ? 'var(--sf-primary)' : 'var(--sf-text-strong)'
  })))), panel && /*#__PURE__*/React.createElement("div", {
    style: {
      margin: '0 16px 8px',
      padding: 16,
      borderRadius: 16,
      background: 'var(--sf-surface-raised)',
      border: '1px solid var(--sf-border-soft)',
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, "Font Family"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 6
    }
  }, [['Sans-Serif', false], ['Serif', true]].map(([l, v]) => /*#__PURE__*/React.createElement("button", {
    key: l,
    type: "button",
    onClick: () => setSerif(v),
    style: {
      border: serif === v ? 'none' : '1px solid var(--sf-border)',
      cursor: 'pointer',
      borderRadius: 8,
      padding: '5px 10px',
      font: 'var(--sf-weight-medium) 12px/1 var(--sf-font-sans)',
      background: serif === v ? 'var(--sf-primary-tint)' : 'transparent',
      color: serif === v ? 'var(--sf-primary)' : 'var(--sf-text-secondary)'
    }
  }, l)))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, "Text Size (", size, "sp)"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 4
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: () => setSize(Math.max(12, size - 1)),
    style: {
      width: 36,
      height: 36,
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, "A-"), /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: () => setSize(Math.min(30, size + 1)),
    style: {
      width: 36,
      height: 36,
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      font: 'var(--sf-weight-bold) 18px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, "A+")))), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflowY: 'auto',
      paddingBottom: 168
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      margin: '0 24px',
      height: 200,
      borderRadius: 24,
      background: 'var(--sf-gradient-thumb-fallback)',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-bold) 64px/1 var(--sf-font-sans)',
      color: 'rgb(255 255 255/.85)'
    }
  }, initial)), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '20px 24px 0'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-bold) 11px/1 var(--sf-font-sans)',
      letterSpacing: '.5px',
      color: 'var(--sf-primary)'
    }
  }, (article.category || 'Top Stories').toUpperCase()), /*#__PURE__*/React.createElement("h1", {
    style: {
      margin: '8px 0 0',
      font: `var(--sf-weight-extrabold) 26px/1.25 var(--sf-font-sans)`,
      color: 'var(--sf-text-strong)',
      textWrap: 'pretty'
    }
  }, article.title), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 8,
      marginTop: 14,
      flexWrap: 'wrap'
    }
  }, D.tags.slice(0, 2).map(t => /*#__PURE__*/React.createElement("span", {
    key: t,
    style: {
      borderRadius: 8,
      padding: '4px 8px',
      background: 'var(--sf-primary-ghost)',
      font: 'var(--sf-weight-bold) 12px/1.3 var(--sf-font-sans)',
      color: 'var(--sf-primary)'
    }
  }, t))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      marginTop: 16
    }
  }, /*#__PURE__*/React.createElement(PublisherAvatar, {
    sourceName: article.source,
    size: 40
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-semibold) 13px/1.3 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, article.source), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 6,
      font: 'var(--sf-weight-regular) 11px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-muted)'
    }
  }, /*#__PURE__*/React.createElement("span", null, article.publishedAt), /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--sf-text-disabled)'
    }
  }, "\u2022"), /*#__PURE__*/React.createElement("span", null, article.readingTime))), /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onPlay,
    style: {
      marginLeft: 'auto',
      display: 'inline-flex',
      alignItems: 'center',
      gap: 6,
      height: 40,
      padding: '0 16px',
      borderRadius: 20,
      border: 'none',
      cursor: 'pointer',
      background: 'var(--sf-primary)',
      color: 'var(--sf-on-primary)',
      font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "play_arrow",
    size: 18,
    color: "var(--sf-on-primary)"
  }), "Listen"))), /*#__PURE__*/React.createElement("div", {
    style: {
      margin: '20px 24px',
      padding: 18,
      borderRadius: 24,
      background: 'var(--sf-surface-card)',
      border: '1px solid var(--sf-border-soft)'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 8,
      font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "auto_awesome",
    size: 18,
    color: "var(--sf-primary)"
  }), "AI Key Insights"), /*#__PURE__*/React.createElement("span", {
    style: {
      borderRadius: 8,
      padding: '3px 8px',
      background: 'var(--sf-primary-tint)',
      border: '1px solid rgb(var(--sf-primary-rgb)/.3)',
      font: 'var(--sf-weight-semibold) 10px/1.3 var(--sf-font-sans)',
      color: 'var(--sf-primary)'
    }
  }, "On-Device")), summary === 'idle' && /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: gen,
    style: {
      width: '100%',
      height: 44,
      borderRadius: 16,
      border: 'none',
      cursor: 'pointer',
      background: 'var(--sf-primary)',
      color: 'var(--sf-on-primary)',
      font: 'var(--sf-weight-bold) 14px/1 var(--sf-font-sans)',
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "auto_awesome",
    size: 16,
    color: "var(--sf-on-primary)"
  }), "Generate Summary"), summary === 'loading' && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '12px 0',
      textAlign: 'center',
      font: 'var(--sf-weight-regular) 13px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-secondary)'
    }
  }, "Gemini is summarizing\u2026"), summary === 'done' && /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 8
    }
  }, D.summary.map(s => /*#__PURE__*/React.createElement("div", {
    key: s,
    style: {
      display: 'flex',
      gap: 8,
      alignItems: 'flex-start'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 6,
      height: 6,
      borderRadius: '50%',
      background: 'rgb(var(--sf-primary-rgb)/.2)',
      marginTop: 6,
      flex: '0 0 auto'
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-regular) 13px/19px var(--sf-font-sans)',
      color: 'var(--sf-text-body)'
    }
  }, s))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'flex-end',
      gap: 4,
      marginTop: 4
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    style: {
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      display: 'inline-flex',
      alignItems: 'center',
      gap: 4,
      padding: '2px 8px',
      font: 'var(--sf-weight-medium) 11px/1 var(--sf-font-sans)',
      color: 'var(--sf-primary)'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "content_copy",
    size: 14,
    color: "var(--sf-primary)"
  }), "Copy"), /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: gen,
    style: {
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      display: 'inline-flex',
      alignItems: 'center',
      gap: 4,
      padding: '2px 8px',
      font: 'var(--sf-weight-medium) 11px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-secondary)'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "refresh",
    size: 14,
    color: "var(--sf-text-secondary)"
  }), "Regenerate")))), tab === 'Reader' ? /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '0 24px'
    }
  }, D.readerBody.map((p, i) => /*#__PURE__*/React.createElement("p", {
    key: i,
    style: {
      margin: '0 0 18px',
      font: `var(--sf-weight-regular) ${size}px/${Math.round(size * 1.5)}px ${serif ? 'var(--sf-font-serif)' : 'var(--sf-font-sans)'}`,
      letterSpacing: '.5px',
      color: 'var(--sf-text-strong)',
      textWrap: 'pretty'
    }
  }, p))) : /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '0 24px'
    }
  }, /*#__PURE__*/React.createElement(SkeletonLoader, {
    variant: "text",
    rows: 3
  }))));
}
Object.assign(window, {
  ReaderScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/android-app/ReaderScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/android-app/SearchScreen.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const {
  useState,
  useEffect
} = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const {
  CategoryPill,
  ArticleCard,
  EmptyState,
  Icon
} = NS;
const SUGGESTIONS = ['Artificial Intelligence', 'Climate Change', 'Global Economy', 'Space Exploration'];
function SearchScreen({
  onOpenArticle
}) {
  const D = window.SF_DATA;
  const [query, setQuery] = useState('');
  const [recents, setRecents] = useState(D.recentSearches);
  const [placeholder, setPlaceholder] = useState('Search articles...');

  // The app types its own placeholder suggestions in and out on an 80ms/40ms cadence.
  useEffect(() => {
    let i = 0,
      n = 0,
      dir = 1,
      timer;
    const tick = () => {
      const full = `Search "${SUGGESTIONS[i]}"...`;
      n += dir;
      setPlaceholder(full.slice(0, n));
      if (n >= full.length) {
        dir = -1;
        timer = setTimeout(tick, 1800);
        return;
      }
      if (n <= 0) {
        dir = 1;
        i = (i + 1) % SUGGESTIONS.length;
        timer = setTimeout(tick, 400);
        return;
      }
      timer = setTimeout(tick, dir > 0 ? 80 : 40);
    };
    timer = setTimeout(tick, 400);
    return () => clearTimeout(timer);
  }, []);
  const results = query.trim() ? [...D.latest, ...D.trending].filter(a => (a.title + a.source).toLowerCase().includes(query.toLowerCase())) : [];
  return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '24px 24px 20px',
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      margin: 0,
      font: 'var(--sf-weight-bold) 34px/1.1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, "Search"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 4,
      font: 'var(--sf-weight-regular) 14px/1.4 var(--sf-font-sans)',
      color: 'var(--sf-text-muted)'
    }
  }, "Search articles and topics")), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '0 24px 16px',
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      height: 56,
      padding: '0 16px',
      borderRadius: 18,
      background: query ? 'rgb(255 255 255/.8)' : 'var(--sf-surface-variant-soft)',
      border: query ? '2px solid var(--sf-primary)' : '1px solid rgb(var(--sf-on-surface-rgb)/.1)'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "search",
    size: 22,
    color: "var(--sf-primary)"
  }), /*#__PURE__*/React.createElement("input", {
    value: query,
    onChange: e => setQuery(e.target.value),
    placeholder: placeholder,
    style: {
      flex: 1,
      border: 'none',
      outline: 'none',
      background: 'transparent',
      font: 'var(--sf-weight-regular) 15px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }), query && /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Clear",
    onClick: () => setQuery(''),
    style: {
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      display: 'grid'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "close",
    size: 20,
    color: "var(--sf-text-muted)"
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflowY: 'auto',
      paddingBottom: 168
    }
  }, !query.trim() ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '4px 24px 12px',
      font: 'var(--sf-weight-bold) 15px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-secondary)'
    }
  }, "Trending Topics"), /*#__PURE__*/React.createElement(Strip, null, D.trendingTopics.map(t => /*#__PURE__*/React.createElement(CategoryPill, {
    key: t,
    text: t,
    icon: "trending_up",
    onClick: () => setQuery(t)
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '24px 24px 8px'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--sf-weight-bold) 15px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-secondary)'
    }
  }, "Recent Searches"), /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: () => setRecents([]),
    style: {
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      font: 'var(--sf-weight-bold) 13px/1 var(--sf-font-sans)',
      color: 'var(--sf-primary)'
    }
  }, "Clear All")), recents.length ? recents.map(r => /*#__PURE__*/React.createElement("div", {
    key: r,
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 16,
      padding: '12px 24px',
      cursor: 'pointer'
    },
    onClick: () => setQuery(r)
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "history",
    fill: false,
    size: 18,
    color: "var(--sf-text-faint)"
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      flex: 1,
      font: 'var(--sf-weight-regular) 14px/1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, r), /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Remove",
    onClick: e => {
      e.stopPropagation();
      setRecents(recents.filter(x => x !== r));
    },
    style: {
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      display: 'grid'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "close",
    size: 16,
    color: "var(--sf-text-faint)"
  })))) : /*#__PURE__*/React.createElement(EmptyState, {
    illustration: "../../assets/otter-search-welcome.png",
    title: "Start searching",
    description: "Type a keyword to find articles and topics."
  })) : results.length ? /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 8,
      padding: '0 24px'
    }
  }, results.map(a => /*#__PURE__*/React.createElement(ArticleCard, _extends({
    key: a.id
  }, a, {
    onClick: () => onOpenArticle(a)
  })))) : /*#__PURE__*/React.createElement(EmptyState, {
    illustration: "../../assets/otter-no-search.png",
    title: "No results found",
    description: `Nothing matched "${query}". Try a different keyword or check your spelling.`
  })));
}
Object.assign(window, {
  SearchScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/android-app/SearchScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/android-app/SettingsScreen.jsx
try { (() => {
const {
  useState
} = React;
const NS = window.StreamFolioDesignSystem_2b0fe2;
const {
  SettingsCard,
  CardDivider,
  SettingsRow,
  SectionLabel,
  StreamFolioSwitch,
  SelectorField,
  InfoNote,
  RadioCard,
  SubPageTopBar,
  Icon
} = NS;
function SettingsScreen() {
  const [page, setPage] = useState(null);
  const [taps, setTaps] = useState(0);
  const [fm, setFm] = useState(false);
  const [autoplay, setAutoplay] = useState(true);
  const [tags, setTags] = useState(true);
  const [translate, setTranslate] = useState(false);
  if (page === 'preferences') {
    return /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        overflowY: 'auto',
        padding: '0 24px 168px'
      }
    }, /*#__PURE__*/React.createElement(SubPageTopBar, {
      title: "Preferences",
      onBack: () => setPage(null)
    }), /*#__PURE__*/React.createElement(SectionLabel, null, "Feed"), /*#__PURE__*/React.createElement("div", {
      style: {
        height: 10
      }
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        flexDirection: 'column',
        gap: 14
      }
    }, /*#__PURE__*/React.createElement(SelectorField, {
      label: "Region",
      value: "United Kingdom",
      icon: "public"
    }), /*#__PURE__*/React.createElement(SelectorField, {
      label: "Language",
      value: "English",
      icon: "translate"
    }), /*#__PURE__*/React.createElement(InfoNote, null, "Region decides which curated publishers appear by default. You can enable sources from other regions at any time.")));
  }
  return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflowY: 'auto',
      padding: '24px 24px 168px',
      filter: fm ? 'blur(16px)' : 'none'
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      margin: 0,
      font: 'var(--sf-weight-bold) 34px/1.1 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, "Settings"), /*#__PURE__*/React.createElement("div", {
    style: {
      margin: '8px 0 32px',
      font: 'var(--sf-weight-regular) 14px/1.4 var(--sf-font-sans)',
      color: 'var(--sf-text-muted)'
    }
  }, "Customise your StreamFolio experience."), /*#__PURE__*/React.createElement(SectionLabel, null, "Personalisation"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 10
    }
  }), /*#__PURE__*/React.createElement(SettingsCard, null, /*#__PURE__*/React.createElement(SettingsRow, {
    icon: "tune",
    iconBg: "var(--sf-primary)",
    title: "Preferences",
    subtitle: "Language & region for your news feed",
    onClick: () => setPage('preferences')
  }), /*#__PURE__*/React.createElement(CardDivider, null), /*#__PURE__*/React.createElement(SettingsRow, {
    icon: "swipe",
    iconBg: "var(--sf-secondary)",
    title: "Swipe Gestures",
    subtitle: "Customize left & right swipe actions",
    onClick: () => {}
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 32
    }
  }), /*#__PURE__*/React.createElement(SectionLabel, null, "Data Management"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 10
    }
  }), /*#__PURE__*/React.createElement(SettingsCard, null, /*#__PURE__*/React.createElement(SettingsRow, {
    icon: "list",
    iconBg: "var(--sf-tertiary)",
    title: "Manage Content & Sources",
    subtitle: "Customise topics and curation channels",
    onClick: () => {}
  }), /*#__PURE__*/React.createElement(CardDivider, null), /*#__PURE__*/React.createElement(SettingsRow, {
    icon: "rss_feed",
    iconBg: "var(--sf-primary)",
    title: "Custom RSS Feeds",
    subtitle: "Add and manage private RSS publishers",
    onClick: () => {}
  }), /*#__PURE__*/React.createElement(CardDivider, null), /*#__PURE__*/React.createElement(SettingsRow, {
    icon: "backup",
    iconBg: "var(--sf-secondary)",
    title: "Backup & Restore",
    subtitle: "Import/export feeds and settings backup",
    onClick: () => {}
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 32
    }
  }), /*#__PURE__*/React.createElement(SectionLabel, null, "Audio"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 10
    }
  }), /*#__PURE__*/React.createElement(SettingsCard, null, /*#__PURE__*/React.createElement(SettingsRow, {
    icon: "hearing",
    iconBg: "var(--sf-tertiary)",
    title: "Auto-play next article",
    subtitle: "Continue the audio queue without tapping",
    trailing: /*#__PURE__*/React.createElement(StreamFolioSwitch, {
      checked: autoplay,
      onChange: setAutoplay
    })
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 32
    }
  }), /*#__PURE__*/React.createElement(SectionLabel, null, "AI Services"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 10
    }
  }), /*#__PURE__*/React.createElement(SettingsCard, null, /*#__PURE__*/React.createElement(SettingsRow, {
    icon: "auto_awesome",
    iconBg: "var(--sf-tertiary)",
    title: "Smart tags",
    subtitle: "Semantic tags across your feed",
    trailing: /*#__PURE__*/React.createElement(StreamFolioSwitch, {
      checked: tags,
      onChange: setTags
    })
  }), /*#__PURE__*/React.createElement(CardDivider, null), /*#__PURE__*/React.createElement(SettingsRow, {
    icon: "translate",
    iconBg: "var(--sf-primary)",
    title: "Offline translation",
    subtitle: "Translate foreign feeds on-device",
    trailing: /*#__PURE__*/React.createElement(StreamFolioSwitch, {
      checked: translate,
      onChange: setTranslate
    })
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 12
    }
  }), /*#__PURE__*/React.createElement(InfoNote, null, "AI insights require Gemini Nano (AICore). Everything runs locally \u2014 nothing you read leaves the device."), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 32
    }
  }), /*#__PURE__*/React.createElement(SectionLabel, null, "About"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 10
    }
  }), /*#__PURE__*/React.createElement(SettingsCard, null, /*#__PURE__*/React.createElement("div", {
    onClick: () => {
      const n = taps + 1;
      setTaps(n);
      if (n >= 5) {
        setTaps(0);
        setFm(true);
      }
    },
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 14,
      padding: '16px 18px',
      cursor: 'pointer'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 44,
      height: 44,
      borderRadius: 14,
      background: 'var(--sf-gradient-icon)',
      display: 'grid',
      placeItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "settings",
    size: 22,
    color: "var(--sf-white)"
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-semibold) 15px/1.3 var(--sf-font-sans)',
      color: 'var(--sf-text-strong)'
    }
  }, "StreamFolio"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--sf-weight-regular) 12px/1.4 var(--sf-font-sans)',
      color: 'var(--sf-text-label)'
    }
  }, "Version 1.3.0 \xB7 Free forever", taps >= 3 ? ` · ${5 - taps} more taps for StreamFolio FM` : ''))), /*#__PURE__*/React.createElement(CardDivider, null), /*#__PURE__*/React.createElement(SettingsRow, {
    icon: "email",
    iconBg: "var(--sf-primary)",
    title: "Contact Us",
    subtitle: "sfolio@sume.uk",
    onClick: () => {}
  }))), fm && /*#__PURE__*/React.createElement("div", {
    onClick: () => setFm(false),
    style: {
      position: 'absolute',
      inset: 0,
      display: 'grid',
      placeItems: 'center',
      zIndex: 40
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: e => e.stopPropagation(),
    style: {
      width: '100%'
    }
  }, /*#__PURE__*/React.createElement(RadioCard, {
    broadcast: "Now spinning on 99.9 FM: Central banks hold rates steady as inflation cools. From Reuters.",
    onTuneOff: () => setFm(false)
  }))));
}
Object.assign(window, {
  SettingsScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/android-app/SettingsScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/android-app/data.js
try { (() => {
// Sample feed content for the UI kit. No thumbnails are shipped with the repo, so cards
// render the app's authentic fallback: a blue-green tile with the publisher's initial.
window.SF_DATA = {
  categories: ['Top Stories', 'World', 'Business', 'Technology', 'Science', 'Sports', 'Health'],
  publishers: [{
    name: 'Reuters',
    domain: 'reuters.com'
  }, {
    name: 'BBC News',
    domain: 'bbc.co.uk'
  }, {
    name: 'The Verge',
    domain: 'theverge.com'
  }, {
    name: 'Ars Technica',
    domain: 'arstechnica.com'
  }, {
    name: 'Wired',
    domain: 'wired.com'
  }],
  trending: [{
    id: 't1',
    source: 'Reuters',
    category: 'Business',
    title: 'Central banks hold rates steady as inflation cools across major economies',
    publishedAt: '1h ago',
    readingTime: '4 min read'
  }, {
    id: 't2',
    source: 'Ars Technica',
    category: 'Science',
    title: 'Deep-sea sensors reveal an Atlantic current no model predicted',
    publishedAt: '2h ago',
    readingTime: '7 min read'
  }, {
    id: 't3',
    source: 'The Verge',
    category: 'Technology',
    title: 'On-device models are quietly reshaping what phones can do offline',
    publishedAt: '3h ago',
    readingTime: '6 min read'
  }],
  latest: [{
    id: 'a1',
    source: 'BBC News',
    title: 'Port strike ends after eleven days as unions accept revised terms',
    publishedAt: '2h ago',
    readingTime: '3 min read',
    coverageCount: 3
  }, {
    id: 'a2',
    source: 'Wired',
    title: 'The quiet standard that finally made RSS readers pleasant again',
    publishedAt: '4h ago',
    readingTime: '5 min read',
    bookmarked: true
  }, {
    id: 'a3',
    source: 'Reuters',
    title: 'Chipmakers signal a slower second half as inventories normalise',
    publishedAt: '6h ago',
    readingTime: '4 min read'
  }, {
    id: 'a4',
    source: 'The Verge',
    title: 'A folding phone that finally survived our crease test',
    publishedAt: '8h ago',
    readingTime: '6 min read',
    read: true
  }, {
    id: 'a5',
    source: 'Ars Technica',
    title: 'Why local text-to-speech got good enough to listen to all day',
    publishedAt: 'Yesterday',
    readingTime: '9 min read'
  }],
  tags: ['#semiconductors', '#monetary-policy', '#oceanography', '#on-device-ai'],
  trendingTopics: ['Artificial Intelligence', 'Climate Change', 'Global Economy', 'Space Exploration'],
  recentSearches: ['on-device AI', 'shipping strike', 'atlantic current'],
  collections: [{
    id: 'all',
    title: 'All Saved',
    count: 42,
    isDefault: true
  }, {
    id: 'c1',
    title: 'Read later',
    count: 7
  }, {
    id: 'c2',
    title: 'Semiconductors',
    count: 1
  }, {
    id: 'c3',
    title: 'Longreads',
    count: 12
  }],
  readerBody: ['Policymakers left benchmark rates unchanged for a fourth consecutive meeting, pointing to a slow but consistent decline in core inflation and a labour market that has cooled without cracking.', 'The decision was widely expected. What surprised analysts was the tone of the accompanying statement, which dropped a long-standing reference to further tightening and replaced it with language about "sustaining the current stance for as long as necessary".', 'Two members dissented, arguing that services inflation remains too far above target to justify any softening in signal. Their objection was recorded in the minutes but did not change the vote.', 'Markets moved little. Bond yields slipped a few basis points before recovering, and currency traders treated the statement as confirmation of a plateau rather than the start of a pivot.'],
  summary: ['Rates unchanged for a fourth meeting; core inflation is declining steadily.', 'The statement dropped its tightening bias in favour of holding the current stance.', 'Two members dissented over persistent services inflation.', 'Market reaction was muted — read as a plateau, not a pivot.']
};
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/android-app/data.js", error: String((e && e.message) || e) }); }

__ds_ns.ArticleCard = __ds_scope.ArticleCard;

__ds_ns.PublisherAvatar = __ds_scope.PublisherAvatar;

__ds_ns.TrendingCard = __ds_scope.TrendingCard;

__ds_ns.CollectionGridCard = __ds_scope.CollectionGridCard;

__ds_ns.AddCollectionGridCard = __ds_scope.AddCollectionGridCard;

__ds_ns.CollectionListRow = __ds_scope.CollectionListRow;

__ds_ns.EmptyState = __ds_scope.EmptyState;

__ds_ns.OfflineIndicator = __ds_scope.OfflineIndicator;

__ds_ns.SkeletonLoader = __ds_scope.SkeletonLoader;

__ds_ns.SwipeHintBanner = __ds_scope.SwipeHintBanner;

__ds_ns.ToastBanner = __ds_scope.ToastBanner;

__ds_ns.Icon = __ds_scope.Icon;

__ds_ns.CategoryPill = __ds_scope.CategoryPill;

__ds_ns.GlassmorphicNavBar = __ds_scope.GlassmorphicNavBar;

__ds_ns.SubPageTopBar = __ds_scope.SubPageTopBar;

__ds_ns.TtsMiniPlayer = __ds_scope.TtsMiniPlayer;

__ds_ns.AiSpotlightBanner = __ds_scope.AiSpotlightBanner;

__ds_ns.RadioCard = __ds_scope.RadioCard;

__ds_ns.InfoNote = __ds_scope.InfoNote;

__ds_ns.SectionLabel = __ds_scope.SectionLabel;

__ds_ns.SelectorField = __ds_scope.SelectorField;

__ds_ns.SettingsCard = __ds_scope.SettingsCard;

__ds_ns.CardDivider = __ds_scope.CardDivider;

__ds_ns.SettingsRow = __ds_scope.SettingsRow;

__ds_ns.StreamFolioSwitch = __ds_scope.StreamFolioSwitch;

__ds_ns.BottomSheet = __ds_scope.BottomSheet;

__ds_ns.ConfirmationSheet = __ds_scope.ConfirmationSheet;

__ds_ns.SheetActionRow = __ds_scope.SheetActionRow;

})();
