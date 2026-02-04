# Refactoring-Plan – TourPlaner we2026_5

**Stand:** Februar 2026

---

## Ausgangslage

- App vollständig auf Compose umgestellt; alle Screens nutzen Jetpack Compose
- **CustomerAdapter / RecyclerView-Pfad** ist Legacy-Code – wird nicht mehr genutzt
- Liste-/Tour-Umbenennung: "Liste" → "Tour" (Rückwärtskompatibilität an vielen Stellen)
- Deprecated Felder: `intervallTage`, `abholungDatum`, etc.
- Kleinere Code-Qualitätsprobleme (Condition always true, hardcodierte Texte)

---

## Phase 1: CustomerAdapter-Legacy entfernen (Priorität hoch) ✅ erledigt

**Ziel:** Toten RecyclerView/Adapter-Code entfernen, da die App vollständig Compose nutzt.

### Gelöschte Dateien
| Datei | Grund |
|-------|-------|
| `CustomerAdapter.kt` | TourPlanner nutzt Compose, Adapter ungenutzt |
| `CustomerItemHelper.kt` | Nur für CustomerAdapter |
| `CustomerViewHolderBinder.kt` | Nur für CustomerAdapter |
| `CompletionHintsHelper.kt` | Nur für CustomerViewHolderBinder |
| `CustomerAdapterCallbacks.kt` | Nur für CustomerAdapter |
| `CustomerAdapterCallbacksConfig.kt` | Nur für CustomerAdapter |
| `item_customer.xml` | Nur für CustomerAdapter |
| `item_section_header.xml` | Nur für CustomerAdapter |

### Anpassungen
- `CustomerButtonVisibilityHelper`: `apply()` und alle ItemCustomerBinding-Methoden entfernt, nur `getSheetState()` bleibt (für Compose)
- `ListItem.kt`, `CustomerDialogHelper`: Kommentare angepasst

### Was bleibt
- `ListItem.kt` – von TourDataProcessor und TourPlannerScreen genutzt
- `CustomerDialogHelper`, `CustomerButtonVisibilityHelper` – von TourPlannerCoordinator/Activity genutzt

---

## Phase 2: Liste → Tour Migration (Priorität mittel)

| # | Maßnahme | Dateien |
|---|----------|---------|
| 2.1 | ~~DB-Migration: kundenArt "Liste" → "Tour"~~ ✅ erledigt | ListeToTourMigration.kt, MainActivity |
| 2.2 | Alle `kundenArt == "Liste" \|\| kundenArt == "Tour"` durch `== "Tour"` ersetzen | AddCustomerActivity, AddCustomerScreen, CustomerDetailScreen, CustomerTypeButtonHelper, ListeBearbeitenScreen, ListeBearbeitenViewModel, CustomerManagerViewModel, TourDataProcessor, TourPlannerScreen |
| 2.3 | Save-Logik: keine Umwandlung `"Liste" → "Tour"` mehr | AddCustomerActivity, CustomerDetailScreen |

---

## Phase 3: Deprecated-Felder Migration (Priorität mittel)

| # | Maßnahme | Dateien |
|---|----------|---------|
| 3.1 | ~~`intervallTage` durch `intervalle` ersetzen~~ ✅ erledigt | CustomerDetailScreen.kt |
| 3.2 | ~~intervallTage aus intervalle bevorzugen~~ ✅ erledigt | CustomerExportHelper, TerminAusKundeUtils, TerminAnlegenUnregelmaessigActivity |
| 3.3 | `Customer.getFaelligAm()` auf neue Struktur umstellen oder als deprecated markieren | Customer.kt |

---

## Phase 4: Einzelne Fixes (Priorität niedrig)

| # | Maßnahme | Dateien |
|---|----------|---------|
| 4.1 | ~~"Condition always true" (Zeile 299) beheben~~ ✅ erledigt | CustomerDetailScreen.kt |
| 4.2 | ~~Hardcodierte Dialog-Texte in strings.xml auslagern~~ ✅ erledigt | ListeBearbeitenCallbacks, CustomerExportHelper, CustomerPhotoManager, TerminRegelManagerActivity |

---

## Phase 5: TourDataProcessor aufteilen (optional)

| # | Maßnahme | Dateien |
|---|----------|---------|
| 5.1 | ~~Wochentagslisten vs. Tour-Listen in separate Methoden trennen~~ ✅ erledigt | TourDataProcessor.kt: fillWochentagslisten(), fillTourListen() |
| 5.2 | (optional) Interfaces: `WochentagslistenProcessor`, `TourListenProcessor` – bei Bedarf in eigene Klassen | tourplanner/ |

---

## Abhängigkeiten

```
Phase 1 → unabhängig
Phase 2.1 → Phase 2.2, 2.3
Phase 3, 4, 5 → jederzeit parallel möglich
```

---

## Empfohlene Reihenfolge

1. **Phase 1** – CustomerAdapter-Legacy entfernen
2. **Phase 4.1** – Condition-always-true beheben
3. **Phase 2** – Liste→Tour Migration (wenn DB-Änderung gewünscht)
4. **Phase 4.2** – Strings auslagern
5. **Phase 3 und 5** – nach Bedarf
