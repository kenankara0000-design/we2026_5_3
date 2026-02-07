package com.example.we2026_5.sevdesk

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

/**
 * Einfacher REST-Client für SevDesk API.
 * Basis: https://my.sevdesk.de/api/v1
 * Auth: Authorization-Header mit API-Token.
 *
 * Nur Lesezugriff: Es werden ausschließlich GET-Requests ausgeführt.
 * Es werden keine Daten in SevDesk erstellt, geändert oder gelöscht.
 */
object SevDeskApi {

    /** SevDesk-Kategorie „Kunde“ (Customer) – nur diese Kontakte werden importiert. */
    private const val CATEGORY_ID_KUNDE = "3"

    private const val BASE_URL = "https://my.sevdesk.de/api/v1"

    /** Token in URL als Query-Parameter und im Header (SevDesk akzeptiert beides; manche Umgebungen verlangen den Header). */
    private fun urlWithToken(path: String, query: String, token: String): URL {
        val t = token.trim()
        val q = if (query.isNotEmpty()) "$query&" else ""
        val full = "$BASE_URL$path?${q}token=${java.net.URLEncoder.encode(t, "UTF-8")}"
        return URL(full)
    }

    /** Laut SevDesk-Doku: Authorization-Header mit dem API-Token als Wert (z. B. "Authorization": "b7794de..."). */
    private fun setTokenHeader(conn: HttpURLConnection, token: String) {
        val t = token.trim()
        if (t.isNotEmpty()) {
            conn.setRequestProperty("Authorization", t)
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
            val url = urlWithToken("/Contact", "depth=1&embed=addresses&$categoryFilter&limit=$limit&offset=$offset", token)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            setTokenHeader(conn, token)
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            val code = conn.responseCode
            if (code != 200) {
                val err = conn.errorStream?.use { Scanner(it).useDelimiter("\\A").next() } ?: "HTTP $code"
                throw SevDeskApiException("Kontakte: $err")
            }
            val json = conn.inputStream.use { Scanner(it).useDelimiter("\\A").next() }
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
            val url = urlWithToken("/Part", "embed=unity&limit=$limit&offset=$offset", token)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            setTokenHeader(conn, token)
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            val code = conn.responseCode
            if (code != 200) {
                val err = conn.errorStream?.use { Scanner(it).useDelimiter("\\A").next() } ?: "HTTP $code"
                throw SevDeskApiException("Artikel: $err")
            }
            val json = conn.inputStream.use { Scanner(it).useDelimiter("\\A").next() }
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

    // ---------- Nur zum Testen: Roh-Responses prüfen (kundenspezifische Preise/Artikel) ----------

    private const val TEST_RAW_MAX_LEN = 8000

    /**
     * Führt einen GET aus und gibt die Response-Body als String zurück (gekürzt).
     * Nur zum Testen, ob z. B. embed=partUnitPrices kundenspezifische Preise liefert.
     */
    fun getRawForTest(token: String, path: String, query: String): String {
        val url = urlWithToken(path, query, token)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        setTokenHeader(conn, token)
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        val code = conn.responseCode
        val body = if (code == 200) {
            conn.inputStream.use { Scanner(it).useDelimiter("\\A").next() }
        } else {
            val err = conn.errorStream?.use { Scanner(it).useDelimiter("\\A").next() } ?: ""
            "HTTP $code\n$err"
        }
        return if (body.length <= TEST_RAW_MAX_LEN) body
        else body.take(TEST_RAW_MAX_LEN) + "\n\n… (gekürzt, ${body.length} Zeichen gesamt)"
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

class SevDeskApiException(message: String) : Exception(message)
