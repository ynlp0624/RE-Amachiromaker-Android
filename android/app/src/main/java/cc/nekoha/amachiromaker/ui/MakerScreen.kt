package cc.nekoha.amachiromaker.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.nekoha.amachiromaker.data.AssetStore
import cc.nekoha.amachiromaker.data.MakerState
import cc.nekoha.amachiromaker.export.Exports
import cc.nekoha.amachiromaker.export.PsdLayer
import cc.nekoha.amachiromaker.export.PsdWriter
import cc.nekoha.amachiromaker.export.SvgLayer
import cc.nekoha.amachiromaker.export.SvgWriter
import cc.nekoha.amachiromaker.render.Compositor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun MakerScreen(
    state: MakerState,
    store: AssetStore,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    noticeAccepted: Boolean,
    onAcceptNotice: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    val gridColumns = if (landscape) 5 else 4

    var showNotice by remember { mutableStateOf(!noticeAccepted) }
    var showAbout by remember { mutableStateOf(false) }
    var busyText by remember { mutableStateOf<String?>(null) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val psdSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/vnd.adobe.photoshop")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busyText = "雫在打包 PSD 图层…"
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) { buildPsdBytes(state, store) }
                withContext(Dispatchers.IO) { Exports.writeBytes(context, uri, bytes) }
                toast("PSD 装好啦，图层都留着喵~ (${bytes.size / 1024 / 1024} MB)")
            } catch (t: Throwable) {
                toast("PSD 打包失败：${t.message}")
            } finally {
                busyText = null
            }
        }
    }

    val presetSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            Exports.writeBytes(context, uri, state.exportedPresetJson().toByteArray(Charsets.UTF_8))
            toast("预设导出好啦，别人也能载入这套搭配喵~")
        } catch (t: Throwable) {
            toast("预设导出失败：${t.message}")
        }
    }

    val presetLoader = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = Exports.readText(context, uri)
        if (text != null && state.importPreset(text)) {
            toast("预设载入成功喵~")
        } else {
            toast("预设文件无效，请选择正确的预设文件喵~")
        }
    }

    val svgSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/svg+xml")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busyText = "雫在打包 SVG 图层…"
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) { buildSvgBytes(state, store) }
                withContext(Dispatchers.IO) { Exports.writeBytes(context, uri, bytes) }
                toast("SVG 装好啦，每个部位都是一个图层喵~ (${bytes.size / 1024} KB)")
            } catch (t: Throwable) {
                toast("SVG 打包失败：${t.message}")
            } finally {
                busyText = null
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(scheme.background)
        ) {
            TopBar(
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                onAbout = { showAbout = true },
                onNotice = { showNotice = true }
            )

            val previewPane: @Composable (Modifier) -> Unit = { modifier ->
                PreviewPane(
                    state = state,
                    store = store,
                    modifier = modifier,
                    onReset = {
                        state.reset()
                        toast("已经回到默认的样子啦~")
                    },
                    onSavePng = {
                        val bitmap = state.preview
                        if (bitmap == null) {
                            toast("还在合成中，等一下下喵~")
                        } else {
                            busyText = "雫在保存 PNG…"
                            scope.launch {
                                try {
                                    val name = "amachiromaker-output-${System.currentTimeMillis()}.png"
                                    val where = withContext(Dispatchers.IO) {
                                        Exports.savePng(context, bitmap, name)
                                    }
                                    toast("保存好啦！(≧◡≦)♡ 位置：$where")
                                } catch (t: Throwable) {
                                    toast("保存失败：${t.message}")
                                } finally {
                                    busyText = null
                                }
                            }
                        }
                    },
                    onSavePsd = {
                        psdSaver.launch("amachiromaker-output-${System.currentTimeMillis()}.psd")
                    },
                    onSaveSvg = {
                        svgSaver.launch("amachiromaker-output-${System.currentTimeMillis()}.svg")
                    },
                    onLoadPreset = { presetLoader.launch(arrayOf("*/*")) },
                    onSavePreset = {
                        presetSaver.launch("amachiromaker-preset-${System.currentTimeMillis()}.json")
                    }
                )
            }

            val pickerPane: @Composable (Modifier) -> Unit = { modifier ->
                PickerPane(state = state, store = store, columns = gridColumns, modifier = modifier)
            }

            if (landscape) {
                Row(Modifier.weight(1f)) {
                    previewPane(Modifier.weight(0.95f).fillMaxHeight())
                    pickerPane(Modifier.weight(1.05f).fillMaxHeight())
                }
            } else {
                Column(Modifier.weight(1f)) {
                    previewPane(Modifier.weight(0.95f).fillMaxWidth())
                    pickerPane(Modifier.weight(1.15f).fillMaxWidth())
                }
            }
        }

        val busy = busyText
        if (busy != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(14.dp))
                    Text(busy, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (showNotice) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("使用须知") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            buildString {
                                append("本项目为 祈烨猫猫 基于 Charlie Chiangm 的原项目进行的二改。\n\n")
                                append("允许二改，但请在 GitHub 拉分支且标注本项目的地址呐！\n\n")
                                append("请勿盗图说自己画的或售卖呐！违者公开展示喵~\n\n")
                                append("图片（除缩略图）版权均为 甘城なつき 所有。\n\n")
                                append("请勿商用或二次售卖本项目呐！违者自己承担一切后果呐~\n\n")
                                append("原项目地址：github.com/NekoQiye/RE-Amachiromaker\n\n")
                                append("本 Android 版为离线原生移植，素材全部打包在安装包内。")
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onAcceptNotice()
                        showNotice = false
                    }) { Text("同意") }
                },
                dismissButton = {
                    TextButton(onClick = { showNotice = false }) { Text("不同意") }
                }
            )
        }

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text("关于本站") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            buildString {
                                append("RE:Amachiromaker\n")
                                append("复活被甘城猫猫下线的捏脸网站\n\n")
                                append("项目图片画师：甘城なつき\n")
                                append("缩略图 & 项目所有者：祈烨猫猫\n")
                                append("网站运营 & 维护：喵喵喵\n")
                                append("原项目作者：Charlie Chiangm\n\n")
                                append("Android 原生移植版（Kotlin + Compose，无 WebView 套壳）\n\n")
                                append("请勿商用本项目和本项目生成的图片！\n\n")
                                append("原项目仓库：github.com/NekoQiye/RE-Amachiromaker\n")
                                append("画师官网：amashiro.com")
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAbout = false }) { Text("关闭") }
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onAbout: () -> Unit,
    onNotice: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "RE:Amachiromaker",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Text(
                "捏脸工坊 · 原生离线版",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
        FlatButton(
            text = if (darkTheme) "☀ 日间" else "🌙 夜间",
            onClick = onToggleTheme
        )
        Spacer(Modifier.width(8.dp))
        FlatButton(text = "关于", onClick = onAbout)
        Spacer(Modifier.width(8.dp))
        FlatButton(text = "须知", onClick = onNotice)
    }
}

