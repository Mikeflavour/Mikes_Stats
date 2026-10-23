# Mikes_Stats – Design

Stand: 23.10.2026 · Fabric · Minecraft 26.2 · Server: Exaroton

Beispielbilder (klickbar, mit Play): https://claude.ai/artifact/6iyrKxf7Wp3315vxDnWgFw
Bild 1–2 = `/stats` als Truhen-Menü, Bild 3 = Escape-Menü, Bild 4 = Client-Menü „Statistiken alle".

Kennzeichnung: **[Mike]** = so von Mike gewünscht. **[Claude]** = Entscheidung von Claude, Mike kann sie hier ändern.

---

## 1. Worum es geht

Ein Mod, der die Statistiken **aller Spieler, die jemals auf dem Server waren**, zeigt, nicht nur die eigenen wie in Vanilla. Auch Spieler, die gerade offline sind. [Mike]

Dazu ein paar Werte, die Vanilla nicht speichert: höchstes Level, XP gesamt, Level beim Tod. Die zählt der Mod erst ab dem Tag, an dem er installiert wird. Das ist so akzeptiert. [Mike]

### Ziele
- Alle Spieler auf einen Blick vergleichen, als Rangliste. [Mike]
- Alle Vanilla-Statistiken, seit Tag 1 vom Server (rückwirkend). [Mike]
- Schöner als das Standard-Menü, aber dezent, mit kleinen Piktogrammen. [Mike]
- Funktioniert auch für Spieler **ohne** Client-Mod (über `/stats`). [Mike]

### Nicht-Ziele
- Keine Werte von vor der Installation erfinden (höchstes Level, XP gesamt, einzelne Tode). [Mike]
- Keine Detailseite pro Spieler im Client-Menü. [Mike]
- Keine neuen Blöcke oder Items. Vanilla-Clients sollen ohne Mod beitreten können. [Claude]
- Keine Bearbeitung von Werten im Spiel. [Claude]

### Anti-Ziele (wäre ein Fehlschlag, auch wenn es technisch läuft)
- Sieht nach „AI-Slop" oder 0815-Menü aus. [Mike]
- Dieselbe Info steht in zwei Reitern doppelt. [Mike]
- Spieler müssen den Mod installieren, um überhaupt etwas zu sehen. [Mike]

---

## 2. Bauweise

Überlegt wurden drei Wege:

| Weg | Vorteil | Nachteil |
|---|---|---|
| A: nur Server-Mod | Nur Exaroton bekommt den Mod | Kein Knopf im Escape-Menü, nur Truhen-Look |
| B: Server + Client Pflicht | Schönes eigenes Menü | Ohne Mod sieht ein Spieler nichts |
| **C: Server-Mod, Client-Teil optional** | Läuft für alle, mit Mod schöner | Zwei Oberflächen zu bauen |

**Gewählt: C.** [Mike]

- **Eine einzige .jar** für Server und Client (wie Mikes_Upper mit `splitEnvironmentSourceSets`). Auf dem Server Pflicht, beim Spieler freiwillig. [Claude]
- `/stats` öffnet **immer** das Truhen-Menü (Bild 1–2), auch wenn man den Client-Mod hat. [Mike]
- Mit Client-Mod: Knopf „Statistiken alle" im Escape-Menü öffnet das eigene Menü (Bild 4). Nur darüber gibt es das schöne Menü. [Mike]
- Alle Daten kommen vom Server. Der Client zeigt nur an. [Claude]

---

## 3. Daten: woher, wohin

### Was Vanilla schon speichert (rückwirkend seit 27.04.2026)
| Datei auf dem Server | Inhalt |
|---|---|
| `world/stats/<UUID>.json` | Alle Statistiken: Tode, Mobs, Blöcke, Items, Wege, Spielzeit, Allgemein |
| `world/playerdata/<UUID>.dat` | Aktuelles Level, XP |
| `usercache.json` | Spielername zur UUID |

### Was der Mod zusätzlich speichert (ab Installation)
`world/mikes_stats/<UUID>.json`, eine Datei pro Spieler: [Claude]
- höchstes Level
- XP gesamt (jeder XP-Gewinn wird addiert, Verluste nicht)
- Liste der Tode: Datum, Level beim Tod, Ursache
- zuletzt online (Datum)
- ausgeblendete Spieler (für `/stats`, siehe 6.)

`config/mikes_stats.json`: [Claude]
- `startDatum`: `2026-04-27` [Mike]

