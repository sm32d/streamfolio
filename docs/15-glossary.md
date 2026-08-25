# 15 — Glossary & Terminology

> **Last verified commit:** `aaac96970daf81cd5d75cf5fa56dfebf2ecb6b48`  
> **Analysis Date:** 2026-08-25

---

## 1. Android & Mobile Development Terms

| Term | Definition |
| :--- | :--- |
| **AAB (Android App Bundle)** | The publishing format for Google Play that contains compiled code and resources, allowing Google Play to generate optimized APKs tailored to individual device configurations. |
| **AGP (Android Gradle Plugin)** | The official build system plugin for Android applications developed with Gradle. |
| **AICore** | Google's system service that manages on-device AI foundation models (such as Gemini Nano) and provides hardware-accelerated local execution. |
| **AppWidget** | Miniature application views that can be embedded into the Android home screen launcher. |
| **Baseline Profiles** | Specifications bundled in the APK/AAB that allow Android runtimes (ART) to pre-compile critical code paths ahead-of-time (AOT) to reduce app startup latency. |
| **Foreground Service** | An Android background execution component that performs operations noticeable to the user and requires an active system notification (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`). |
| **Jetpack Compose** | Android's modern, declarative UI toolkit for building native interfaces with Kotlin. |
| **MediaSession (`MediaSessionCompat`)** | An Android framework component that allows apps to interact with system media controls, lockscreens, Bluetooth remotes, and Android Auto. |
| **RemoteViews** | A lightweight view hierarchy that can be displayed in another process (used for AppWidgets and custom notifications). |
| **Room Persistence Library** | An Android Jetpack object-relational mapping (ORM) library that provides an abstraction layer over SQLite. |
| **WorkManager** | The recommended Android library for persistent, deferrable, and guaranteed background work. |

---

## 2. Web, RSS & Security Terms

| Term | Definition |
| :--- | :--- |
| **Atom 1.0** | An XML-based syndication format specification (`<feed><entry>`) providing standardized metadata for news articles and web content. |
| **DNS Rebinding** | An attack where an external domain name resolves to a legitimate IP initially, and then resolves to an internal private IP (e.g. `127.0.0.1` or `169.254.169.254`) on subsequent requests. |
| **Jsoup** | A Java/Kotlin HTML parser library used to parse, clean, extract, and sanitize HTML documents. |
| **OPML (Outline Processor Markup Language)** | An XML format for exchanging lists of RSS and Atom web feeds between different feed readers. |
| **RSS 2.0 (Really Simple Syndication)** | An XML-based news feed format utilizing `<rss><channel><item>` hierarchies. |
| **SSRF (Server-Side / Client-Side Request Forgery)** | A vulnerability where an application is coerced into sending unauthorized HTTP requests to internal, private, or loopback network addresses. |
| **XXE (XML External Entity)** | An attack against applications that parse XML input, where external entity declarations are used to read local files or trigger internal network probes. |

---

## 3. Artificial Intelligence & NLP Terms

| Term | Definition |
| :--- | :--- |
| **Gemini Nano** | Google's most efficient on-device foundation model designed for edge AI inference (summarization, proofreading) without internet connectivity. |
| **ML Kit GenAI Summarization** | Google ML Kit SDK allowing Android apps to run on-device article summarization powered by Gemini Nano via AICore. |
| **ML Kit Translate** | On-device machine translation SDK capable of translating text between 50+ languages offline using downloaded language packs. |
| **TF-IDF (Term Frequency-Inverse Document Frequency)** | A numerical statistic used in text clustering to measure the importance of words in a document relative to a corpus (used in StreamFolio for story grouping). |
