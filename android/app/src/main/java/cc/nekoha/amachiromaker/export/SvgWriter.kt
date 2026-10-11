package cc.nekoha.amachiromaker.export

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 把当前搭配导出成 SVG。
 *
 * 每个部位是一个带名字的 `<g>`，里面用 `<image>` 嵌入该部位的贴图（base64 PNG），
 * 坐标与画布位置和 PNG / PSD 导出完全一致。这样文件可以被浏览器直接打开、
 * 也能丢进 Illustrator / Inkscape / Figma 里按图层继续改（换某一层、缩放到任意尺寸、
 * 替换成矢量素材都行）。
 */
class SvgLayer(
    val name: String,
    val left: Int,
    val top: Int,
    val bitmap: Bitmap
)

object SvgWriter {

    fun write(width: Int, height: Int, layers: List<SvgLayer>): ByteArray {
        val sb = StringBuilder(1 shl 21)
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
        sb.append("width=\"").append(width).append("\" height=\"").append(height)
        sb.append("\" viewBox=\"0 0 ").append(width).append(' ').append(height).append("\">\n")
        sb.append("  <title>RE:Amachiromaker output</title>\n")
        sb.append("  <desc>Each part is a separate group, embedded as base64 PNG in its original position.</desc>\n")

        layers.forEachIndexed { index, layer ->
            val bitmap = layer.bitmap
            val png = ByteArrayOutputStream().apply {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
            }.toByteArray()
            val base64 = Base64.encodeToString(png, Base64.NO_WRAP)

            sb.append("  <g id=\"part-").append(index).append("\">\n")
            sb.append("    <title>").append(escapeXml(layer.name.ifBlank { "layer-$index" })).append("</title>\n")
            sb.append("    <image x=\"").append(layer.left).append("\" y=\"").append(layer.top)
            sb.append("\" width=\"").append(bitmap.width).append("\" height=\"").append(bitmap.height)
            sb.append("\" preserveAspectRatio=\"none\" xlink:href=\"data:image/png;base64,")
            sb.append(base64).append("\"/>\n")
            sb.append("  </g>\n")
        }

        sb.append("</svg>\n")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
