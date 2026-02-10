package com.example.we2026_5.sevdesk

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Einfacher REST-Client für SevDesk API.
 * Basis: https://my.sevdesk.de/api/v1
 * Auth: Authorization-Header mit API-Token.
 * Nutzt OkHttp (robuster bei SSL „chain validation failed“ auf einigen Android-Geräten).
 *
 * Nur Lesezugriff: Es werden ausschließlich GET-Requests ausgeführt.
 * Es werden keine Daten in SevDesk erstellt, geändert oder gelöscht.
 */
object SevDeskApi {

    /** SevDesk-Kategorie „Kunde“ (Customer) – nur diese Kontakte werden importiert. */
    private const val CATEGORY_ID_KUNDE = "3"

    private const val BASE_URL = "https://my.sevdesk.de/api/v1"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** URL-String für GET (Token in Query + Header). */
    private fun urlStringWithToken(path: String, query: String, token: String): String {
        val t = token.trim()
        val q = if (query.isNotEmpty()) "$query&" else ""
        return "$BASE_URL$path?${q}token=${java.net.URLEncoder.encode(t, "UTF-8")}"
    }

    /** GET ausführen (OkHttp). Bei Code != 200 wird SevDeskApiException mit optionalem Präfix geworfen. */
    private fun doGet(token: String, path: String, query: String, errorPrefix: String = ""): String {
        val (code, body) = doGetWithCode(token, path, query)
        if (code != 200) throw SevDeskApiException(errorPrefix + body.ifBlank { "HTTP $code" })
        return body
    }

