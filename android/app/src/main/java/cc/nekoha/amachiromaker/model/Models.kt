package cc.nekoha.amachiromaker.model

import kotlinx.serialization.Serializable

/**
 * One selectable "part" (背景 / 顔 / 前髪 ...) as described by orderedLayers.json.
 * Field names mirror the upstream RE:Amachiromaker data format.
 */
@Serializable
data class LayerPart(
    val pId: Int = 0,
    val pType: Int = 0,
    val pNm: String = "",
    val isMenu: Int = 0,
    val thumbUrl: String = "",
    val colorCnt: Int = 0,
    val canMv: Int = 0,
    val cpId: String = "",
    val isRmv: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
    val angle: Int = 0,
    val isMX: Int = 0,
    val isMY: Int = 0,
    val isRsiz: Int = 0,
    val isRota: Int = 0,
    val lyrs: List<Int> = emptyList(),
    val items: List<LayerItem> = emptyList(),
    val colors: Map<String, String> = emptyMap()
)

@Serializable
data class LayerItem(
    val itmId: Int = 0,
    val thumbUrl: String = "",
    val typeId: Int = 0,
    val originals: List<LayerOriginal> = emptyList()
)

@Serializable
data class LayerOriginal(
    val cId: Int = 0,
    val url: String = ""
)

/** One slot of the current outfit: which texture is drawn where. */
@Serializable
data class LayerSelection(
    val url: String = "",
    val x: Int = 0,
    val y: Int = 0,
    val itmId: Int = 0,
    val cId: Int = 0
)
