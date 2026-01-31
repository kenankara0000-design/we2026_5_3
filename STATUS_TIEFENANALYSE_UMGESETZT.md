# Status: Tiefenanalyse – Was ist umgesetzt?

**Abgleich:** BERICHT_TIEFENANALYSE_APP_2026.md vs. aktueller App-Stand  
**Stand:** Januar 2026

---

## 5.1 Hohe Priorität (Stabilität, Wartbarkeit)

| Empfehlung | Status | Nachweis im Code |
|------------|--------|-------------------|
| **CustomerDetailActivity auf ViewModel umstellen** | ✅ Erledigt | `CustomerDetailViewModel` (getCustomerFlow, saveCustomer, deleteCustomer); Activity beobachtet nur `currentCustomer`, `deleted`, `errorMessage`; Coordinator nutzt ViewModel. |
| **Einheitliche Fehlerbehandlung** | ✅ Erledigt | `Result<T>` (util/Result.kt); `LoadState<T>` (util/LoadState.kt); `AppErrorMapper` (toMessage, toSaveMessage, toDeleteMessage, toLoadMessage); Repositories liefern Result; ViewModels setzen errorMessage; Activities zeigen nur an. |
| **Restliche hardcodierte Strings auslagern** | ✅ Erledigt | Sichtbare Texte in strings.xml (TourPlaner 2026, Name fehlt, Speichere…, Offline, Synchronisiere…, Statistik-Labels, Toasts, contentDescriptions). Layouts nutzen @string/; Kotlin nutzt getString(). |
| **Listener-API vollständig ablösen** | ✅ Erledigt | Keine Aufrufe von addCustomerListener/addCustomersListener mehr; CustomerRepository und FakeCustomerRepository enthalten die Methoden nicht mehr. Flow + ViewModel überall (getAllCustomersFlow, getCustomerFlow). |

---

## 5.2 Mittlere Priorität (UX, Konsistenz)

| Empfehlung | Status | Nachweis im Code |
|------------|--------|-------------------|
| **Login-Feedback** | ⏳ Zukunftsplan | In ZUKUNFTSPLAENE.md eingetragen. Nicht umgesetzt (bewusst für später). |
| **App-Name vereinheitlichen** | ✅ Erledigt | strings.xml: `app_name` = „TourPlaner 2026“; Manifest: `android:label="@string/app_name"`; main_title/main_offline in strings. |
| **Statistik-Zurück-Button** | ✅ Erledigt | activity_statistics.xml: `android:src="@drawable/ic_arrow_back"`, contentDescription @string/content_desc_back. |
| **CustomerAdapter-Callbacks bündeln** | ✅ Erledigt | `CustomerAdapterCallbacksConfig` (adapter/CustomerAdapterCallbacksConfig.kt) bündelt onAbholung, onAuslieferung, onKw, onVerschieben, onUrlaub, onRueckgaengig, getAbholungDatum, getAuslieferungDatum, getNaechstesTourDatum, getTermineFuerKunde, onAktionenClick usw. Adapter nutzt `var callbacks: CustomerAdapterCallbacksConfig`. |
| **Barrierefreiheit prüfen** | ✅ Teilweise | contentDescriptions für wichtige Buttons/Views ergänzt (Main, TourPlanner, CustomerManager, Login, item_customer, Karten, Listen). Keine systematische TalkBack-/Kontrast-/48dp-Dokumentation. |

---

## 5.3 Niedrige Priorität (Nice-to-have)

| Empfehlung | Status | Nachweis im Code |
|------------|--------|-------------------|
| **TourPlannerActivity entlasten** | ✅ Erledigt | `TourPlannerViewModel.deleteTerminFromCustomer()` und `resetTourCycle()` übernehmen Logik; Activity ruft nur ViewModel auf und zeigt Ergebnis (Toast/Error, reloadCurrentView). Kein FirebaseRetryHelper/Repository direkt in Activity für diese Aktionen. |
| **Dark Theme durchgängig prüfen** | ✅ Erledigt | values-night/colors.xml mit background_light, surface_white, section_*, customer_* für Night-Modus. values-night/themes.xml vorhanden. Layouts nutzen @color/ – werden im Night-Modus überschrieben. |
| **MapView bei vielen Adressen** | ✅ Erledigt | MapViewActivity: MAX_WAYPOINTS = 25; bei mehr Adressen Filter „nur heute fällig“ (TerminBerechnungUtils.hatTerminAmDatum, KundenListeRepository); Toast map_filtered_today_toast. Fehler über getString(error_message_generic). |

