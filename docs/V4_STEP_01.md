# V4 Schritt 01: freie Widgetkoordinaten

## Implementiert

- Scene v1 bleibt lesbar; negative Positionen, Positionen über 100% und größere Widgets überleben Parser, Deep Copy, Save/Load und Undo.
- Drag und Resize klemmen nicht mehr an Displayrändern; der bisherige Zwangssnap bei x=34% entfällt.
- X/Y/Breite/Höhe sind exakte Prozentfelder. Dezimalkomma und Dezimalpunkt werden akzeptiert. Ungültige/nichtendliche Zahlen werden abgewiesen. Größen müssen positiv sein (numerische Mindestgröße 0,1%); keine Displayobergrenze.
- Widget-Auswahl pro Profil erlaubt die Bearbeitung vollständig außerhalb liegender Elemente.
- Texte in Deutsch, Englisch und Türkisch.
- Bestehende Host-Anbindung, Sperrverhalten, Ladepfade und Bildassets bleiben unverändert.

## Begrenzter Umfang

Noch kein vollständiger Pro-Editor: Rotation, Ebenensortierung, Redo, Seiten, Kalibrierung und frei transformierbare Edge-Animation folgen separat. Der Canvas zeigt weiterhin nur den sichtbaren Ausschnitt; ein zoombarer Arbeitsbereich folgt später. Außerhalb liegende Widgets lassen sich über Auswahl und Zahlenfelder zurückholen. Vorhandene geschätzte Kameraguides sind nicht als exakte Kalibrierung bestätigt.

## Prüfung

- Neue Tests: Offscreen-/Übergrößen-Roundtrip, Drag über Displayrand mit Save/Load, Resize jenseits des rechten Randes, ungültige Werte.
- Bestehende Basistests bleiben erhalten; der alte Test für erzwungene Displaygrenzen wurde an die neue Anforderung angepasst.
- Lokal: Gradle-Download durch Netzwerk blockiert; keine lokal erfolgreichen Android-Tests behauptet.
- GitHub Actions nach ausdrücklicher Zustimmung erfolgreich: testDebugUnitTest, lintDebug und assembleDebug. Run: https://github.com/ahmetokan45-tech/RearStudio/actions/runs/42177504711, geprüfter Code-Commit: 9e91f19af765af5e44816803f8d8fd62a3ea97e5. Build dauerte 2m 15s; bestehende Deprecation-Warnungen vorhanden. APK-Artefakt erstellt und ZIP-SHA256 geprüft.
- APK ist weiterhin versionCode 9 / 0.9-preview (erster V4-Entwicklungsschritt, keine vollständige V4). CI-Debugsignatur kann von der bereits installierten lokalen Version abweichen; bei Signaturkonflikt nicht ohne Datensicherung deinstallieren.
- Statische Prüfung: git diff --check bestanden; alle drei geänderten XML-Ressourcendateien geparst, keine doppelten Schlüsselnamen, neue Übersetzungsschlüssel vorhanden. Dies ersetzt keine Kotlin-Kompilierung oder Ausführung der Tests.
- Xiaomi-Gerätetest: offen. Prüfen: bestehendes Layout öffnen, negative X/Y speichern/neustarten, Widget >100% vergrößern, außerhalb liegendes Widget wieder auswählen, Undo/Cancel, Live-Vorschau beenden, normale Lade-/Musik-/Notification-Funktionen vergleichen.
