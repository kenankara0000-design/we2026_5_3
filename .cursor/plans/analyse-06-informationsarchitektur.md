# Analyse 06: Informationsarchitektur

**Status:** ✅ Erledigt (2026-02-13)  
**Priorität:** 🟡 Mittel

---

## 1. Kunden-Karte im Tourenplaner

**Datei:** `TourPlannerCustomerRow.kt`

| Info | Vorhanden | Sichtbar auf einen Blick |
|------|-----------|--------------------------|
| Kundenart-Badge (G/P/L) | ✅ | ✅ |
| Name (displayName) | ✅ | ✅ |
| A/L-Wochentage (AlWochentagText) | ✅ | ✅ |
| Status-Badge (A, L, AL, Überfällig, Urlaub, Verschoben) | ✅ | ✅ |
| verschobenInfo / verschobenVonInfo | ✅ | ✅ (wenn vorhanden) |
| Button „Aktionen" | ✅ | ✅ |
| **Adresse / PLZ** | ❌ | — |
| **Telefon** | ❌ | — |
| **Nächster Termin / Fälligkeitsdatum** | ❌ | — |
| **Listenzugehörigkeit** | ❌ (nur über ListeHeader) | — |

**Bewertung:** Die wichtigsten Infos (Wer, Wann, Status) sind sichtbar. Adresse fehlt – für Tourenfahrer relevant. Telefon fehlt – für Kontaktaufnahme unterwegs relevant.

---

## 2. Kunden-Karte in der Kundenliste

**Datei:** `CustomerManagerCard.kt`

| Info | Vorhanden | Sichtbar auf einen Blick |
|------|-----------|--------------------------|
| Foto-Thumbnail (40×40dp) | ✅ | ✅ |
| Kundenart-Badge (G/P/L) | ✅ | ✅ |
| Name (displayName, 18sp Bold) | ✅ | ✅ |
| Adresse | ✅ | ✅ |
| A/L-Wochentage | ✅ | ✅ |
| **Status (Pausiert / Ad-hoc)** | ❌ | — |
| **Telefon** | ❌ | — |
| **Listenzugehörigkeit** | ❌ | — |
| **Ohne-Tour-Markierung** | ❌ | — |

**Bewertung:** Name und Adresse sichtbar – gut. Status (pausiert, ad-hoc) fehlt – relevant beim Durchblättern.

---

## 3. Kundendetail – Tab Stammdaten

| Info | Vorhanden |
|------|-----------|
| Name (als TopBar-Titel) | ✅ |
| Alias | ✅ |
| Adresse, PLZ, Stadt | ✅ (klickbar → Maps) |
| Telefon | ✅ (klickbar → Anruf) |
| Notizen | ✅ |
| Fotos | ✅ (LazyRow) |
| **Kundennummer** | Nur in „Weitere Angaben" (eingeklappt) |
| **Status (Aktiv/Pausiert)** | ❌ (nur im Termine-Tab) |
| **Überfällig-Hinweis** | ❌ (obwohl `statusOverdue` übergeben wird) |
| **Offene Belegeanzahl** | ❌ |

**Bewertung:** Kontaktdaten gut erreichbar. Status und Überfällig-Info fehlen im Stammdaten-Tab – der Nutzer muss zum Termine-Tab wechseln.

---

## 4. Kundendetail – Tab Termine

| Info | Vorhanden |
|------|-----------|
| Status (Aktiv/Pausiert/Ad-hoc) | ✅ |
| Nächster Termin | ✅ |
| Kundenart, Kunden-Typ | ✅ |
| A/L-Wochentage | ✅ |
| Ausnahme-Termine | ✅ (einklappbar) |
| Kunden-Termine | ✅ (einklappbar) |
| Alle Termine 365 Tage | ✅ (max. 6 Zeilen) |
| Tour-Listen-Hinweis | ✅ |

**Bewertung:** Umfangreich und vollständig.

---

## 5. Kundendetail – Tab Belege

| Info | Vorhanden |
|------|-----------|
| Offene Belege (pro Monat) | ✅ |
| Erledigte Belege | ✅ (Segmented Button) |
| Anzahl Erfassungen pro Monat | ✅ |
| Gesamtsumme | ❌ (erst im BelegDetail) |

---

## 6. Hauptbildschirm (Ad-hoc Slots)

| Info | Vorhanden |
|------|-----------|
| Fällige Touren (Badge-Zahl) | ✅ |
| Ad-hoc-Slot-Vorschläge (max. 5) | ✅ |
| Slot: Kundenname, Datum, Beschreibung | ✅ |
| **Überfällige Anzahl** | ❌ (nur Tour-Fälligkeit gesamt) |

---

## 7. Listen-Karte (`KundenListenListenItem.kt`)

| Info | Vorhanden |
|------|-----------|
| Listenname | ✅ |
| ListeArt | ✅ |
| Kundenanzahl | ✅ |
| ErstelltAm | ✅ |
| Wochentag-Hinweis (bei leerer Liste) | ✅ |

---

## 8. Erfassung (Kunden-Auswahl)

| Info | Vorhanden |
|------|-----------|
| Kundenname | ✅ |
| Adresse | ❌ (nur Name in Suchtreffer) |
| Alias | ❌ |

---

## 9. Belege-Übersicht

| Info | Vorhanden |
|------|-----------|
| Kundenname | ✅ |
| Monatslabel | ✅ |
| Anzahl Erfassungen | ✅ |
| **Gesamtpreis** | ❌ (erst im BelegDetail) |

---

## 10. Statistiken-Screen

| Info | Vorhanden |
|------|-----------|
| 9 StatCards (heute/Woche/Monat) | ✅ |
| Fällig, Überfällig, Erledigt | ✅ |
| Erledigungsquote | ✅ |
| Gesamtkunden | ✅ |
| **Aktuell deaktiviert** (`STATISTICS_SLEEP_MODE = true`) | ⚠️ |

---

## Redundanzen

| Info | Ort 1 | Ort 2 | Bewertung |
|------|-------|-------|-----------|
| Kundenart / Kunden-Typ | Stammdaten-Form (Edit) | Termine-Tab (KundenTypSection) | Beabsichtigt (Edit vs. Read), aber verwirrend |
| Status-Badge | TourPlanner-Karte | OverviewDialog | Nützlich, aber doppelt |
| A/L-Wochentage | TourPlanner-Karte | OverviewDialog, Kundendetail-Termine | Konsistent |

---

## Fehlende Infos (zusammengefasst)

| Fehlende Info | Wo relevant | Auswirkung |
|---------------|-------------|------------|
| Adresse auf Tour-Karte | Tourenplaner | Fahrer sieht nicht, wo er hinfährt |
| Telefon auf Tour-Karte | Tourenplaner | Kein schneller Anruf möglich |
| Status in Kundenliste | CustomerManager | Pausierte/Ad-hoc-Kunden nicht erkennbar |
| Überfällig-Hinweis in Stammdaten-Tab | Kundendetail | Nutzer muss zum Termine-Tab wechseln |
| Gesamtpreis in Belege-Übersicht | Belege | Erst im Detail sichtbar |
| Adresse/Alias in Erfassung-Suche | WaschenErfassung | Verwechslungsgefahr bei ähnlichen Namen |

---

*Keine Umsetzung ohne ausdrückliche Freigabe.*
