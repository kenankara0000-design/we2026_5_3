# Problem: Überfällig und Erledigt Sections nicht sichtbar

## 🔍 Gefundenes Problem

### Aktuelles Verhalten:
- ❌ "ÜBERFÄLLIG" Section-Header wird nicht angezeigt
- ❌ "ERLEDIGT" Section-Header wird nicht angezeigt

### Ursache:

**In `TourPlannerViewModel.kt` Zeile 78:**
```kotlin
private val expandedSections = mutableSetOf<SectionType>()  // ❌ Leer - Sections sind nicht expanded
```

**In `TourPlannerViewModel.kt` Zeile 212-226:**
```kotlin
if (overdueGewerblich.isNotEmpty()) {
    items.add(ListItem.SectionHeader("ÜBERFÄLLIG", overdueGewerblich.size, SectionType.OVERDUE))
    if (expandedSections.contains(SectionType.OVERDUE)) {  // ❌ Nur wenn expanded
        overdueGewerblich.forEach { items.add(ListItem.CustomerItem(it)) }
    }
}
```

**Problem:**
- Section-Header werden erstellt (Zeile 213, 222)
- Aber die Sections sind standardmäßig NICHT expanded
- Die Header sollten aber IMMER sichtbar sein, auch wenn nicht expanded
- Die Kunden darin werden nur angezeigt, wenn expanded

---

## 🔧 Lösung

### Option 1: Sections standardmäßig expanded machen (Empfohlen)

**Vorteile:**
- Sections sind sofort sichtbar
- Benutzer sieht sofort überfällige/erledigte Kunden
- Bessere UX

**Nachteile:**
- Sections sind immer expanded (kann eingeklappt werden)

### Option 2: Sections immer anzeigen, aber standardmäßig eingeklappt

**Vorteile:**
- Sections können eingeklappt werden
- Header sind immer sichtbar

**Nachteile:**
- Benutzer muss Sections manuell expandieren

---

## 📋 Empfehlung

**Option 1 ist besser:** Sections standardmäßig expanded machen, damit Benutzer sofort sieht, was überfällig oder erledigt ist.

**Ende des Berichts**
