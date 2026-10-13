package cc.nekoha.amachiromaker.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import cc.nekoha.amachiromaker.model.LayerSelection

/**
 * Flattens the selected layers into the 600x600 canvas used by the original site
 * (merge-images with per-layer x/y offsets, transparent background).
 *
 * 默认与原站 App.tsx 完全一致：按 layerComb 的数组顺序逐层叠加。
 * 唯一例外是用户主动打开「让衣服遮住项圈」——这时才把项圈挪到衣服之前。
 */
object Compositor {

    const val CANVAS_SIZE = 600

    /** orderedLayers 里「服」的下标。 */
    private const val CLOTH_INDEX = 16

    /** orderedLayers 里「チョーカー」的下标。 */
    private const val COLLAR_INDEX = 17

    fun compose(
        selections: List<LayerSelection>,
        canvasSize: Int = CANVAS_SIZE,
        collarUnderCloth: Boolean = false,
        loader: (String) -> Bitmap?
    ): Bitmap {
        val out = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { isAntiAlias = true }

        for (index in drawOrder(selections.size, collarUnderCloth)) {
            val layer = selections.getOrNull(index) ?: continue
            if (layer.url.isEmpty()) continue
            val bmp = loader(layer.url) ?: continue
            canvas.drawBitmap(bmp, layer.x.toFloat(), layer.y.toFloat(), paint)
        }
        return out
    }

    /**
     * 绘制顺序。默认就是 0..n-1（原站行为）；
     * 打开「让衣服遮住项圈」时把项圈移到衣服之前，领口就会盖住项圈。
     */
    private fun drawOrder(size: Int, collarUnderCloth: Boolean): List<Int> {
        val base = (0 until size).toList()
        if (!collarUnderCloth || size <= COLLAR_INDEX) return base
        val order = base.toMutableList()
        order.remove(COLLAR_INDEX)
        val clothPos = order.indexOf(CLOTH_INDEX)
        order.add(if (clothPos >= 0) clothPos else order.size, COLLAR_INDEX)
        return order
    }

    /** Un-premultiplied colours only: returns the flat bitmap with a light backdrop for previews. */
    fun withBackdrop(src: Bitmap, backdrop: Int): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(backdrop)
        c.drawBitmap(src, 0f, 0f, null)
        return out
    }
}
