package com.example.we2026_5.data.repository

import com.example.we2026_5.wasch.Article
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class ArticleRepository(
    private val database: FirebaseDatabase
) {
    private val articlesRef: DatabaseReference = database.reference.child("articles")

    private val cacheLock = Any()
    private var cachedArticles: List<Article> = emptyList()
    private var lastLoadDay: Long = -1L

    private fun getTodayDayKey(): Long = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())

    /** Artikel-Liste maximal 1× pro Tag aus Firebase laden; sonst aus Cache. */
    suspend fun getArticlesWithDailyCache(): List<Article> = withContext(Dispatchers.IO) {
        val today = getTodayDayKey()
        synchronized(cacheLock) {
            if (lastLoadDay == today && cachedArticles.isNotEmpty()) return@withContext cachedArticles
        }
        val list = getAllArticles()
        synchronized(cacheLock) {
            cachedArticles = list
            lastLoadDay = today
        }
        list
    }

    /** Cache ungültig machen (z. B. nach Speichern/Import), damit beim nächsten Abruf neu geladen wird. */
    fun invalidateArticlesCache() {
        synchronized(cacheLock) { lastLoadDay = -1L }
    }

    /** Liefert die Artikel-Liste; lädt nur 1× pro Tag aus Firebase, sonst aus Cache. */
    fun getAllArticlesFlow(): Flow<List<Article>> = flow {
        emit(getArticlesWithDailyCache())
    }

    suspend fun getAllArticles(): List<Article> {
        val snapshot = articlesRef.get().await()
        val list = mutableListOf<Article>()
        snapshot.children.forEach { child ->
            val id = child.key ?: return@forEach
            parseArticle(child, id)?.let { list.add(it) }
        }
        return list.sortedBy { it.name }
    }

    private fun parseArticle(snapshot: DataSnapshot, id: String): Article? {
        val name = snapshot.child("name").getValue(String::class.java) ?: return null
        val preis = (snapshot.child("preis").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
        val einheit = snapshot.child("einheit").getValue(String::class.java) ?: ""
        val sevDeskId = snapshot.child("sevDeskId").getValue(String::class.java)
        return Article(id = id, name = name, preis = preis, einheit = einheit, sevDeskId = sevDeskId)
    }

    suspend fun saveArticle(article: Article): Boolean {
        return try {
            val key = if (article.id.isNotBlank()) article.id else articlesRef.push().key ?: return false
            val toSave = article.copy(id = key)
            withTimeout(2000) { articlesRef.child(key).setValue(mapOf(
                "name" to toSave.name,
                "preis" to toSave.preis,
                "einheit" to (toSave.einheit),
                "sevDeskId" to (toSave.sevDeskId ?: "")
            )).await() }
            invalidateArticlesCache()
            true
        } catch (e: Exception) {
            android.util.Log.e("ArticleRepository", "Save article failed", e)
            false
        }
    }

    suspend fun deleteArticle(articleId: String): Boolean {
        return try {
            withTimeout(2000) { articlesRef.child(articleId).removeValue().await() }
            invalidateArticlesCache()
            true
        } catch (e: Exception) {
            android.util.Log.e("ArticleRepository", "Delete article failed", e)
            false
        }
    }

    /** Löscht alle Artikel, die aus SevDesk importiert wurden (sevDeskId gesetzt). @return Anzahl gelöschter Artikel. */
    suspend fun deleteAllSevDeskArticles(): Int {
        val all = getAllArticles()
        val toDelete = all.filter { it.sevDeskId?.isNotBlank() == true }
        var deleted = 0
        for (a in toDelete) {
            if (deleteArticle(a.id)) deleted++
        }
        return deleted
    }
}
