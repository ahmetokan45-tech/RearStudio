# V5.6: Gerätetest auf Xiaomi 17 Pro Max

Status: **App-Gerätetests nicht durchgeführt**. Die vom Nutzer bereitgestellte ADB-Ausgabe bestätigt lediglich die USB-Verbindung zum Xiaomi (`device`, Modell 2509FPN0BC). Diese Checkliste gehört zur isolierten Test-App,
nicht zu einer Verkaufsfreigabe oder einer vollständigen V5.6-Abnahme.

## Vorbereitung

1. Erst eine APK verwenden, deren zugehöriger GitHub-Lauf Build, Unit-Tests, Lint
   und die direkte APK-Prüfung bestanden hat.
2. Vorhandenes Rear Studio nicht deinstallieren. Die Test-App heißt
   **AHM3T45 V5.6 Test** und hat die Paketkennung `de.rearstudio.v56dev`.
   Sie übernimmt keine privaten Daten der alten App automatisch.
3. Die Rückdisplay-Ausgabe der alten App vor dem Aktivieren der Test-App beenden.
   Beide Apps dürfen nicht gleichzeitig um die Xiaomi-Rückdisplay-Steuerung kämpfen.
4. Xiaomi per USB mit dem MacBook verbinden, USB-Debugging erlauben und den
   Rechner-Fingerabdruck auf dem Telefon bestätigen. `adb devices` muss das Gerät
   als `device` anzeigen, nicht als `unauthorized`.
5. Test-APK installieren; die bisherige App und ihre Designs bleiben erhalten.
   Shizuku- und Benachrichtigungszugriff für die neue Test-App getrennt freigeben.

## Grundfunktionen und Datenverlustschutz

- Startseite und Editor öffnen; keine Abstürze, richtige Displayausrichtung.
- Design speichern, App beenden und erneut öffnen: Hintergrund und Widgets bleiben.
- Entwurf ändern, Prozess beenden: Wiederherstellung anbieten. Verwerfen darf das
  aktive gespeicherte Design nicht verändern.
- Verschieben, Größe, Drehung, Ebenen, Sperre, Duplizieren, Undo und Redo prüfen.
- Gesperrte Ebene darf nicht verschoben oder über den Löschgriff entfernt werden.
- Zweite Uhr wird beim Hinzufügen abgewiesen.
- Designbibliothek: speichern, umbenennen, Favorit, duplizieren, löschen.
- Designvorschau abbrechen: keine Änderung. Nur Hintergrund übernehmen: Positionen
  und Widget-Einstellungen müssen identisch bleiben. Vorheriges Design wiederherstellen.

## Bilder und Farben

- PNG/JPEG/GIF einmal importieren, Galerie-Zugriff beenden, App neu starten:
  Das Original bleibt intern verfügbar.
- Bibliotheksvorschau, Favorit, Name und zuletzt verwendet prüfen.
- Verschieben/Zoom, Helligkeit, Kontrast, Sättigung testen; Zurücksetzen stellt
  unverändertes Original dar.
- Pipette auf bekannten Farbflächen: ARGB/RGB gegen gerenderten Pixel vergleichen.
  Die Pipette erfasst die zusammengesetzte Darstellung, nicht die Quellfarbe.

## Kamerakalibrierung und Licht

- Originalreferenz `93534.png` unverändert vergleichen. Die Ovalkontur ist keine
  separate Vermessung beider Linsenkreise.
- Tatsächliche Displaygröße, Orientierung, vier Eckradien sowie X/Y/Radius beider
  Linsen bestimmen. Keine Schätzwerte als verifiziert übernehmen.
- Kritische Inhalte und Bedienelemente dürfen weder Linsen noch Displayrand schneiden.
- Ohne zwei hinterlegte Kreise muss die Kameralicht-Auswahl gesperrt bleiben.
- Mit Kalibrierung: statisch, Puls, Blinken, Welle, Umlauf; Farben, Helligkeit,
  Richtung, Zeit, Wiederholungen sowie Pause/Neustart prüfen.
