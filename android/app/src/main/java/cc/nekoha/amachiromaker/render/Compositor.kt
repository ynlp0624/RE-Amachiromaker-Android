package cc.nekoha.amachiromaker.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
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

        for (layer in selections) {
            if (layer.url.isEmpty()) continue
            val bmp = loader(layer.url) ?: continue
            canvas.drawBitmap(bmp, layer.x.toFloat(), layer.y.toFloat(), paint)
        }
        return out
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