### Sonderfälle
- **Zuletzt online vor der Installation:** Datum der Datei `playerdata/<UUID>.dat`. Ungefähr richtig. [Claude]
- **Name eines Offline-Spielers:** aus `usercache.json`. Fehlt er dort: UUID gekürzt anzeigen. [Claude]
- **Level eines Offline-Spielers:** aus `playerdata/<UUID>.dat`. [Claude]
- **Locator-Bar-Farbe:** Die Farbe des Markers über der Hungerleiste. Standard wird aus der UUID berechnet, außer sie wurde mit `/waypoint` geändert. Genaue Formel und Speicherort beim Bauen im Minecraft-26.2-Code nachsehen. [Claude]
- **Ingame-Tag:** Gesamtzeit der Overworld (`gameTime`) ÷ 24000, nicht die Tageszeit (die ändert `/time set`). [Claude]

### Wer kann was ändern
- Spieler sehen die Dateien nicht. Nur wer bei Exaroton Dateizugriff hat. [Mike gefragt, so erklärt]
- Ein manipulierter Client ändert nur seine eigene Anzeige. [Claude]

---

## 4. Client-Menü (Bild 3 und 4)

### Escape-Menü (Bild 3)
- Neuer Knopf **„Statistiken alle"** direkt rechts neben „Statistiken". [Mike]
- Wenn rechts kein Platz ist (kleines Fenster): Knopf unter „Statistiken". [Claude]

### Aussehen (Bild 4)
- Dunkle Farben, Hintergrund 50 % durchsichtig, das Spiel bleibt dahinter sichtbar. [Mike]
- Minecraft-Schrift, kleine Piktogramme, echte Skin-Köpfe der Spieler. [Mike]
- Kopfzeile: „Statistiken alle", „Seit 27.04.2026 · X Tage", „Ingame-Tag X". [Mike]
- Rechts oben Knopf **„Player X/Y"** zum Ein- und Ausblenden (siehe 6.). [Mike]
- Lange Listen scrollen. [Claude]

### Sortieren (gilt in **jedem** Reiter, für **jede** Spalte) [Mike]
- Klick auf einen Spaltenkopf sortiert danach.
- Erster Klick: Text A–Z, Zahlen höchste zuerst. Zweiter Klick: umgekehrt.
- ▲ / ▼ steht nur an der Spalte, nach der gerade sortiert wird, hervorgehoben.
- Kein extra Sortieren-Knopf.

### Reiter (in dieser Reihenfolge) [Mike]

**Übersicht** – eine Zeile pro Spieler, Start: A–Z
| Spalte | Quelle |
|---|---|
| Farbiges Kästchen = Locator-Bar-Farbe, Maus drauf zeigt `#RRGGBB` | Vanilla |
| Kopf | Skin |
| Name, rechts daneben grün „online" oder grau „zuletzt online am: TT.MM.JJJJ" | Server |
| Level | Vanilla |
| Höchstes (seit Mod) | Mod |
| Tode | Vanilla |
| Spielzeit (Stunden) | Vanilla |
| XP gesamt (seit Mod) | Mod |

Unten links nur hier: „Farbiges Kästchen = Locator-Bar-Farbe". [Mike]

**Allgemein, Mobs, Blöcke, Items, Wege** – gleicher Aufbau: [Claude, von Mike abgenickt]
- Eine Zeile pro Ding (Mob, Block, Wert), mit kleinem Icon.
- Eine Spalte pro Spieler, Kopf oben. Ganz rechts „Gesamt".
- Höchster Wert pro Zeile weiß hervorgehoben, 0 als „–".
- Start-Sortierung: Gesamt, höchste zuerst.
- Zeilen, bei denen alle angezeigten Spieler 0 haben, fallen weg. [Claude]

| Reiter | Umschalter darüber | Inhalt |
|---|---|---|
| Allgemein | – | Alle Vanilla-„Allgemein"-Werte, die sonst nirgends stehen: Schaden ausgeteilt/erhalten, gehandelt, Tiere gezüchtet, Fische, geschlafen, Zeit seit letztem Tod, Truhen, Werkbank, Ofen, Kuchen, Glocke, Raids usw. [Mike] |
| Mobs | Getötet / Getötet von | Vanilla `killed`, `killed_by` |
| Blöcke | Abgebaut / Platziert | `mined`; „Platziert" = `used` bei Block-Items (Vanilla hat keinen eigenen Platziert-Wert) [Claude] |
| Items | Hergestellt / Benutzt / Zerbrochen / Aufgesammelt / Fallengelassen | wie Vanilla [Claude: Bild zeigt nur 3, Vanilla hat 5] |
| Wege | – | alle Strecken in km (gelaufen, gesprintet, geschwommen, Elytra, Boot, Pferd, gefallen, …) plus Sprünge und Schleichzeit [Mike] |

