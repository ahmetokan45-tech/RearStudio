# AHM3T45 STUDIO – 0.10

Diese Datei ist Arbeitsauftrag und Abnahmeübersicht für die besprochenen Aktualisierungen. Ein vorhandener Menüpunkt ist kein Nachweis für einen erfolgreichen Hardwaretest.

## Gewünschtes Produkt

- Eine feste Displayfläche mit gemeinsamem Hintergrund. Musik, Laden und Mitteilungen ergänzen Widgets; keine erzwungenen Komplettwechsel.
- Mobiler Editor: direkt verschieben, skalieren, löschen, duplizieren, sperren, Ebenen wählen, rückgängig/wiederholen. Verdeckte Widgets bleiben über Ebenen erreichbar.
- Je Widget eigene Darstellungen, Farben, Sichtbarkeitsbedingungen und Überlagerungsregeln. Endet eine Bedingung, erscheinen darunterliegende Widgets wieder, falls ihre eigenen Bedingungen erfüllt sind.
- Musik nur während aktiver Wiedergabe; bei beendeter Sitzung kein leeres Musikfeld. Ein Player, der im Hintergrund weiterspielt, zählt weiterhin als aktiv.
- Farbpalette mit fünf individuell speicherbaren Lieblingsfarben.
- Musikvarianten: kompakt, Albumcover, minimal, Schallplatte. Separat aktivierbarer Plattenspieler, über Widget oder frei wählbare Wischgeste erreichbar. Externe Musikdienste bieten keine frei zugänglichen Audiodecks; echtes DJ-Audio benötigt lokale geeignete Audiodateien.
- Eigener Sport-Hub: Lieblingsteam, Tabelle, bis zu drei vergangene Spiele, kommendes Spiel und auswählbare Wettbewerbe. Bereiche einzeln an/aus. Ruhige Karten und nachvollziehbare Quelle/Zeit. Fehlende Ergebnisse dürfen nicht erfunden werden.
- Hintergründe: Farbe, Verlauf, Bild/GIF und Video in stummer Dauerschleife. Wiedergabe bei nicht sichtbarer Anzeige anhalten.
- Freie Gesten und App-Schaltflächen, mit wählbarem Zieldisplay. Fremde Apps und Kamera auf dem Rückdisplay benötigen Hardwareprüfung.
- Kamerabereiche und Displayrundungen müssen kalibriert werden; vorläufige Hilfslinien sind keine verifizierte Kontur.
- Zusätzliche Ladeansicht auf dem Hauptdisplay nur nach ausdrücklicher Aktivierung. Keine Umgehung von Sperrbildschirm- oder Hintergrundstartbeschränkungen.
- DE/EN/TR, verständliche Berechtigungen und abschließende Store-Prüfung (Signierung, Rechte, Datenschutz, Datenanbieter).

## Bekannter Fehler aus 0.9

„Normales Design beibehalten“ ersetzte auch die bearbeitbare Widgetliste durch die normale Liste. Dadurch waren Ladepositionen nicht mehr anwählbar. Korrektur: Hintergrund und bestehende Widgets beibehalten, Ladeelemente darüber hinzufügen und ihre eigenen Koordinaten bearbeiten. Ein Regressionstest prüft Verschieben/Skalieren und unverändertes Grundlayout.

## Datenanbieter

TheSportsDB dokumentiert für den kostenlosen Schlüssel bei Team-Vorschau/Rückblick nur ein Heimspiel. „Letzte drei“ kann daher weniger als drei echte Datensätze enthalten; Auswärtsspiele sind nicht garantiert. Tabelle und Wettbewerb werden nur aus gelieferten Daten dargestellt. Keine garantierten Live-Daten, keine Store-Freigabe des Datenanbieters behauptet.

Quelle: https://www.thesportsdb.com/docs/api

## Abnahme

Build/Test- und Installationsnachweise werden nach Ausführung ergänzt. Store-Veröffentlichung und echte Hardwarefunktion sind getrennte Abnahmen; die App wird nicht allein wegen erfolgreicher Kompilierung als fertig oder fehlerfrei bezeichnet.

## In 0.10 umgesetzt

- Gemeinsame Fläche und pro Widget Sichtbarkeit, Überlagerung, Sperre, Ebenen, Duplizieren sowie Reihenfolge. Vier konfigurierbare Wischrichtungen und App-Schaltflächen.
- Mehrere Darstellungen je Widget; aktive Wiedergabe als Musikbedingung. Verdeckte Elemente bleiben über Ebenen bearbeitbar.
- Palette mit fünf Lieblingsfarben (lange drücken speichert die gewählte Farbe); native Farbcodeeingabe.
- Stumme Video-Dauerschleife und Bild/GIF-Hintergründe. Dateien werden app-privat kopiert, bis 100 MB.
- Separat aktivierbarer Plattenspieler: externe Player normal steuern; lokale Audiodateien mit zwei Decks, Tempo, Crossfader und Positionssuche über die Platte. Lokale Wiedergabe wird beim Verlassen pausiert; kein professionelles Scratch-DSP.
- Native Sport-Hub-Karten und Konfiguration mit echter Team-Suche, Wettbewerbsauswahl, Tabelle als ausdrücklich begrenztem Anbieterauszug, bis zu drei tatsächlich gelieferten vergangenen Spielen und nächstem gelieferten Spiel. F1 mit Podium und nächstem Termin.
- Der Ladeeditor-Fehler ist übernommen und durch Regressionstest abgedeckt.

## Noch keine Fertigmeldung für

- Exakte Gerätekontur/Kamera-Kalibrierung und Gestenkollisionen mit HyperOS-Systemrändern.
- Automatische Ladeansicht auf dem Hauptdisplay außerhalb unserer sichtbaren App.
- Garantierte Rückdisplay-Starts jeder Fremd-App oder Benachrichtigung; gesperrte Geräte werden nicht umgangen.
- Lückenlose Live-Sportdaten, professionelle Scratch-Audioverarbeitung, Store-Veröffentlichung und perfekte Übersetzungs-/Hardwareabdeckung.
