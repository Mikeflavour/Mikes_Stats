# Mikes_Stats Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ein Fabric-Mod, der die Statistiken aller Spieler zeigt: als Truhen-Menü über `/stats` für alle und als eigenes Menü über den Escape-Knopf für Spieler mit Client-Mod.

**Architecture:** Eine .jar mit `splitEnvironmentSourceSets`. Reine Logik (Dateien lesen, Tabellen bauen, sortieren, formatieren, Paket kodieren) liegt in `mikesstats.core` ohne Minecraft-Abhängigkeit und ist mit JUnit getestet. Der Server liest Vanilla-Dateien plus eigene Dateien, baut einen `StatsSnapshot` und schickt ihn gzip-JSON-kodiert an Clients; `/stats` zeigt denselben Snapshot als Truhen-Menü.

**Tech Stack:** Java 25, Fabric Loader 0.19.3, Fabric API 0.153.0+26.2, Loom 1.17-SNAPSHOT, Gradle 9.5, Gson (in Minecraft enthalten), JUnit 5, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-10-23-mikes-stats-design.md`

## Global Constraints

- Minecraft 26.2 ohne Mappings, Java 25, Versionen exakt wie `Mikeflavour/Mikes_Upper/gradle.properties`.
- Mod-ID `mikes_stats`, Paket `mikesstats`, Name „Mikes Stats".
- Gebaut wird nur auf GitHub Actions (lokal sind Fabric-/Mojang-Server gesperrt). Jede Task endet mit grünem CI-Lauf.
- Keine neuen Blöcke, Items oder Registry-Einträge: Vanilla-Clients müssen beitreten können.
- `/stats` öffnet **immer** das Truhen-Menü. Das eigene Menü nur über den Escape-Knopf.
- Startdatum `2026-04-27` als Standard in `config/mikes_stats.json`.
- Mod-Daten in `world/mikes_stats/<UUID>.json`; Vanilla-Dateien werden nur gelesen, nie geschrieben.
- Texte deutsch über `assets/mikes_stats/lang/de_de.json` (+ `en_us.json` mit gleichen Keys).
- Sortierung überall: Text A–Z, Zahlen höchste zuerst; zweiter Klick dreht; Pfeil ▲/▼ nur an der aktiven Spalte.
- Keine Info doppelt in zwei Reitern.

## Review Focus

1. Spieler ohne `stats`-Datei oder ohne Eintrag in `usercache.json` → Zeile mit gekürzter UUID, kein Absturz. (Test in Task 3 und 6)
2. Kaputte oder halb geschriebene `world/mikes_stats/<UUID>.json` → wird als `.bak` gesichert, Spieler startet leer, andere Spieler unberührt. (Test in Task 4)
3. Viele Spieler / große Statistiken → Paket muss unter 1 MB bleiben. (Test in Task 2: 30 Spieler × 2000 Werte)
4. Alle Spieler ausgeblendet → leere Tabelle mit Hinweis, kein Absturz; ausgeblendete fehlen auch in „Gesamt". (Test in Task 5)
5. Kleines Fenster / hohe GUI-Skalierung → Escape-Knopf hat rechts keinen Platz → unter „Statistiken". (Manuell in Task 10, Logik-Test für die Platzwahl)

## Dateien

```
.github/workflows/build.yml          CI: build + test, Jar als Artifact
.github/workflows/mc-sources.yml     manuell: Minecraft-Quellcode als Artifact
CLAUDE.md                            Regeln für spätere Threads
build.gradle, settings.gradle, gradle.properties, gradlew*, gradle/wrapper/*
src/main/resources/fabric.mod.json
src/main/resources/assets/mikes_stats/lang/{de_de,en_us}.json
src/main/java/mikesstats/
  core/        (ohne Minecraft)
    StatsSnapshot.java   Records: Snapshot, PlayerEntry, DeathEntry
    SnapshotCodec.java   gzip-JSON <-> byte[]
    VanillaStatsReader.java
    ModData.java, ModDataStore.java
    Formats.java         Datum, km, Stunden, Tage
    Tabs.java            welche Stat-IDs in welchen Reiter
    TableModel.java      Übersicht- und Pivot-Tabellen, Sortierung
    WaypointColors.java
  MikesStatsMod.java     ModInitializer
  server/
    Tracking.java        XP, Level, Tode, online/offline -> ModDataStore
    SnapshotBuilder.java liest Dateien + online Spieler -> StatsSnapshot
    Network.java         Payloads + Server-Handler
    StatsCommand.java    /stats
    ChestMenus.java      Hauptseite, Detailseite, Player-Filter
src/client/java/mikesstats/client/
  MikesStatsClient.java, PauseButton.java, StatsScreen.java,
  PlayerFilterScreen.java, ClientConfig.java
src/test/java/mikesstats/core/*Test.java
```

---

### Task 1: Gerüst, CI und Minecraft-Quellcode

**Files:**
- Create: alle Gradle-Dateien (aus Mikes_Upper kopiert, `mod_id=mikes_stats`, `maven_group=mikesstats`, `archives_base_name=Mikes_Stats`, `mod_version=0.1.0+26.2`), `fabric.mod.json` (environment `*`, Entrypoints `main: mikesstats.MikesStatsMod`, `client: mikesstats.client.MikesStatsClient`), beide Workflows, `CLAUDE.md`, `.gitignore`, leere Initializer.
- Test: `src/test/java/mikesstats/core/SmokeTest.java`

**Interfaces:**
- Produces: grüner Workflow `build`; Artifact `Mikes_Stats-jar`; Artifact `mc-sources` (Minecraft-26.2-Quellen) zum Nachschlagen in allen späteren Tasks.

- [ ] **Step 1:** `build.gradle` um JUnit erweitern: `testImplementation "org.junit.jupiter:junit-jupiter:5.11.4"`, `testRuntimeOnly "org.junit.platform:junit-platform-launcher"`, `test { useJUnitPlatform() }`. SmokeTest: `assertEquals(2, 1 + 1)`.
- [ ] **Step 2:** `build.yml`: on push/pull_request, `actions/setup-java@v4` (temurin, 25), `gradle/actions/setup-gradle@v4`, `./gradlew build`, `actions/upload-artifact@v4` mit `build/libs/Mikes_Stats-*.jar` (ohne `-sources`), Name `Mikes_Stats-jar`.
- [ ] **Step 3:** `mc-sources.yml`: `workflow_dispatch`, gleiche Java-Einrichtung, `./gradlew genSources`, Artifact `mc-sources` = das von Loom erzeugte Minecraft-Sources-Jar (Pfad im Log suchen, `~/.gradle/caches/fabric-loom/**/minecraft-*-sources.jar`).
- [ ] **Step 4:** `CLAUDE.md`: Versionen, „bauen nur über GitHub Actions", Artifact-Download per `gh run download`, Lieferung als Zip nach `/mnt/project-files/mikes-stats/`, Regeln aus Global Constraints, Verweis auf Spec und diesen Plan.
- [ ] **Step 5:** Commit, push. Run: `gh run watch` auf den `build`-Lauf. Expected: grün, SmokeTest läuft.
- [ ] **Step 6:** `gh workflow run mc-sources.yml`, dann `gh run download -n mc-sources -D /home/claude/mc-src`. Expected: Jar liegt lokal, `unzip -l` zeigt `net/minecraft/...`. Klappt der Download nicht: Mike Bescheid geben (Plan bleibt machbar, aber jede MC-Klasse muss dann per CI-Fehlschlag geprüft werden).

---

### Task 2: Snapshot-Modell und Kodierung

**Files:**
- Create: `core/StatsSnapshot.java`, `core/SnapshotCodec.java`
- Test: `src/test/java/mikesstats/core/SnapshotCodecTest.java`

**Interfaces:**
- Produces:
  - `record StatsSnapshot(LocalDate serverStart, long daysSinceStart, long ingameDay, List<PlayerEntry> players, List<DeathEntry> deaths)`
  - `record PlayerEntry(UUID uuid, String name, boolean online, LocalDate lastSeen /*null wenn online*/, int level, int maxLevel /*-1 = unbekannt*/, long xpTotal /*-1*/, int waypointColor /*0xRRGGBB*/, Map<String,Integer> pvpVictims /*Name -> Anzahl seit Mod*/, Map<String, Map<String,Integer>> stats /*Kategorie-ID -> Stat-ID -> Wert*/)`
  - `record DeathEntry(LocalDate date, UUID player, int level, String cause /*fertiger Text, z.B. "von Ben getötet"*/)`
  - `SnapshotCodec.encode(StatsSnapshot): byte[]`, `SnapshotCodec.decode(byte[]): StatsSnapshot` (Gson + GZIP, Daten als ISO-Strings)

- [ ] **Step 1: Tests**
  - `roundTrip_keepsAllFields`: Snapshot mit 2 Spielern (einer online, einer offline mit `lastSeen`), Umlaut-Name „Jürgen", 1 Tod → `decode(encode(s))` equals `s`.
  - `bigServer_staysUnderOneMegabyte`: 30 Spieler × 2000 Stat-Werte (IDs `minecraft:block_<i>`) → `encode(s).length < 1_000_000`.
- [ ] **Step 2:** Run `./gradlew test` via CI. Expected: FAIL (Klassen fehlen).
- [ ] **Step 3:** Implementieren.
- [ ] **Step 4:** CI grün.
- [ ] **Step 5:** Commit `feat: snapshot model and codec`.

---

### Task 3: Vanilla-Statistikdateien lesen

**Files:**
- Create: `core/VanillaStatsReader.java`
- Test: `VanillaStatsReaderTest.java` + `src/test/resources/stats/sample.json` (echtes Format: `{"stats":{"minecraft:mined":{"minecraft:stone":12},"minecraft:custom":{"minecraft:deaths":3}},"DataVersion":4000}`)

**Interfaces:**
- Produces: `VanillaStatsReader.read(Path statsFile): Map<String, Map<String,Integer>>`; `VanillaStatsReader.readUserCache(Path usercacheJson): Map<UUID,String>`

- [ ] **Step 1: Tests**
  - `read_sampleFile`: `result.get("minecraft:mined").get("minecraft:stone") == 12`, `custom/deaths == 3`.
  - `read_missingFile_returnsEmpty`, `read_corruptFile_returnsEmpty` (Datei `{"stats":` ) – kein Fehler geworfen.
  - `readUserCache_mapsUuidToName` und `readUserCache_missingFile_returnsEmpty`.
- [ ] **Step 2–4:** CI rot → implementieren (Gson `JsonParser`) → CI grün.
- [ ] **Step 5:** Commit `feat: read vanilla stats and user cache`.

---

### Task 4: Eigene Spielerdaten speichern

**Files:**
- Create: `core/ModData.java`, `core/ModDataStore.java`
- Test: `ModDataStoreTest.java` (mit `@TempDir`)

**Interfaces:**
- Produces:
  - `final class ModData { int maxLevel=-1; long xpTotal=-1; LocalDate lastSeen; List<DeathRecord> deaths; Map<UUID,Integer> pvpVictims; Set<UUID> hidden; }` mit `record DeathRecord(LocalDate date, int level, String cause)`
  - `ModDataStore(Path dir)`; `ModData get(UUID)` (lädt einmal, cached); `void save(UUID)` (schreibt `.tmp`, dann atomic move); `Map<UUID,ModData> all()` (lädt alle Dateien im Ordner)
  - `ModData.addXp(int gained)` (nur positive Werte, setzt `xpTotal` von -1 auf 0 beim ersten Mal); `ModData.seeLevel(int level)` (max-Update)

- [ ] **Step 1: Tests**
  - `saveAndLoad_roundTrip`.
  - `corruptFile_isBackedUpAndStartsEmpty`: Datei mit Müll → `get` liefert leeres ModData, `<uuid>.json.bak` existiert mit altem Inhalt, Datei eines zweiten Spielers bleibt unverändert.
  - `addXp_ignoresNegative`, `seeLevel_keepsMaximum`.
- [ ] **Step 2–4:** CI rot → implementieren → CI grün.
- [ ] **Step 5:** Commit `feat: mod data store`.

---

### Task 5: Tabellen-Logik (Reiter, Sortierung, Formate)

**Files:**
- Create: `core/Tabs.java`, `core/TableModel.java`, `core/Formats.java`
- Test: `TabsTest.java`, `TableModelTest.java`, `FormatsTest.java`

**Interfaces:**
- Produces:
  - `enum Tab { OVERVIEW, GENERAL, MOBS, BLOCKS, ITEMS, WAYS, DEATHS }`
  - `Tabs.subCategories(Tab): List<String>` → MOBS: `minecraft:killed`, `minecraft:killed_by`; BLOCKS: `minecraft:mined`, `minecraft:used` (nur Blöcke); ITEMS: `minecraft:crafted`, `minecraft:used`, `minecraft:broken`, `minecraft:picked_up`, `minecraft:dropped`; GENERAL/WAYS: `minecraft:custom`
  - `Tabs.belongsTo(Tab, String customStatId): boolean` → WAYS = alle `*_one_cm` + `minecraft:jump` + `minecraft:sneak_time`; GENERAL = alle anderen `custom` außer `minecraft:play_time`, `minecraft:deaths`, `minecraft:player_kills` (stehen in der Übersicht)
  - `Tabs.hasNoTotal(String statId)` → true für `minecraft:time_since_death`
  - `record Column(String key, boolean numeric)`; `record SortState(String key, boolean flipped)` mit `SortState.click(String key)` (gleiche Spalte → flip, andere → neu, nicht flipped)
  - `TableModel.pivot(StatsSnapshot, Tab, String category, Set<UUID> hidden, Predicate<String> isBlock, SortState): PivotTable` mit `record PivotTable(List<PlayerEntry> columns, List<PivotRow> rows)`, `record PivotRow(String statId, long[] values, long total /*-1 = kein Gesamt*/)`
  - `TableModel.overview(StatsSnapshot, Set<UUID> hidden, SortState): List<PlayerEntry>` (Spaltenschlüssel: `name, level, maxLevel, deaths, pvpKills, playTime, xpTotal`)
  - `Formats.date(LocalDate)` → `"21.10.2026"`; `Formats.km(long cm)` → `"12,4 km"`; `Formats.hours(long ticks)` → `"86 h"`; `Formats.number(long)` → `"18.240"`; `Formats.daysSince(LocalDate start, LocalDate today)`; `Formats.ingameDay(long gameTime)` = `gameTime / 24000`

- [ ] **Step 1: Tests (Auszug der Pflicht-Fälle)**
  - `sort_textAscendingFirst_thenFlips`: Namen `Tim, Anna, Ben` → `Anna, Ben, Tim`; nach zweitem Klick `Tim, Ben, Anna`.
  - `sort_numbersHighestFirst`.
  - `pivot_dropsRowsWhereAllVisibleAreZero`.
  - `pivot_hiddenPlayersExcludedFromColumnsAndTotal`.
  - `pivot_allHidden_returnsEmptyTable` (keine Exception).
  - `pivot_timeSinceDeath_hasNoTotal` (`total == -1`).
  - `general_excludesOverviewAndWays`: `minecraft:play_time`, `minecraft:deaths`, `minecraft:player_kills`, `minecraft:walk_one_cm`, `minecraft:jump` nicht in GENERAL.
  - `blocks_used_onlyBlockIds`: `isBlock` = nur `minecraft:dirt` → Zeile `minecraft:bread` fehlt.
  - `overview_playerWithoutStatsFile_hasZeros`.
  - Formats: `km(1_240_000) == "12,4 km"`, `hours(86*72000) == "86 h"`, `daysSince(2026-04-27, 2026-10-23) == 179`, `ingameDay(51_360_000) == 2140`.
- [ ] **Step 2–4:** CI rot → implementieren → CI grün.
- [ ] **Step 5:** Commit `feat: table model, tabs and formats`.

---

### Task 6: Server – Daten sammeln und mitzählen

**Files:**
- Create: `MikesStatsMod.java`, `server/Tracking.java`, `server/SnapshotBuilder.java`, `core/WaypointColors.java`
- Test: `WaypointColorsTest.java`, `SnapshotBuilderNamesTest.java`

**Interfaces:**
- Consumes: Task 2–5.
- Produces: `SnapshotBuilder.build(MinecraftServer): StatsSnapshot`; `MikesStatsMod.store(): ModDataStore`; `MikesStatsMod.config(): Config` (`record Config(LocalDate startDate)`, liest/schreibt `config/mikes_stats.json`).

- [ ] **Step 1:** In `mc-sources` nachschlagen und im Commit-Text notieren: (a) wo XP-Gewinn läuft (Methode, die Erfahrungspunkte addiert), (b) wie die Locator-Bar-Standardfarbe aus der UUID berechnet wird und wo eine `/waypoint`-Farbe gespeichert ist, (c) welches Fabric-Event Tod (mit DamageSource/Killer), Join und Disconnect liefert, (d) wie man `playerdata/<uuid>.dat` liest (Level `XpLevel`).
- [ ] **Step 2: Tests**
  - `WaypointColors.defaultFor(UUID)`: zwei feste UUIDs → exakt die Werte, die die Formel aus Step 1b ergibt (im Test mit Kommentar auf die MC-Methode).
  - `SnapshotBuilder.displayName(UUID, Map<UUID,String> cache)`: fehlt im Cache → erste 8 Zeichen der UUID.
- [ ] **Step 3:** Implementieren:
  - Tracking: XP-Gewinn → `addXp` + `seeLevel`; Join → `seeLevel(aktuell)`; Disconnect → `lastSeen = heute`, `save`; Tod → `DeathRecord(heute, level, Ursache)`, bei Spieler-Killer Ursache `"von <Name> getötet"` und `killer.pvpVictims[opfer]++`. Gespeichert wird sofort nach Tod/Disconnect und beim Server-Stopp (keine Datenverluste bei Absturz außer laufender XP).
  - SnapshotBuilder: alle UUIDs aus `world/stats/*.json` ∪ ModData; online → Live-Werte; offline → `playerdata`, `lastSeen` aus ModData sonst Datei-Änderungsdatum von `playerdata/<uuid>.dat`; `ingameDay` aus Overworld-`gameTime`.
- [ ] **Step 4:** CI grün.
- [ ] **Step 5:** Commit `feat: server tracking and snapshot builder`.

---

### Task 7: Netzwerk

**Files:**
- Create: `server/Network.java`
- Modify: `MikesStatsMod.java` (registrieren)

**Interfaces:**
- Produces: C2S `RequestStatsPayload` (`mikes_stats:request`, leer); S2C `StatsPayload(byte[] data)` (`mikes_stats:stats`, Inhalt = `SnapshotCodec.encode`). Server antwortet auf Request mit `SnapshotBuilder.build`. Client-Seite nutzt `SnapshotCodec.decode`.

- [ ] **Step 1:** Payloads mit Fabric `PayloadTypeRegistry` (play C2S/S2C) und `ServerPlayNetworking.registerGlobalReceiver` – genaue 26.2-API in `mc-sources`/Fabric-API-Jar prüfen.
- [ ] **Step 2:** Rate-Limit: höchstens eine Antwort pro Spieler pro Sekunde (einfache Map UUID→letzter Zeitpunkt).
- [ ] **Step 3:** CI grün. Commit `feat: stats network payloads`.

---

### Task 8: `/stats` Truhen-Menü

**Files:**
- Create: `server/StatsCommand.java`, `server/ChestMenus.java`
- Test: `ChestLayoutTest.java` (reine Slot-Rechnung in `core`: `ChestLayout.headSlots(page, count)`, `ChestLayout.pages(count)`)

**Interfaces:**
- Consumes: `SnapshotBuilder.build`, `TableModel.overview`, `ModDataStore` (`hidden` des Betrachters).
- Produces: Befehl `/stats` (ohne Rechte-Stufe).

Layout (6 Reihen, Slots 0–53), wie Bild 1–2:
- Hauptseite: Slot 0 Buch (Server-Infos im Tooltip), Slot 8 Namensschild „Player", Slots 9–44 Köpfe (36 pro Seite, A–Z bzw. Trichter-Sortierung), Slot 45 Pfeil zurück, 48 Trichter (zyklisch: Name → Level → Tode → Spielzeit), 49 Barriere, 53 Pfeil weiter. Kopf-Tooltip: Name, online/zuletzt online, Level, Höchstes, Tode, PvP-Kills, Spielzeit, XP gesamt.
- Detailseite: Slot 4 Kopf; Slots 19–24 je ein Item für Allgemein, Mobs, Blöcke, Items, Wege, Tode mit Top 10 im Tooltip (Tode: letzte 10 mit Level); Slot 45 zurück, 49 schließen.
- Player-Filter: alle Köpfe, ausgeblendete mit grauem Farbstoff statt Kopf, Klick schaltet, speichert in ModData des Betrachters.
- Alle Klicks abfangen (kein Herausnehmen, kein Shift-Klick, kein Ziehen).

- [ ] **Step 1: Tests** `pages(0)==1`, `pages(36)==1`, `pages(37)==2`, `headSlots(1, 40)` = Slots 9..12.
- [ ] **Step 2–3:** Implementieren, Köpfe mit echtem Profil (UUID + Name) damit der Skin lädt.
- [ ] **Step 4:** CI grün. Commit `feat: /stats chest menu`.

---

### Task 9: Client-Menü

**Files:**
- Create: `client/MikesStatsClient.java`, `client/PauseButton.java`, `client/StatsScreen.java`, `client/PlayerFilterScreen.java`, `client/ClientConfig.java`
- Test: `ButtonPlacementTest.java` (`core/ButtonPlacement.rightOf(int statsX, int statsW, int screenW, int btnW): boolean`)

**Interfaces:**
- Consumes: Payloads (Task 7), `TableModel`, `Formats`, `Tabs`.
- Produces: Knopf „Statistiken alle" im Pause-Menü; `StatsScreen` wie Bild 4.

- [ ] **Step 1: Test** `rightOf` liefert false, wenn `statsX + statsW + 4 + btnW > screenW - 4` (dann Knopf unter „Statistiken").
- [ ] **Step 2:** PauseButton: Fabric `ScreenEvents.AFTER_INIT` auf dem Pause-Screen, Knopf „Statistiken" über die Übersetzung `gui.stats` finden.
- [ ] **Step 3:** StatsScreen nach Bild 4 (https://claude.ai/artifact/6iyrKxf7Wp3315vxDnWgFw): Hintergrund 50 % durchsichtig (`0x800E0F12`), Kopfzeile, Reiter in Spec-Reihenfolge, Umschalter, klickbare Spaltenköpfe mit `SortState`, Zebra-Zeilen, Scrollen, höchster Wert pro Zeile weiß, `–` für 0, Tooltips (Hex-Farbe am Kästchen, PvP-Opfer an der Zahl), Legende nur in Übersicht, Hinweis bei leerer Tabelle. Köpfe über den Skin-Manager per UUID.
- [ ] **Step 4:** PlayerFilterScreen: Liste mit Häkchen, gespeichert in `config/mikes_stats_client.json` (`Set<UUID> hidden`).
- [ ] **Step 5:** CI grün. Commit `feat: client stats screen`.

---

### Task 10: Feinschliff, Review, Lieferung

- [ ] **Step 1:** Alle Texte in `de_de.json`/`en_us.json`, Stat-Namen über Vanilla-Übersetzungen (`stat.minecraft.*`, Item-/Block-/Entity-Namen).
- [ ] **Step 2:** Review mit `agent-skills:code-reviewer` über den ganzen Branch, Funde beheben.
- [ ] **Step 3:** CI grün, Jar herunterladen, als `Mikes_Stats-0.1.0+26.2.zip` nach `/mnt/project-files/mikes-stats/` legen.
- [ ] **Step 4:** Mike testet auf Exaroton (Server) und im Client: Checkliste in der Nachricht (Escape-Knopf, `/stats`, Sortieren, Ausblenden, Tod mit Level, PvP).