@Composable
private fun PreviewPane(
    state: MakerState,
    store: AssetStore,
    modifier: Modifier,
    onReset: () -> Unit,
    onSavePng: () -> Unit,
    onSavePsd: () -> Unit,
    onSaveSvg: () -> Unit,
    onLoadPreset: () -> Unit,
    onSavePreset: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.padding(12.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            val preview = state.preview
            if (preview != null) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "合成预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    // 预览偶尔会放大到容器尺寸，用高质量重采样，边缘更平滑
                    filterQuality = FilterQuality.High
                )
            } else {
                Text(
                    text = "雫正在把贴图一层层叠起来…🐾",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlatButton("重置", onReset, Modifier.weight(1f))
            FlatButton("保存 PNG", onSavePng, Modifier.weight(1f), prominent = true)
            FlatButton("保存 PSD", onSavePsd, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlatButton("保存 SVG", onSaveSvg, Modifier.weight(1f))
            FlatButton("载入预设", onLoadPreset, Modifier.weight(1f))
            FlatButton("导出预设", onSavePreset, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PickerPane(
    state: MakerState,
    store: AssetStore,
    columns: Int,
    modifier: Modifier
) {
    val index = state.selectedTab.coerceIn(0, max(0, state.parts.size - 1))
    val part = state.parts.getOrNull(index)

    Column(modifier) {
        PartTabs(
            parts = state.parts,
            store = store,
            selectedIndex = index,
            onSelect = { state.selectedTab = it }
        )

        if (part == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "素材还在搬进来喵…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val selection = state.selections.getOrNull(index) ?: cc.nekoha.amachiromaker.model.LayerSelection()
            ItemGrid(
                part = part,
                selection = selection,
                store = store,
                columns = columns,
                onPick = { state.selectItem(index, it) },
                onClear = { state.clearSlot(index) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )

            val originals = part.items
                .firstOrNull { it.itmId == selection.itmId }
                ?.originals
                .orEmpty()

            if (part.colors.size > 1 && selection.itmId != 0 && originals.size > 1) {
                ColourRow(
                    originals = originals,
                    colours = part.colors,
                    selectedCId = selection.cId,
                    onPick = { state.selectColour(index, it) }
                )
            }
        }
    }
}

/** 生成 SVG：每个部位一个带名字的图层组，顺序与原站一致（按 layerComb 数组顺序）。 */
private fun buildSvgBytes(state: MakerState, store: AssetStore): ByteArray {
    val layers = ArrayList<SvgLayer>()
    state.parts.forEachIndexed { index, part ->
        val selection = state.selections.getOrNull(index) ?: return@forEachIndexed
        if (selection.url.isEmpty()) return@forEachIndexed
        val bitmap = store.decodeBitmap(selection.url) ?: return@forEachIndexed
        val cropped = cropToCanvas(bitmap, selection.x, selection.y) ?: return@forEachIndexed
        layers.add(
            SvgLayer(
                name = part.pNm.ifBlank { "layer-$index" },
                left = cropped.left,
                top = cropped.top,
                bitmap = cropped.bitmap
            )
        )
    }
    return SvgWriter.write(Compositor.CANVAS_SIZE, Compositor.CANVAS_SIZE, layers)
}

/** Builds the layered PSD: one cropped layer per part, plus the flattened preview. */
private fun buildPsdBytes(state: MakerState, store: AssetStore): ByteArray {
    val layers = ArrayList<PsdLayer>()
    state.parts.forEachIndexed { index, part ->
        val selection = state.selections.getOrNull(index) ?: return@forEachIndexed
        if (selection.url.isEmpty()) return@forEachIndexed
        val bitmap = store.decodeBitmap(selection.url) ?: return@forEachIndexed
        val cropped = cropToCanvas(bitmap, selection.x, selection.y) ?: return@forEachIndexed
        layers.add(
            PsdLayer(
                name = part.pNm.ifBlank { "layer-$index" },
                left = cropped.left,
                top = cropped.top,
                bitmap = cropped.bitmap
            )
        )
    }
    val merged = state.preview
        ?: Compositor.compose(state.selections) { store.decodeBitmap(it) }
    return PsdWriter.write(Compositor.CANVAS_SIZE, Compositor.CANVAS_SIZE, layers, merged)
}

private class CroppedLayer(val bitmap: Bitmap, val left: Int, val top: Int)

/** Clips a texture to the 600x600 canvas so Photoshop never sees out-of-bounds layers. */
private fun cropToCanvas(bitmap: Bitmap, x: Int, y: Int): CroppedLayer? {
    val size = Compositor.CANVAS_SIZE
    val left = max(0, x)
    val top = max(0, y)
    val right = min(size, x + bitmap.width)
    val bottom = min(size, y + bitmap.height)
    if (right <= left || bottom <= top) return null
    val alreadyInside = left == x && top == y && right == x + bitmap.width && bottom == y + bitmap.height
    if (alreadyInside) return CroppedLayer(bitmap, left, top)
    val cropped = Bitmap.createBitmap(bitmap, left - x, top - y, right - left, bottom - top)
    return CroppedLayer(cropped, left, top)
}
