package cc.nekoha.amachiromaker.data

import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.nekoha.amachiromaker.model.LayerItem
import cc.nekoha.amachiromaker.model.LayerOriginal
import cc.nekoha.amachiromaker.model.LayerPart
import cc.nekoha.amachiromaker.model.LayerSelection
import cc.nekoha.amachiromaker.render.Compositor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val KEY_COMBINATION = "layerComb"
const val KEY_THEME = "themeMode"
const val KEY_COLLAR_UNDER = "collarUnderCloth"

/**
 * Holds the whole maker state: the 25 part definitions, the current combination and the
 * flattened 600x600 preview. Mirrors the original React app (localStorage -> SharedPreferences).
 */
class MakerState(
    private val store: AssetStore,
    private val prefs: SharedPreferences
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var previewJob: Job? = null

    var parts by mutableStateOf<List<LayerPart>>(emptyList())
        private set

    var selections by mutableStateOf<List<LayerSelection>>(emptyList())
        private set

    var preview by mutableStateOf<Bitmap?>(null)
        private set

    var isReady by mutableStateOf(false)
        private set

    var loadError by mutableStateOf<String?>(null)
        private set

    var selectedTab by mutableStateOf(0)

    /**
     * 「让衣服遮住项圈」：默认关闭 = 原站行为（项圈露在衣服外）。
     * 打开后项圈会画到衣服底下，穿高领衣服时就不会浮在领口外面。
     */
    var collarUnderCloth by mutableStateOf(false)
        private set

    fun applyCollarUnderCloth(value: Boolean) {
        if (collarUnderCloth == value) return
        collarUnderCloth = value
        prefs.edit().putBoolean(KEY_COLLAR_UNDER, value).apply()
        refreshPreview()
    }

    suspend fun bootstrap() {
        try {
            val loadedParts = store.loadParts()
            val defaults = store.loadDefaultCombination()

            val restored = prefs.getString(KEY_COMBINATION, null)
                ?.let { store.decodeSelections(it) }
                ?.takeIf { it.isNotEmpty() }

            parts = loadedParts
            selections = align(restored ?: defaults, loadedParts.size)
            collarUnderCloth = prefs.getBoolean(KEY_COLLAR_UNDER, false)
            isReady = true
            refreshPreview()
        } catch (t: Throwable) {
            // A broken preset must never white-screen the app (upstream bug fixed the same way).
            prefs.edit().remove(KEY_COMBINATION).apply()
            try {
                val loadedParts = store.loadParts()
                parts = loadedParts
                selections = align(store.loadDefaultCombination(), loadedParts.size)
                isReady = true
                refreshPreview()
            } catch (fatal: Throwable) {
                loadError = fatal.message ?: fatal.javaClass.simpleName
            }
        }
    }

    /** Guarantees one slot per part, keeping stored offsets when the slot already existed. */
    private fun align(list: List<LayerSelection>, size: Int): List<LayerSelection> {
        if (list.size == size) return list
        val out = ArrayList<LayerSelection>(size)
        for (i in 0 until size) out.add(list.getOrElse(i) { LayerSelection() })
        return out
    }

    private fun persist() {
        prefs.edit().putString(KEY_COMBINATION, store.encodeSelections(selections)).apply()
    }

    fun refreshPreview() {
        val snapshot = selections
        val collarUnder = collarUnderCloth
        previewJob?.cancel()
        previewJob = scope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                Compositor.compose(snapshot, Compositor.CANVAS_SIZE, collarUnder) { url -> store.decodeBitmap(url) }
            }
            preview = bitmap
        }
    }

    fun selectItem(index: Int, item: LayerItem) {
        val current = selections.getOrNull(index) ?: return
        val sameColour = item.originals.firstOrNull { it.cId == current.cId }
        val chosen = sameColour ?: item.originals.firstOrNull() ?: return
        applySelection(index, item.itmId, chosen)
    }

    fun selectColour(index: Int, original: LayerOriginal) {
        val current = selections.getOrNull(index) ?: return
        if (current.itmId == 0) return
        applySelection(index, current.itmId, original)
    }

    /** The "empty" tile — upstream keeps the slot but clears its texture. */
    fun clearSlot(index: Int) {
        val current = selections.getOrNull(index) ?: return
        update(index, current.copy(url = "", itmId = 0, cId = 0))
    }

    private fun applySelection(index: Int, itmId: Int, original: LayerOriginal) {
        val current = selections.getOrNull(index) ?: return
        update(index, current.copy(url = original.url, itmId = itmId, cId = original.cId))
    }

    private fun update(index: Int, value: LayerSelection) {
        val list = selections.toMutableList()
        list[index] = value
        selections = list
        persist()
        refreshPreview()
    }

    fun reset() {
        scope.launch {
            val defaults = store.loadDefaultCombination()
            selections = align(defaults, parts.size)
            persist()
            refreshPreview()
        }
    }

    fun exportedPresetJson(): String = store.encodeSelections(selections)

    /** Loads a preset produced by this app or by the web version. Returns false when unusable. */
    fun importPreset(json: String): Boolean {
        val parsed = store.decodeSelections(json) ?: return false
        if (parsed.isEmpty()) return false
        selections = align(parsed, parts.size)
        persist()
        refreshPreview()
        return true
    }

    /** Current colour for a slot, if the part defines a palette. */
    fun selectedColourHex(index: Int): String? {
        val part = parts.getOrNull(index) ?: return null
        val cId = selections.getOrNull(index)?.cId ?: return null
        if (cId == 0) return null
        return part.colors[cId.toString()]
    }
}
