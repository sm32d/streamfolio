// Sample feed content for the UI kit. No thumbnails are shipped with the repo, so cards
// render the app's authentic fallback: a blue-green tile with the publisher's initial.
window.SF_DATA = {
  categories: ['Top Stories', 'World', 'Business', 'Technology', 'Science', 'Sports', 'Health'],
  publishers: [
    { name: 'Reuters', domain: 'reuters.com' },
    { name: 'BBC News', domain: 'bbc.co.uk' },
    { name: 'The Verge', domain: 'theverge.com' },
    { name: 'Ars Technica', domain: 'arstechnica.com' },
    { name: 'Wired', domain: 'wired.com' },
  ],
  trending: [
    { id: 't1', source: 'Reuters', category: 'Business', title: 'Central banks hold rates steady as inflation cools across major economies', publishedAt: '1h ago', readingTime: '4 min read' },
    { id: 't2', source: 'Ars Technica', category: 'Science', title: 'Deep-sea sensors reveal an Atlantic current no model predicted', publishedAt: '2h ago', readingTime: '7 min read' },
    { id: 't3', source: 'The Verge', category: 'Technology', title: 'On-device models are quietly reshaping what phones can do offline', publishedAt: '3h ago', readingTime: '6 min read' },
  ],
  latest: [
    { id: 'a1', source: 'BBC News', title: 'Port strike ends after eleven days as unions accept revised terms', publishedAt: '2h ago', readingTime: '3 min read', coverageCount: 3 },
    { id: 'a2', source: 'Wired', title: 'The quiet standard that finally made RSS readers pleasant again', publishedAt: '4h ago', readingTime: '5 min read', bookmarked: true },
    { id: 'a3', source: 'Reuters', title: 'Chipmakers signal a slower second half as inventories normalise', publishedAt: '6h ago', readingTime: '4 min read' },
    { id: 'a4', source: 'The Verge', title: 'A folding phone that finally survived our crease test', publishedAt: '8h ago', readingTime: '6 min read', read: true },
    { id: 'a5', source: 'Ars Technica', title: 'Why local text-to-speech got good enough to listen to all day', publishedAt: 'Yesterday', readingTime: '9 min read' },
  ],
  tags: ['#semiconductors', '#monetary-policy', '#oceanography', '#on-device-ai'],
  trendingTopics: ['Artificial Intelligence', 'Climate Change', 'Global Economy', 'Space Exploration'],
  recentSearches: ['on-device AI', 'shipping strike', 'atlantic current'],
  collections: [
    { id: 'all', title: 'All Saved', count: 42, isDefault: true },
    { id: 'c1', title: 'Read later', count: 7 },
    { id: 'c2', title: 'Semiconductors', count: 1 },
    { id: 'c3', title: 'Longreads', count: 12 },
  ],
  readerBody: [
    'Policymakers left benchmark rates unchanged for a fourth consecutive meeting, pointing to a slow but consistent decline in core inflation and a labour market that has cooled without cracking.',
    'The decision was widely expected. What surprised analysts was the tone of the accompanying statement, which dropped a long-standing reference to further tightening and replaced it with language about "sustaining the current stance for as long as necessary".',
    'Two members dissented, arguing that services inflation remains too far above target to justify any softening in signal. Their objection was recorded in the minutes but did not change the vote.',
    'Markets moved little. Bond yields slipped a few basis points before recovering, and currency traders treated the statement as confirmation of a plateau rather than the start of a pivot.',
  ],
  summary: [
    'Rates unchanged for a fourth meeting; core inflation is declining steadily.',
    'The statement dropped its tightening bias in favour of holding the current stance.',
    'Two members dissented over persistent services inflation.',
    'Market reaction was muted — read as a plateau, not a pivot.',
  ],
};
