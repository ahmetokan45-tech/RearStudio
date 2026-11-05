Mi Back Screen — https://github.com/totallynotkasai/mi-back-screen
Revision: 842d0ca875a16b568d931785e80fd47f1cd10208
Copyright (c) 2026 Andrew Worgan. MIT; full text in LICENSE.
RearDisplayPick.kt and ShizukuState.kt are copied with package changes.
The Gradle wrapper is also taken from that repository (Gradle, Apache-2.0).
Shizuku API/provider 13.1.5: https://github.com/RikkaApps/Shizuku-API (MIT).

Optional F1 schedule requests: https://api.jolpi.ca/ergast/f1/current/next.json
Documentation: https://github.com/jolpica/jolpica-f1/tree/main/docs
User-Agent AHM3T45Studio/0.6; manual refresh only, at least 30 seconds between requests.
No Jolpica source code is bundled. No sports website is scraped or embedded.
The launcher A mark is an original vector; no club or Formula 1 logos are used.

0.7 result cards: TheSportsDB v1 API (public development key 123), https://www.thesportsdb.com/docs/api
Terms: https://www.thesportsdb.com/docs_terms_of_use.php
Personal development build only; app-store distribution requires the provider's paid subscription. No artwork is fetched. The free previous-team endpoint is limited and documents home events only; it is not a live feed or guaranteed latest fixture across all competitions.
F1 results: https://api.jolpi.ca/ergast/f1/current/last/results.json (Jolpica-F1).
Historical 0.7 implementation (removed in 0.8): RearBridge.kt adapts the fixed am-start and reflective Shizuku newProcess pattern from Mi Back Screen RearCommands.kt, MIT, same pinned revision above. No other Xiaomi commands or persistent settings changes are included.

0.8 imports the complete Mi Back Screen Android implementation and 37 upstream test files from revision 842d0ca875a16b568d931785e80fd47f1cd10208, copyright (c) 2026 Andrew Worgan, MIT (LICENSE). Package renamed from com.backscreen.wallpaper to de.rearstudio; upstream MainActivity renamed DisplayActivity. Resources and manifest are adapted for the Studio launcher, icon, internet-enabled sport cards and StudioRearLayer. Upstream app title and privacy text are updated; its original credit remains here, in the packaged licence and About text. The old Studio test-only bridge is removed.
Dependencies additionally include AndroidX AppCompat (Apache-2.0), Material Components Android (Apache-2.0), AndroidX Palette (Apache-2.0), and test-only Robolectric (MIT).

0.9 adds the Studio scene model, shared live renderer/editor, charging profiles, unified media-session access, per-app notification selection, PendingIntent opening and DE/EN/TR menus. Legacy German/Turkish resources were machine-assisted translations of public MIT menu strings, followed by selected corrections; user data was not translated. The inherited engine and its MIT attribution remain. Sports request user-agent is AHM3T45Studio/0.9.
