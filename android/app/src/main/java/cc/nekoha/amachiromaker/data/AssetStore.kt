package cc.nekoha.amachiromaker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import cc.nekoha.amachiromaker.model.LayerPart
import cc.nekoha.amachiromaker.model.LayerSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Reads the bundled artwork straight out of the APK assets (fully offline, no CDN, no WebView).
 * Assets live under `image_maker/168503/<partId>/<i_|ii_|p_>*.png`.
 */
class AssetStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val partsSerializer = ListSerializer(LayerPart.serializer())
    private val selectionsSerializer = ListSerializer(LayerSelection.serializer())

    /** Upstream data uses "/app/image_maker/..." while the APK stores "image_maker/...". */
    fun assetPathFor(url: String): String {
        var p = url.trim()
        if (p.startsWith("/app/")) p = p.removePrefix("/app/")
        if (p.startsWith("app/")) p = p.removePrefix("app/")
        if (p.startsWith("/")) p = p.removePrefix("/")
        return p
    }

    private suspend fun readAssetText(path: String): String = withContext(Dispatchers.IO) {
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    suspend fun loadParts(): List<LayerPart> =
        json.decodeFromString(partsSerializer, readAssetText("data/orderedLayers.json"))

    suspend fun loadDefaultCombination(): List<LayerSelection> =
        json.decodeFromString(selectionsSerializer, readAssetText("data/defaultCombination.json"))

    fun encodeSelections(list: List<LayerSelection>): String =
        json.encodeToString(selectionsSerializer, list)

    fun decodeSelections(text: String): List<LayerSelection>? = try {
        json.decodeFromString(selectionsSerializer, text)
    } catch (t: Throwable) {
        null
    }

    /** Small LRU keyed by asset path — thumbnails are tiny, textures are 600x600 max. */
    private val bitmapCache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun decodeBitmap(url: String): Bitmap? {
        val path = assetPathFor(url)
        if (path.isEmpty()) return null
        bitmapCache.get(path)?.let { return it }
        return try {
            context.assets.open(path).use { stream ->
                val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val bmp = BitmapFactory.decodeStream(stream, null, opts) ?: return null
                bitmapCache.put(path, bmp)
                bmp
            }
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun decodeBitmapAsync(url: String): Bitmap? =
        withContext(Dispatchers.IO) { decodeBitmap(url) }

    fun clearMemoryCache() = bitmapCache.evictAll()
}
