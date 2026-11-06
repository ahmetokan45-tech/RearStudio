# V4 Schritt 02 – gemeinsame Geometrie / 0.10-geometry-preview

## Implementiert

- `DisplayGeometry`: gemeinsame Kontur für Studio-Clip und Studio-Edge-Charging, vier einzelne Rundungsradien, natürliche Displaygröße, optional zwei separat kalibrierte Kamerakreise.
- Gerätewerte über bestehende BackScreen-Erkennung und Android RoundedCorner; bei fehlenden Angaben ausdrücklich geschätzter Fallback aus dem älteren 976×596/101px-Gerätebericht.
- Die Vorschau übernimmt das Seitenverhältnis der Geometrie. Der Ladekonturpfad berücksichtigt die halbe Strichbreite und korrigiert die inneren Radien.
- Keine erfundenen Kameramasken ab Werk: Kamerakreise werden erst nach manueller Aktivierung verwendet. Vorschläge im Dialog sind ausdrücklich ungemessen und keine Sperrzonen.
- Kalibrierung direkt im Editor, vier Radien plus X/Y/Radius beider Linsen. Optional Hilfslinien, Rückkehr zu Gerätewerten.
- Kalibrierung gehört zum Szenenentwurf: Save/Cancel/Undo, temporäre Rückvorschau und dauerhafte Speicherung verwenden dieselben Daten. Ungültige Kalibrierung beim Laden fällt auf Gerätewerte zurück, ohne Widgets zu verlieren.
- Versionskennung 10 / 0.10-geometry-preview; die bestehende Mi-Back-Screen-Anbindung und Bilddateien werden nicht ersetzt.

## Tests und Grenzen

Neue Tests für Szenen-Roundtrip/Persistenz, negative Widgetpositionen trotz Kalibrierung, gemeinsame Pfadabmessungen, Skalierung, fehlende Kameramasken, beschädigte Kalibrierung, getrennten Draft und leeren Viewport.

CI-Ergebnis wird nach Build ergänzt. Lokaler Android-Build weiterhin durch fehlenden Gradle-Download blockiert. Hardware nicht erreichbar: Diese Cloud-Sitzung hat weder MacBook- noch USB-/ADB-Zugriff. Das Anschließen des Telefons am Laptop verbindet es nicht mit dieser Sitzung.

Die Kalibrierung ist eine vorbereitete Funktion, keine Bestätigung der exakten Hardwarekontur. Manueller Vergleich am Xiaomi erforderlich. Die Kontur unterstützt abgerundete Rechtecke mit vier Radien; beliebige Herstellerpfade bleiben offen. Die ältere ChargingLayer hat weiterhin ihren eigenen Pfad, bis eine separate Migration getestet ist. Andere V4-Pakete (vollständiges Widgetsystem, Sport-Livefeeds, Smart Stack usw.) sind weiterhin offen.

## Gerätetest

1. Installation als Update prüfen; bei Signaturkonflikt vorhandene App/Daten erhalten.
2. Editor öffnen, Display kalibrieren: Quelle/Displaygröße ablesen; Radien am realen Panel vergleichen.
3. Kamerakreise nur mit gemessenen Werten aktivieren. Widgets weiterhin durch/zwischen die Kreise verschieben können.
4. Kalibrierung anwenden, Undo und Cancel testen, dann bewusst speichern und App neu öffnen.
5. Live-Rückvorschau und Charging-Edge bei mehreren Dicken/Prozentwerten mit dem sichtbaren Display vergleichen.
6. Shizuku, normale Layouts, Musik, Benachrichtigungen, Ladegerät, Sperre und Doze separat regressieren.