- Display aus, Energiesparen und reduzierte Animationen: keine fortlaufenden
  Lichtanimationen. Nach Aktivierung muss ein konsistenter Zustand erscheinen.

## Smart Views und bestehende Funktionen

- Erst Vorschau in der App prüfen, dann experimentelle Rückdisplay-Gesten aktivieren.
- Unten→oben Aktivität; oben→unten Benachrichtigungen; rechts→links Wetter;
  links→rechts Musik. Wetter/Aktivität melden derzeit ausdrücklich fehlende Datenquellen.
- X, konfigurierbare Schließgeste und Inaktivität prüfen; Berührung setzt Timer zurück.
  Timer 0 bedeutet aus. Nach Schließen muss dasselbe Standarddesign erscheinen.
- Kein gleichzeitiges Öffnen der bisherigen Kamera oder Benachrichtigungs-Schublade.
- Näherungssensor mit Hand/Tisch verdecken: keine unbeabsichtigten Gesten.
- Spotify/YouTube Music: Start, Pause, Titelwechsel, Cover, drei Tasten, Zugriff entziehen,
  keine Doppelbanner. Keine universelle Cover-Unterstützung voraussetzen.
- Echte Nachrichten: Vorschau auf gesperrtem Gerät, ausgeschlossene Apps, mehrere
  Nachrichten, Entfernen, konkurrierende Musik- und Ladeereignisse prüfen.
- Laden beginnen/beenden; bisherige Edge-/Akkuansichten regressionsprüfen.
- Shizuku beenden, Berechtigung verweigern, Display ausschalten, App stoppen,
  Telefon neu starten: Wiederherstellung und Rückgabe an Xiaomi prüfen.

## Messwerte und Freigabe

Für jeden Punkt Ergebnis (bestanden/fehlgeschlagen/nicht unterstützt), ROM-Build,
Android-Version, Zeitpunkt und bei Fehlern reproduzierbare Schritte notieren.
Akku- und Wärmeverhalten mindestens im Standby sowie mit laufender Lichtanimation
vergleichen. Keine Zuverlässigkeits- oder Verkaufsfreigabe ohne diese Messungen.

Offene Softwarefunktionen stehen in `V56_STATUS.md`; erfolgreiche Gerätetests dieses
Zwischenstands schließen diese Entwicklungslücken nicht.

## Neue Prüfungen für 0.12

- Compose-Kacheln öffnen jeweils Editor, Designs, Elemente und Einstellungen.
- Schriftfamilien in allen Widget-Typen, Uhrfarben und optionalen Sekunden prüfen.
- Schrittberechtigung erlauben/ablehnen/widerrufen; echte Schritte, Neustart und
  Datumswechsel prüfen. Angezeigt werden erfasste Schritte ab Trackingstart.
- Wetterstandort setzen, ändern und deaktivieren; offline bleiben Zeitstempel und
  gespeicherte Daten erkennbar. Koordinatenfreigabe erfolgt ausdrücklich im Dialog.
- Wechsel-Widget: Anzeigedauern, manuelles Wischen und Konkurrenz zur Smart View.
- Design als ZIP mit Bildern exportieren, importieren, Kalibrierung vergleichen;
  beschädigte Dateien ablehnen. Aktives Design erst durch Speichern ändern.
- Zehn Hintergrunddateien offline anzeigen; verwendete Bilder dürfen nicht löschbar sein.
- Gruppen gemeinsam verschieben; gesperrte Widgets unverändert; Zweifinger-Skalierung
  und Drehung sowie Undo/Redo prüfen.
- App-spezifische Benachrichtigungsstile mit echten Nachrichten vergleichen.
- Transparenz darf Musikcover nicht vollständig verdecken.
- Lange drücken: Favoritenvorschau, Abbrechen, Bestätigen und Hintergrundmodus;
  während Ausschalten darf der Wechselbildschirm nicht aktiv bleiben.
- Licht-A/B-Vergleich separat pausieren/starten/zurücksetzen; Ein-/Ausblenden,
  Helligkeit/Glow/Linienbreite und Kameraaussparungen kontrollieren.