Bei „Zeit seit letztem Tod" kein Gesamt-Wert. [Claude]

Nicht in Allgemein, weil schon woanders: Spielzeit, Tode, alle Strecken, Sprünge, Schleichzeit. [Mike]

**Tode** – Liste aller Tode seit Installation [Mike]
- Spalten: Datum, Spieler (Kopf + Name), Level beim Tod, Ursache.
- Start-Sortierung: neueste zuerst.
- Tode vor der Installation gibt es nur als Anzahl (Übersicht).

---

## 5. `/stats` als Truhen-Menü, für alle (Bild 1 und 2)

- Jeder Spieler darf `/stats` nutzen. [Claude]
- Großes Truhen-Menü (6 Reihen). Klicks werden abgefangen, nichts kann herausgenommen werden. [Claude]

**Hauptseite (Bild 1)** [Mike]
- Spielerköpfe, **A–Z von links nach rechts**.
- Maus über Kopf: Name, online/zuletzt online, Level, Höchstes, Tode, Spielzeit, XP gesamt.
- Buch oben links: Server-Infos (Startdatum, Tage, Ingame-Tag).
- Namensschild oben rechts: „Player" (ein/ausblenden).
- Unten: Pfeile = blättern, Trichter = Sortierung wechseln (A–Z → Level → Tode → Spielzeit), Barriere = schließen.

**Detailseite pro Spieler (Bild 2)** – nur hier, nicht im Client [Mike]
- Klick auf einen Kopf öffnet sie.
- Ein Item pro Reiter (Allgemein, Mobs, Blöcke, Items, Wege, Tode). Maus drüber zeigt die Top 10 des Spielers. [Claude]
- Tode: letzte Tode mit Level beim Tod. [Mike]
- Pfeil zurück, Barriere schließen.

---

## 6. Spieler ein- und ausblenden [Mike]

- Jeder Spieler entscheidet **für sich**, wen er sieht.
- Client-Menü: Knopf „Player" öffnet eine Liste aller Spieler mit Haken. Gespeichert auf dem PC des Spielers (`config/mikes_stats_client.json`). [Claude]
- `/stats`: Namensschild öffnet eine Truhe mit allen Köpfen, Klick schaltet um. Gespeichert auf dem Server in der Datei des Spielers. [Claude]
- Ausgeblendete Spieler fehlen in allen Reitern, auch in „Gesamt". [Claude]

---

## 7. Technik (für den Bau)

- Fabric, Minecraft 26.2 (ohne Mappings), Java 25, Fabric API. Versionen wie in Mikes_Upper (`gradle.properties`). [Claude]
- Daten zum Client: Fabric-Networking. Client fragt beim Öffnen, Server schickt ein Paket mit allen Daten. [Claude]
  - shortcut: ein Paket für alles. Wenn es bei vielen Spielern zu groß wird (Fabric-Grenze ca. 1 MB), aufteilen.
- Offline-Spieler: Dateien werden beim Öffnen gelesen, nicht dauernd. [Claude]
- XP gesamt, höchstes Level, Tode, zuletzt online: über Fabric-Events, sonst kleine Mixins. [Claude]
- Skins von Offline-Spielern: Client lädt sie über die UUID wie bei Spielerköpfen. [Claude]
- Sprache: Deutsch, über Sprachdateien (`de_de.json`, `en_us.json`), damit Englisch leicht nachrüstbar ist. [Claude]
- Tests: Logik ohne Minecraft (Sortieren, Summen, Datei lesen, Farbe aus UUID, Tage zählen) mit JUnit. [Claude]
- Lieferung: `Mikes_Stats-<version>.jar` als Zip im Projektordner. Server braucht Fabric API. [Claude]

---

## 8. Offen für den Bau (kann der Bauende selbst klären)

- Genaue Pixel-Abstände und Farben: an Bild 4 orientieren.
- Welche Icons für Allgemein- und Wege-Zeilen: passende Vanilla-Items.
- Formel der Locator-Bar-Farbe im 26.2-Code nachsehen.
- Was passiert bei sehr vielen Spielern (> 20 Spalten): seitlich scrollen.
