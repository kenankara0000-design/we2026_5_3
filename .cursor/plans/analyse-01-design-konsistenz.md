# Analyse 01: Design-Konsistenz

**Status:** ✅ Erledigt (2026-02-13)  
**Quelle:** Extrahiert aus `2026-02-13_21-02.plan.md` (Abschnitte 3, 9, 13, 15)  
**Visuell:** Siehe `2026-02-13_21-02.design-vorher-nachher.html`

---

## 1. Farben – Inkonsistenz

| Problem | Stellen | Empfehlung |
|---------|---------|------------|
| Hardcodiert `Color.White` | `MainScreen:158, 279` | → `colorResource(R.color.white)` |
| Hardcodiert `Color(0xFFE0E0E0)` | `CustomerDetailStammdatenTab:87`, `ListeBearbeitenMetadatenBlock:46`, `ListeErstellenScreen:212` | → `colorResource(R.color.light_gray)` |
| Hardcodiert `Color(0xFFFFEB3B)` | `TourPlannerTopBar:159, 164` | → `colorResource(R.color.offline_yellow)` |
| Hardcodiert `Color(0xFF388E3C)` | `TourPlannerCustomerRow:47` | → `colorResource(R.color.status_done)` |
| Hardcodiert `Color(0xFFF5F5F5)` | `UrlaubWindowContent:81, 93` | → `colorResource(R.color.surface_light)` |
| Doppelte Farben in `colors.xml` | `status_overdue` = `error_red` = `#D32F2F`; `status_done` = `button_auslieferung` = `#388E3C` | Konsolidieren |
| Veraltete Farbe | `purple_500` ("alte Primärfarbe") | Prüfen und entfernen |

---

## 2. Typografie – Kein einheitliches System

| Verwendung | Werte (verstreut) | Empfohlener Standard |
|-----------|-------------------|---------------------|
| Titel groß | `28.sp` (MainScreen) | `TitleLarge = 28.sp` |
| Titel mittel | `20.sp` (diverse Screens) | `TitleMedium = 20.sp` |
| Titel klein | `18.sp` (TourListeCardRow) | `TitleSmall = 18.sp` |
| Section-Titel | `16.sp` (DetailUiConstants) | `SectionTitle = 16.sp` |
| Body | `14.sp` (DetailUiConstants) | `Body = 14.sp` |
| Button-Text | `14-17.sp` (inkonsistent!) | `ButtonText = 16.sp` |
| Caption | `12.sp` (diverse) | `Caption = 12.sp` |
| Small | `10.sp` (diverse) | `Small = 10.sp` |

**`DetailUiConstants`** existiert, wird aber nur in ~3 Screens genutzt.

---

## 3. Spacing – Inkonsistenz

| Verwendung | Werte (verstreut) | Empfohlener Standard |
|-----------|-------------------|---------------------|
| Screen-Padding | `16dp`, `20dp`, `24dp`, `32dp` | `ScreenPadding = 16.dp` |
| Card-Padding | `12dp`, `16dp`, `20dp` | `CardPadding = 16.dp` |
| Feld-Abstand | `8dp`, `12dp`, `16dp` | `FieldSpacing = 12.dp` |
| Section-Abstand | `16dp`, `20dp`, `24dp`, `28dp` | `SectionSpacing = 20.dp` |

---

## 4. Buttons – Inkonsistenz

| Eigenschaft | Werte (verstreut) | Empfohlener Standard |
|------------|-------------------|---------------------|
| Höhe (Primary) | `48dp`, `56dp`, `72dp` | `PrimaryHeight = 48.dp` |
| Höhe (Secondary) | `44dp`, `48dp`, `64dp` | `SecondaryHeight = 44.dp` |
| Corner-Radius | `8dp`, `12dp`, `16dp`, `20dp` | `CornerRadius = 12.dp` |

---

## 5. Fehlende wiederverwendbare Composables

| Composable | Beschreibung | Nutzer-Screens |
|-----------|-------------|----------------|
| `AppTopBar` | Standard-TopBar mit Back-Button, Titel, Actions | Alle Screens |
| `AppLoadingView` | Einheitlicher Lade-Indikator | Alle Screens |
| `AppErrorView` | Fehlermeldung mit Retry-Button | Alle Screens |
| `AppEmptyView` | Leerzustands-Anzeige | Alle Listen-Screens |
| `AppPrimaryButton` | Primärer Button (einheitlich) | Alle Screens |
| `AppOutlinedButton` | Outlined Button (einheitlich) | Diverse Screens |
| `AppCard` | Standard-Card mit Padding/Elevation | Diverse Screens |
| `AppTextField` | Standard-Textfeld | Formulare |
| `AppBadge` | Status-Badges (A, L, Überfällig usw.) | Tour, Kunden |
| `AppSectionHeader` | Section-Überschrift | Detail, Listen |

Jeder Screen hat aktuell eigene Loading/Error/Empty-Views.

---

## 6. Doppelte Farben in colors.xml

| Farbe 1 | Farbe 2 | Wert | Aktion |
|---------|---------|------|--------|
| `status_overdue` | `error_red` | `#D32F2F` | Konsolidieren |
| `status_done` | `button_auslieferung` | `#388E3C` | Konsolidieren |
| `primary_blue` | `button_blue` | `#1976D2` / `#2196F3` | Klären |

---

## 7. Accessibility (Design-relevant)

| Problem | Empfehlung |
|---------|------------|
| Fehlende contentDescription bei Icons | Alle Icons beschriften |
| Farbkontrast nicht verifiziert | WCAG AA prüfen |
| Touch-Target teilweise < 48dp | Minimum 48dp sicherstellen |

---

## 8. Empfehlung: Zentrales Design-System

```
ui/theme/
  ├── AppTheme.kt          // MaterialTheme-Wrapper + ColorScheme
  ├── AppColors.kt         // Alle Farb-Konstanten
  ├── AppTypography.kt     // Typography-System
  ├── AppSpacing.kt        // Spacing-Konstanten
  ├── AppButtonStyles.kt   // Button-Konfigurationen
  └── AppComponents.kt     // Wiederverwendbare Composables
```

---

*Keine Umsetzung ohne ausdrückliche Freigabe.*
