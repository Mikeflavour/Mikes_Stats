# Mikes_Stats

Fabric-Mod: Statistiken aller Spieler. `/stats` = Truhen-Menü für alle, Escape-Knopf „Statistiken alle" = eigenes Menü (nur mit Client-Mod).

- Spec: `docs/superpowers/specs/2026-10-23-mikes-stats-design.md` (von Mike abgenommen; [Mike]-Punkte nur nach Rückfrage ändern)
- Plan: `docs/superpowers/plans/2026-10-23-mikes-stats.md`
- Beispielbilder: https://claude.ai/artifact/6iyrKxf7Wp3315vxDnWgFw

## Bauen
- Minecraft 26.2 (ohne Mappings), Java 25, Versionen in `gradle.properties` (wie Mikes_Upper).
- Cloud-Sessions erreichen maven.fabricmc.net und Mojang nicht: **gebaut wird nur auf GitHub Actions** (`build.yml` bei jedem Push). Jar = Artifact `Mikes_Stats-jar`.
- Reine Logik in `mikesstats.core` (kein Minecraft): lokal testbar mit `scripts/test-core.sh` (Java 21 reicht, keine Java-22+-Features in `core`).
- Minecraft-Quellcode zum Nachschlagen: Workflow `mc-sources.yml` manuell starten, Artifact `mc-sources` laden.

## Liefern
- Jar aus dem Artifact als Zip nach `/mnt/project-files/mikes-stats/` (Mike ist oft am Handy, .jar direkt lässt sich dort nicht laden).
- Server ist Exaroton: Dateien nur hoch-/runterladen, kein Umbenennen.

## Regeln
- Mike schreibt Deutsch; Texte im Spiel deutsch über `assets/mikes_stats/lang/de_de.json`.
- Keine Info doppelt in zwei Reitern. Keine neuen Blöcke/Items (Vanilla-Clients müssen beitreten können).
- Vanilla-Dateien (`world/stats`, `playerdata`, `usercache.json`) nur lesen, nie schreiben.