    /** GET ausführen, liefert (ResponseCode, Body). */
    private fun doGetWithCode(token: String, path: String, query: String): Pair<Int, String> {
        val url = urlStringWithToken(path, query, token)
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .apply {
                val t = token.trim()
                if (t.isNotEmpty()) addHeader("Authorization", t)
            }
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            return response.code to body
        }
    }

    /**
     * Holt nur Kontakte vom Typ „Kunde“ (SevDesk-Kategorie Customer, ID 3).
     * depth=1 = Firmen + Personen; embed=addresses für Adressdaten.
     * @return Liste von [SevDeskContact] oder Fehler als Exception
     */
    fun getContacts(token: String): List<SevDeskContact> {
        val list = mutableListOf<SevDeskContact>()
        var offset = 0
        val limit = 1000
        val categoryFilter = "category[id]=$CATEGORY_ID_KUNDE&category[objectName]=Category"
        do {
            val query = "depth=1&embed=addresses&$categoryFilter&limit=$limit&offset=$offset"
            val json = doGet(token, "/Contact", query, "Kontakte: ")
            val root = JSONObject(json)
            val arr = when {
                root.has("objects") && root.get("objects") is JSONArray -> root.getJSONArray("objects")
                root.optJSONObject("objects")?.has("Contact") == true -> root.getJSONObject("objects").getJSONArray("Contact")
                else -> JSONArray()
            }
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (!isCategoryKunde(obj)) continue
                val name = contactDisplayName(obj)
                if (name.isNotBlank()) {
                    val (adresse, plz, stadt) = contactAddressFromObject(obj)
                    list.add(
                        SevDeskContact(
                            id = obj.optString("id", "").ifBlank { obj.optString("objectId", "") },
                            name = name.trim(),
                            adresse = adresse,
                            plz = plz,
                            stadt = stadt
                        )
                    )
                }
            }
            offset += limit
            if (arr.length() < limit) break
        } while (true)
        return list
    }

    /** Nur Kontakte mit Kategorie „Kunde“ (ID 3). Fehlt category im JSON, wird eingeschlossen (API-Filter liefert dann nur Kunden). */
    private fun isCategoryKunde(obj: JSONObject): Boolean {
        val cat = obj.optJSONObject("category") ?: return true
        return cat.optString("id", "").trim() == CATEGORY_ID_KUNDE
    }

    /**
     * SevDesk Contact: Organisation hat "name", Person (Privat) hat Vorname + Nachname.
     * API liefert teils camelCase (sureName, familyName), teils lowercase – beide lesen.
     * Leer oder Literal "null" werden nicht als gültiger Name verwendet.
     */
    private fun contactDisplayName(obj: JSONObject): String {
        val name = optStringTrim(obj, "name")
        if (name.isNotEmpty() && name.lowercase() != "null") return name
        val family = optStringTrim(obj, "familyName").ifBlank { optStringTrim(obj, "familyname") }
        val sure = optStringTrim(obj, "sureName").ifBlank { optStringTrim(obj, "surename") }
        val vorname = optStringTrim(obj, "firstname").ifBlank { optStringTrim(obj, "firstName") }
        val nachname = family.ifBlank { optStringTrim(obj, "lastName").ifBlank { optStringTrim(obj, "lastname") } }
        val first = sure.ifBlank { vorname }
        val last = family.ifBlank { nachname }
        val combined = listOf(first, last).filter { it.isNotEmpty() && it.lowercase() != "null" }.joinToString(" ").trim()
        return if (combined.lowercase() == "null") "" else combined
    }

    private fun optStringTrim(obj: JSONObject, key: String): String =
        obj.optString(key, "").trim()

    /**
     * Adresse aus Contact-Objekt: Direkt (street/zip/city) oder aus eingebettetem "addresses"-Array
     * (SevDesk liefert Adressen oft nur per embed=addresses).
     */
    private fun contactAddressFromObject(obj: JSONObject): Triple<String, String, String> {
        // Zuerst eingebettete Adressen (embed=addresses)
        val addresses = obj.optJSONArray("addresses")
        if (addresses != null && addresses.length() > 0) {
            val first = addresses.optJSONObject(0) ?: return tripleFromFlat(obj)
            val street = first.optString("street", "").trim().ifBlank { first.optString("address", "").trim() }
            val zip = first.optString("zip", "").trim()
            val city = first.optString("city", "").trim()
            return Triple(street, zip, city)
        }
        val single = obj.optJSONObject("address") ?: obj.optJSONObject("contactAddress")
        if (single != null) {
            val street = single.optString("street", "").trim().ifBlank { single.optString("address", "").trim() }
            return Triple(street, single.optString("zip", "").trim(), single.optString("city", "").trim())
        }
        return tripleFromFlat(obj)
    }

    private fun tripleFromFlat(obj: JSONObject): Triple<String, String, String> =
        Triple(
            obj.optString("street", "").trim().ifBlank { obj.optString("address", "").trim() },
            obj.optString("zip", "").trim(),
            obj.optString("city", "").trim()
        )

    /**
     * Holt alle Artikel (Parts).
     * @return Liste von [SevDeskPart] oder Fehler als Exception
     */
    fun getParts(token: String): List<SevDeskPart> {
        val list = mutableListOf<SevDeskPart>()
        var offset = 0
        val limit = 1000
        do {
            val query = "embed=unity&limit=$limit&offset=$offset"
            val json = doGet(token, "/Part", query, "Artikel: ")
            val root = JSONObject(json)
            val arr = when {
                root.has("objects") && root.get("objects") is JSONArray -> root.getJSONArray("objects")
                root.optJSONObject("objects")?.has("Part") == true -> root.getJSONObject("objects").getJSONArray("Part")
                else -> JSONArray()
            }
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name", "").trim().ifBlank { obj.optString("partNumber", "").trim() }
                if (name.isNotEmpty()) {
                    val price = (obj.opt("price") as? Number)?.toDouble() ?: 0.0
                    val unit = partUnityFromObject(obj)
                    list.add(
                        SevDeskPart(
                            id = obj.optString("id", "").ifBlank { obj.optString("objectId", "") },
                            name = name,
                            price = price,
                            unit = unit
                        )
                    )
                }
            }
            offset += limit
            if (arr.length() < limit) break
        } while (true)
        return list
    }

    /** Einheit aus Part: unity.name oder unity.code, sonst leer. */
    private fun partUnityFromObject(obj: JSONObject): String {
        val unity = obj.optJSONObject("unity") ?: return ""
        return unity.optString("name", "").trim()
            .ifBlank { unity.optString("code", "").trim() }
            .ifBlank { unity.optString("id", "").trim() }
    }

    /**
     * Holt alle Kundenpreise (PartContactPrice: Contact + Part + Preis).
     * API-Modell: PartContactPrice; Felder: contact, part, type, priceNet, priceGross.
     */
    fun getPartContactPrices(token: String): List<SevDeskPartContactPrice> {
        val list = mutableListOf<SevDeskPartContactPrice>()
        var offset = 0
        val limit = 100
        do {
            val query = "limit=$limit&offset=$offset"
            val json = doGet(token, "/PartContactPrice", query, "PartContactPrice: ")
            val root = JSONObject(json)
            val arr = when {
                root.has("objects") && root.get("objects") is JSONArray -> root.getJSONArray("objects")
                root.optJSONObject("objects")?.has("PartContactPrice") == true ->
                    root.getJSONObject("objects").getJSONArray("PartContactPrice")
                else -> JSONArray()
            }
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val contactId = obj.optJSONObject("contact")?.optString("id", "")?.trim()
                    ?: obj.optString("contact", "").trim()
                val partId = obj.optJSONObject("part")?.optString("id", "")?.trim()
                    ?: obj.optString("part", "").trim()
                val priceNet = (obj.opt("priceNet") as? Number)?.toDouble() ?: 0.0
                val priceGross = (obj.opt("priceGross") as? Number)?.toDouble() ?: 0.0
                if (contactId.isNotBlank() && partId.isNotBlank()) {
                    list.add(
                        SevDeskPartContactPrice(
                            contactId = contactId,
                            partId = partId,
                            priceNet = priceNet,
                            priceGross = priceGross
                        )
                    )
                }
            }
            offset += limit
            if (arr.length() < limit) break
        } while (true)
        return list
    }

    // ---------- Nur zum Testen: Roh-Responses prüfen (kundenspezifische Preise/Artikel) ----------

    private const val TEST_RAW_MAX_LEN = 8000

    /**
     * Führt einen GET aus und gibt die Response-Body als String zurück (gekürzt).
     * Nur zum Testen, ob z. B. embed=partUnitPrices kundenspezifische Preise liefert.
     */
    fun getRawForTest(token: String, path: String, query: String): String {
        val (_, body) = getRawForTestWithCode(token, path, query)
        return body
    }

    /** GET ausführen und (ResponseCode, Body-String) zurückgeben. Body bei Fehler: "HTTP code" + Fehler-JSON (gekürzt). */
    fun getRawForTestWithCode(token: String, path: String, query: String): Pair<Int, String> {
        val (code, body) = doGetWithCode(token, path, query)
        val errBody = if (code == 200) body else "HTTP $code\n$body"
        val truncated = if (errBody.length <= TEST_RAW_MAX_LEN) errBody
        else errBody.take(TEST_RAW_MAX_LEN) + "\n\n… (gekürzt, ${errBody.length} Zeichen gesamt)"
        return code to truncated
    }

    /**
     * Test 1: GET /Part mit embed=unity,partUnitPrices (limit 2).
     * Liefert die API kundenspezifische Preise, sollten sie in partUnitPrices o. ä. erscheinen.
     */
    fun testPartsWithEmbed(token: String): String {
        val q = "limit=2&embed=unity,partUnitPrices"
        return getRawForTest(token, "/Part", q)
    }

    /**
     * Test 2: GET /Contact für einen einzelnen Kunden (erster Treffer, limit 1) mit erweitertem embed.
     * Prüfen, ob Kontakt kundenspezifische Artikel/Preise enthält.
     */
    fun testContactWithEmbed(token: String): String {
        val filter = "category[id]=$CATEGORY_ID_KUNDE&category[objectName]=Category&limit=1&embed=addresses"
        return getRawForTest(token, "/Contact", filter)
    }

    /**
     * Test 3: GET /PartUnitPrice – in SevDesk UI „Kundenpreis“; Verknüpfung Part + Kontakt + Preis.
     * Liefert die API kundenspezifische Preislisten (pro Kunde eigene Artikelpreise).
     * Bei 400/404: kurze Meldung (Model not found). Bei 404 Fallback /PartPrice.
     */
    fun testPartUnitPrice(token: String): String {
        val (code, partUnit) = getRawForTestWithCode(token, "/PartUnitPrice", "limit=25")
        if (code != 200) {
            val shortMsg = if (partUnit.contains("Model_PartUnitPrice not found")) "400 – Model PartUnitPrice nicht in der API."
            else partUnit.take(300)
            return shortMsg + (if (code == 404) "\n\n→ Fallback GET /PartPrice:\n\n" + getRawForTest(token, "/PartPrice", "limit=25") else "")
        }
        return partUnit
    }

    /**
     * Test 4: GET /PartUnitPrice gefiltert nach einem Kontakt (Kunde).
     * contactId = SevDesk Contact-ID (z. B. aus GET /Contact).
     */
    fun testPartUnitPriceForContact(token: String, contactId: String): String {
        if (contactId.isBlank()) return "contactId leer"
        val q = "limit=50&contact[id]=${contactId.trim()}&contact[objectName]=Contact"
        val (code, body) = getRawForTestWithCode(token, "/PartUnitPrice", q)
        if (code != 200 && body.contains("Model_PartUnitPrice not found")) return "400 – Model PartUnitPrice nicht in der API."
        return body
    }

    /**
     * Liefert die ID des ersten Kunden (für Test: PartUnitPrice pro Kunde).
     * GET /Contact?limit=1, Kategorie Kunde; gibt id des ersten Objekts zurück oder null.
     */
    fun getFirstCustomerIdForTest(token: String): String? {
        val json = getRawForTest(token, "/Contact", "category[id]=$CATEGORY_ID_KUNDE&category[objectName]=Category&limit=1&embed=addresses")
        if (json.startsWith("HTTP ")) return null
        return try {
            val root = JSONObject(json)
            val arr = when {
                root.has("objects") && root.get("objects") is JSONArray -> root.getJSONArray("objects")
                root.optJSONObject("objects")?.has("Contact") == true -> root.getJSONObject("objects").getJSONArray("Contact")
                else -> null
            }
            if (arr != null && arr.length() > 0) arr.getJSONObject(0).optString("id", "").takeIf { it.isNotBlank() } else null
        } catch (_: Exception) {
            null
        }
    }

    private const val DISCOVERY_SNIPPET_LEN = 2500

    /**
     * Discovery-Test: Welche API liefert „welche Artikel zu welchem Kunden“?
     * Probiert Endpunkte und Contact-Embeds durch, gibt kompakte Übersicht + bei 200 OK Ausschnitt.
     */
    fun runDiscoveryTest(token: String): String {
        val contactId = getFirstCustomerIdForTest(token) ?: "—"
        return buildString {
            append("Ziel: Artikel ↔ Kunde (welche Artikel gehören zu welchem Kunden).\n")
            append("Erster Kunde (Contact-ID): $contactId\n\n")
            append("——— Endpunkte (Verknüpfung Part/Kontakt) ———\n\n")

            val endpoints = listOf(
                "/ContactPart" to "limit=25",
                "/ContactPart" to "limit=20&contact[id]=$contactId&contact[objectName]=Contact",
                "/PartContact" to "limit=25",
                "/PartContact" to "limit=20&contact[id]=$contactId&contact[objectName]=Contact",
                "/PartPrice" to "limit=25",
                "/PartPrice" to "limit=20&contact[id]=$contactId&contact[objectName]=Contact",
                "/ContactPartPrice" to "limit=25",
                "/PartUnitPrice" to "limit=25",
                "/PartUnitPrice" to "limit=20&contact[id]=$contactId&contact[objectName]=Contact",
            )
            for ((path, q) in endpoints) {
                val (code, body) = getRawForTestWithCode(token, path, q)
                val status = when (code) {
                    200 -> "200 OK"
                    400 -> "400 (Model/Parameter)"
                    404 -> "404"
                    else -> "$code"
                }
                append("GET $path?$q → $status\n")
                if (code == 200) {
                    val snippet = body.take(DISCOVERY_SNIPPET_LEN)
                    append(if (body.length > DISCOVERY_SNIPPET_LEN) snippet + "\n… (gekürzt)\n" else snippet)
                    append("\n")
                } else if (body.contains("message")) {
                    try {
                        val start = body.indexOf("\"message\":")
                        val end = body.indexOf("\"", start + 12)
                        if (start >= 0 && end > start) append("  Message: ${body.substring(start + 11, end)}\n")
                    } catch (_: Exception) {}
                }
                append("\n")
            }

            append("——— GET /Contact/{id} mit verschiedenen embed (Artikel pro Kunde?) ———\n\n")
            if (contactId == "—") {
                append("Kein Kontakt gefunden, Embed-Tests übersprungen.\n")
                return@buildString
            }
            val embeds = listOf(
                "partUnitPrices",
                "partUnitPrice",
                "parts",
                "partPrices",
                "partList",
                "articles",
                "part",
                "contactParts",
                "partPrice",
                "partPrices",
                "customerParts",
                "prices"
            ).distinct()
            for (embed in embeds) {
                val (code, body) = getRawForTestWithCode(token, "/Contact/$contactId", "embed=addresses,$embed")
                val status = when (code) {
                    200 -> "200 OK"
                    400 -> "400"
                    404 -> "404"
                    else -> "$code"
                }
                append("GET /Contact/$contactId?embed=addresses,$embed → $status\n")
                if (code == 200) {
                    val snippet = body.take(DISCOVERY_SNIPPET_LEN)
                    append(if (body.length > DISCOVERY_SNIPPET_LEN) snippet + "\n… (gekürzt)\n" else snippet)
                    append("\n")
                } else if (body.contains("message")) {
                    try {
                        val start = body.indexOf("\"message\":")
                        val end = body.indexOf("\"", start + 12)
                        if (start >= 0 && end > start) append("  Message: ${body.substring(start + 11, end)}\n")
                    } catch (_: Exception) {}
                }
                append("\n")
            }
        }
    }
}

data class SevDeskContact(
    val id: String,
    val name: String,
    val adresse: String = "",
    val plz: String = "",
    val stadt: String = ""
)

data class SevDeskPart(
    val id: String,
    val name: String,
    val price: Double = 0.0,
    val unit: String = ""
)

data class SevDeskPartContactPrice(
    val contactId: String,
    val partId: String,
    val priceNet: Double = 0.0,
    val priceGross: Double = 0.0
)

class SevDeskApiException(message: String) : Exception(message)