---

## Kurzfassung

- **5.1 Hohe Priorität:** Alle 4 Punkte umgesetzt.
- **5.2 Mittlere Priorität:** 4 von 5 umgesetzt; Login-Feedback bewusst als Zukunftsplan in ZUKUNFTSPLAENE.md.
- **5.3 Niedrige Priorität:** Alle 3 Punkte umgesetzt.

**Offen (bewusst):** Nur Login-Feedback (Zukunftsplan).  
**Optional noch vertiefbar:** Barrierefreiheit (TalkBack-Test, Kontrast-Check, 48dp-Touch-Targets dokumentieren oder anpassen).

Die Empfehlungen aus dem Tiefenanalyse-Bericht sind damit bis auf den Zukunftsplan Login-Feedback umgesetzt.

---

## 2.3 Schwächen und technische Schulden – wurden diese umgesetzt?

| Schwäche im Bericht | Umgesetzt? | Was wurde gemacht |
|---------------------|------------|-------------------|
| **Schwere Activities** (TourPlanner, CustomerDetail) | ✅ Teilweise | CustomerDetail: ViewModel + Coordinator, Activity nur UI. TourPlanner: Termin-Löschen und Tour-Zurücksetzen ins ViewModel (deleteTerminFromCustomer, resetTourCycle). Helper (UISetup, CallbackHandler usw.) bleiben; Activity ist schlanker. |
| **Doppelte State-Quellen** | ✅ Ja | Datum nur im ViewModel (selectedTimestamp); Activity liest/zeigt nur. |
| **Listener vs. Flow** | ✅ Ja | Listener-API entfernt; überall Flow + ViewModel (getCustomerFlow, getAllCustomersFlow). |
| **CustomerAdapter viele Callbacks** | ✅ Ja | Gebündelt in CustomerAdapterCallbacksConfig (eine Datenklasse). |
| **Fehlerbehandlung** | ✅ Ja | Result&lt;T&gt;, LoadState, AppErrorMapper; Repository → ViewModel → Activity. |
| **Hardcodierte Texte** | ✅ Ja | strings.xml, @string/, getString(); contentDescriptions ergänzt. |
| **MapViewActivity (viele Adressen)** | ✅ Ja | Bei &gt; 25 Wegpunkten Filter „nur heute fällig“; Toast-Hinweis; Fehler über getString. |

---

## 3.3 Design-Schwächen – wurden diese umgesetzt?

| Design-Schwäche im Bericht | Umgesetzt? | Was wurde gemacht |
|----------------------------|------------|-------------------|
| **App-Name** (we2026_5 vs. TourPlaner 2026) | ✅ Ja | app_name = „TourPlaner 2026“, Manifest nutzt @string/app_name. |
| **Statistik-Labels hardcodiert** | ✅ Ja | Labels in strings.xml (stat_heute_faellig, stat_diese_woche usw.), Layouts nutzen @string/. |
| **Statistik-Zurück-Button** (ic_menu_revert) | ✅ Ja | activity_statistics.xml nutzt ic_arrow_back wie andere Screens. |
| **Barrierefreiheit** (contentDescriptions, TalkBack, 48dp) | ✅ Teilweise | contentDescriptions für wichtige Buttons/Views ergänzt (Login, item_customer, Main, TourPlanner …). TalkBack/Kontrast/48dp nicht systematisch dokumentiert. |
| **Kundenkarte zu voll** (Telefon nur im Sheet?) | ❌ Nein | Unverändert; war optionaler Vorschlag, nicht umgesetzt. |
| **Dark Theme** (values-night) | ✅ Ja | values-night/colors.xml mit Hintergrund-, Flächen- und Section-Farben; Layouts nutzen @color/. |
