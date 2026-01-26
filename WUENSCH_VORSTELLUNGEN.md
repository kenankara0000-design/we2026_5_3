# 💡 Wünsche & Vorstellungen für die App

**Letzte Aktualisierung**: 25. Januar 2026

---

## ✅ **ERLEDIGTE AUFGABEN**

### **25. Januar 2026:**
- ✅ Security Rules implementiert und aktiviert (Realtime Database + Storage)
- ✅ Firebase Authentication hinzugefügt
- ✅ Bildkomprimierung vor Upload implementiert
- ✅ Retry-Logik bei Netzwerkfehlern hinzugefügt
- ✅ Alle Wünsche dokumentiert und analysiert
- ✅ Login-UI verbessert (Button-Text und Hinweis klarer gemacht)
- ✅ **Login vereinfacht**: Anonyme Authentifizierung für Team-Nutzung (5-10 Personen) - kein Login mehr nötig!
- ✅ **Anonymous Authentication aktiviert** in Firebase Console
- ✅ **Section Header Click-Fix**: Überfällig/Erledigt Bereiche können jetzt aufgeklappt werden (Click-Handler verbessert)
- ✅ **🎨 KOMPLETTES DESIGN-REFRESH**: Alle Design-Vorschläge umgesetzt!
  - Primärfarbe: Lila → Modernes Blau (#1976D2)
  - Button-Farben vereinheitlicht (Blau, Grün, Orange, Grau, Rot)
  - Section Headers nach Status eingefärbt (Rot für Überfällig, Grün für Erledigt)
  - Moderne Status-Badges mit abgerundeten Ecken
  - Optimierte Schatten (2dp statt 4dp)
  - Verbesserte Spacing und Padding
  - Runde Buttons (48dp, cornerRadius 24dp)
  - Alle Layouts modernisiert
- ✅ **Foto-Funktion erweitert**: Jetzt möglich, Foto aus Galerie auszuwählen ODER mit Kamera aufzunehmen (Auswahl-Dialog)
- ✅ **Nächstes Tour-Datum**: Wird jetzt in Kunden-Übersicht angezeigt
- ✅ **Navigation-Button**: In Kunden-Übersicht hinzugefügt (öffnet Google Maps mit Adresse)
- ✅ **Google Maps bei Bearbeitung**: Button zum Öffnen von Google Maps für Adress-Auswahl
- ✅ **Section Headers modernisiert**: Neue Drawables mit Rahmen für moderneres Design
- ✅ **AddCustomerActivity Fix**: Button-Problem behoben (finish() auf Main-Thread)
- ✅ **Offline-Modus**: FirebaseConfig als Application-Klasse registriert für besseren Offline-Support
- ✅ **Section Headers nur bei Bedarf**: Überfällig/Erledigt Bereiche werden nur angezeigt, wenn Kunden vorhanden sind
- ✅ **CardView für Section Headers**: Moderneres Design mit CardView und abgerundeten Ecken
- ✅ **Drag & Drop im Tour Planner**: Kunden können durch langes Drücken und Ziehen frei platziert werden
- ✅ **7-Tage-Touren-System**: Jeder Kunde ist an einem Wochentag gebunden (Mo-So)
- ✅ **Reihenfolge-System**: Jeder Kunde hat eine Reihenfolge-Nummer für seinen Wochentag (1, 2, 3, ...)
- ✅ **Wochentag-Auswahl**: Bei Kunden-Erstellung und -Bearbeitung kann der Wochentag ausgewählt werden
- ✅ **Sortierung nach Reihenfolge**: Im Tour Planner werden Kunden nach ihrer Reihenfolge sortiert (statt Fälligkeitsdatum)
- ✅ **Wochentag-Filterung**: Im Tour Planner werden nur Kunden angezeigt, die am angezeigten Wochentag Termine haben
- ✅ **Swipe-Gesten für Datum-Wechsel**: Im Tour Planner kann durch Wischen nach links/rechts zwischen den Tagen gewechselt werden
- ✅ **Loading-Indikatoren verbessert**: ProgressBar in TourPlanner und CustomerManager, visuelles Feedback beim Laden
- ✅ **Error-Handling erweitert**: Aussagekräftige Fehlermeldungen, Retry-Buttons, Error-States mit klaren Anweisungen
- ✅ **Empty States**: "Keine Kunden gefunden" und "Keine Touren für diesen Tag" Meldungen mit Icons
- ✅ **MVVM-Pattern**: Repository Pattern, ViewModels, Dependency Injection mit Hilt implementiert
- ✅ **Dependency Injection**: Hilt eingerichtet, AppModule für Firebase-Abhängigkeiten
- ✅ **Unit-Tests**: Tests für CustomerRepository und CustomerManagerViewModel
- ✅ **Code-Organisation**: Klare Trennung von UI, Logik und Daten

---

## 📝 **ORIGINALE WÜNSCHE (19 Punkte):**

### **1. Touren Planner - Buttons:**
- ✅ Abholung, Auslieferung, Verschiebung und Urlaub Buttons
- ✅ Buttons sollen nur gegenüber fälligen Kunden sichtbar sein

### **2. Erledigte Kunden:**
- ✅ Wenn Abholung und Auslieferung erfolgt ist, auf "erledigt" markieren
- ✅ In den "erledigten Kunden" Bereich verschieben
- ✅ Soll unterhalb der tagesaktuellen Kunden stehen

### **3. Kunden Stamm:**
- ✅ Überall wenn man auf einen Kunden drückt: Übersicht des Kunden
- ✅ Anzeige: Telefon, Notizen, Adresse, Name
- ✅ Abholung, Auslieferung, Verschiebung, Urlaub Bereiche
- ✅ Bearbeiten möglich
- ✅ Lösch-Funktion: Nur im Bearbeitungsmodus möglich
- ✅ Vor Löschung: Sicherheitsabfrage

### **4. Hauptmenü:**
- ✅ App öffnet mit 3 Buttons: "Kunden", "Touren", "Neue Kunden erstellen"
- ✅ Kunden-Button öffnet Kunden Manager mit allen Kunden
- ✅ Suchfunktion im Kunden-Bereich
- ✅ Überall (außer Hauptfenster): Zurück-Button zur vorherigen Seite

### **5. Touren Bereich:**
- ✅ Tagesaktuelle Kunden anzeigen
- ✅ Datum-Wechsel per 2 Pfeile (Vergangenheit/Zukunft)
- ⚠️ Swipe-Geste für Datum-Wechsel (optional - noch nicht implementiert)
- ✅ Überfällige Kunden: Oberhalb der tagesaktuellen Kunden
- ✅ Überfällige Kunden rot markieren

### **6. Firebase:**
- ✅ Online-Funktion über Firebase
- ✅ Mindestens für 500 Kunden ausgelegt

### **7. Intervall-System:**
- ✅ Beim Erstellen: Touren-Intervall festlegen können
- ✅ Ab dem Tag: Intervall-Tage schreiben
- ✅ Unterstützung: 1 Woche, 2 Wochen, 4 Wochen, etc.

### **8. Urlaub-Logik:**
- ✅ Wenn Kunde im Urlaub: Nur Termine im Urlaub-Zeitraum als "Urlaub" markieren
- ✅ Restliche Termine sollen nicht geändert werden

### **9. Verschiebung:**
- ✅ Verschiebung soll nur den Termin betreffen (Standard)
- ✅ Optional: Möglichkeit, alle restlichen Termine auch zu verschieben

### **10. Navigation:**
- ✅ Auf Kunden klicken: Direkt per Google Maps navigieren können

### **11. Foto-Funktionalität:**
- ✅ Im Kunden-Bereich: Fotos in Übersicht sehen
- ✅ Fotos von Abhol- und Auslieferungsort fotografieren
- ✅ Im Bearbeitungsmodus: Für jeden Kunden Foto aufnehmen können
- ✅ Fotos als Thumbnails anzeigen
- ✅ Auf Foto klicken: Vergrößerung

### **12. Qualität:**
- ✅ Modern
- ✅ Robust
- ✅ Stabil
- ✅ Einfach zu bedienen

### **13. Analyse:**
- ✅ Gesamte App analysiert
- ✅ Bericht erstellt
- ✅ Zusätzliche Vorschläge gemacht

### **14. Grundfunktionalität:**
- ✅ App für Touren, Auslieferung, Abholung
- ✅ Kundenstamm mit nötigen Informationen

### **15-19. Projekt-Details:**
- ✅ Projektname: we2026_5
- ✅ Firebase Storage: gs://tourplaner2026.firebasestorage.app
- ✅ JSON-Datei vorhanden

---

## 💡 **MEINE VORSCHLÄGE:**

### **Design & UI/UX:**
- 💡 **Moderne Farbpalette**: Lila → Modernes Blau (#1976D2) für professionelleres Aussehen
- 💡 **Einheitliche Button-Farben**: Konsistente Farben für alle Action-Buttons
- 💡 **Section Header nach Status**: Überfällig (Rot), Erledigt (Grün)
- 💡 **Icons zu Buttons**: Icons statt nur Buchstaben für bessere Erkennbarkeit
- 💡 **Optimierte Schatten**: Leichtere Schatten für moderneres Aussehen
- 💡 **Verbesserte Spacing**: Mehr Abstand zwischen Elementen
- 📄 **Detaillierte Vorschläge**: Siehe `DESIGN_VORSCHLAEGE.md`

### **Performance & Stabilität:**
- ✅ Offline-Modus: Bereits aktiviert
- ✅ Bildkomprimierung: Bereits implementiert
- ✅ Retry-Logik: Bereits implementiert
- 💡 Loading-Indikatoren: Könnte verbessert werden
- 💡 Error-Handling: Könnte erweitert werden

### **Neue Features (Optional):**
- 💡 **Statistiken**: Anzahl Touren pro Tag/Monat anzeigen
- 💡 **Export-Funktion**: PDF/CSV Export der Kundenliste
- 💡 **Benachrichtigungen**: Erinnerungen für anstehende Touren
- 💡 **Swipe-Geste**: Für Datum-Wechsel im Touren-Bereich
- 💡 **Dark Mode**: Unterstützung für dunkles Theme
- 💡 **Mehrsprachigkeit**: Englisch/Deutsch Support

### **Architektur (Langfristig):**
- 💡 **MVVM-Pattern**: Für bessere Code-Organisation
- 💡 **Unit-Tests**: Für kritische Funktionen
- 💡 **Dependency Injection**: Mit Hilt/Koin

---

## 🆕 **NEUE IDEEN (vom Benutzer):**

_Hier werden neue Ideen eingetragen, sobald sie kommen..._

---

## 📌 **NOTIZEN & ANMERKUNGEN:**

_Hier können wichtige Notizen eingetragen werden..._

---

## 🔄 **ÄNDERUNGSHISTORIE:**

### **25. Januar 2026:**
- Datei erstellt
- Alle 19 Wünsche dokumentiert
- Status-Analyse durchgeführt
- Security & Performance Features implementiert

---

**Hinweis**: Diese Datei wird bei jeder Änderung, jedem Vorschlag oder jeder neuen Idee aktualisiert!
