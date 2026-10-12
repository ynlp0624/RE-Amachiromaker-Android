package cc.nekoha.amachiromaker.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import cc.nekoha.amachiromaker.model.LayerSelection

/**
 * Flattens the selected layers into the 600x600 canvas used by the original site
 * (merge-images with per-layer x/y offsets, transparent background).
 */
object Compositor {

    const val CANVAS_SIZE = 600

    fun compose(
        selections: List<LayerSelection>,
        canvasSize: Int = CANVAS_SIZE,
        loader: (String) -> Bitmap?
    ): Bitmap {
        val out = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { isAntiAlias = true }

        // 按修正后的顺序绘制（项圈压在衣服下、追加发垂在衣服上）
        for (index in LayerOrder.orderFor(selections.size)) {
            val layer = selections.getOrNull(index) ?: continue
            if (layer.url.isEmpty()) continue
            val bmp = loader(layer.url) ?: continue
            canvas.drawBitmap(bmp, layer.x.toFloat(), layer.y.toFloat(), paint)
        }

        // 后发（後ろ髪）排在最前，它垂到胸前的那段会被衣服盖住 —— 看起来像"头发穿模"。
        // 这里把「后发 ∩ 衣服范围」补画一次压在衣服上，头发就会自然垂在衣服外面。
        applyBackHairOverCloth(canvas, selections, canvasSize, paint, loader)
        return out
    }

    /** 后发与衣服重叠的部分补画在衣服之上（见 compose 里的说明）。 */
    private fun applyBackHairOverCloth(
        canvas: Canvas,
        selections: List<LayerSelection>,
        canvasSize: Int,
        paint: Paint,
        loader: (String) -> Bitmap?
    ) {
        val back = selections.getOrNull(LayerOrder.BACK_HAIR)
        val cloth = selections.getOrNull(LayerOrder.CLOTH)
        if (back == null || cloth == null) return
        if (back.url.isEmpty() || cloth.url.isEmpty()) return

        val hair = loader(back.url) ?: return
        val clothBmp = loader(cloth.url) ?: return

        val overlay = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val overlayCanvas = Canvas(overlay)
        // 1) 先画出衣服的范围
        overlayCanvas.drawBitmap(clothBmp, cloth.x.toFloat(), cloth.y.toFloat(), paint)
        // 2) 只保留落在这块范围里的后发
        val xfer = Paint().apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        overlayCanvas.drawBitmap(hair, back.x.toFloat(), back.y.toFloat(), xfer)
        // 3) 盖到成品上
        canvas.drawBitmap(overlay, 0f, 0f, paint)
        overlay.recycle()
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
