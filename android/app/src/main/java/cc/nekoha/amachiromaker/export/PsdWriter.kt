package cc.nekoha.amachiromaker.export

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * A tiny, dependency-free Photoshop (.psd) writer.
 *
 * Produces a layered 8-bit RGB document: one layer per selected part (cropped to the texture
 * bounds, positioned with left/top exactly like the canvas offset) plus the flattened preview
 * as the merged image data. Layers stay editable in Photoshop / Krita / GIMP / Photopea.
 */
class PsdLayer(
    val name: String,
    val left: Int,
    val top: Int,
    val bitmap: Bitmap
)

object PsdWriter {

    private const val COMPRESSION_RAW = 0

    fun write(
        width: Int,
        height: Int,
        layers: List<PsdLayer>,
        merged: Bitmap
    ): ByteArray {
        val out = ByteArrayOutputStream(1 shl 22)

        // ---------- File header ----------
        out.writeAscii("8BPS")
        out.writeU16(1)      // version 1 = PSD
        out.writeZeros(6)    // reserved
        out.writeU16(3)      // channels in the merged image (R,G,B)
        out.writeI32(height)
        out.writeI32(width)
        out.writeU16(8)      // bits per channel
        out.writeU16(3)      // colour mode: RGB

        // ---------- Colour mode data (empty) ----------
        out.writeI32(0)

        // ---------- Image resources (empty) ----------
        out.writeI32(0)

        // ---------- Layer and mask information ----------
        val layerInfo = ByteArrayOutputStream(1 shl 22)
        val channelPayloads = ArrayList<ByteArray>()

        layerInfo.writeI16(layers.size)

        for (layer in layers) {
            val bmp = layer.bitmap
            val w = bmp.width
            val h = bmp.height
            val top = layer.top
            val left = layer.left
            val bottom = top + h
            val right = left + w

            layerInfo.writeI32(top)
            layerInfo.writeI32(left)
            layerInfo.writeI32(bottom)
            layerInfo.writeI32(right)

            val pixels = IntArray(w * h)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            val red = ByteArray(w * h)
            val green = ByteArray(w * h)
            val blue = ByteArray(w * h)
            val alpha = ByteArray(w * h)
            for (i in pixels.indices) {
                val c = pixels[i]
                alpha[i] = ((c ushr 24) and 0xFF).toByte()
                red[i] = ((c ushr 16) and 0xFF).toByte()
                green[i] = ((c ushr 8) and 0xFF).toByte()
                blue[i] = (c and 0xFF).toByte()
            }

            val channels = listOf(0 to red, 1 to green, 2 to blue, -1 to alpha)
            layerInfo.writeU16(channels.size)
            for ((id, data) in channels) {
                layerInfo.writeI16(id)
                // channel data length = 2 byte compression tag + raw bytes
                layerInfo.writeI32(2 + data.size)
                channelPayloads.add(data)
            }

            layerInfo.writeAscii("8BIM")
            layerInfo.writeAscii("norm")   // blend mode: normal
            layerInfo.writeU8(255)         // opacity
            layerInfo.writeU8(0)           // clipping: base
            layerInfo.writeU8(0)           // flags: visible
            layerInfo.writeU8(0)           // filler

            val extra = ByteArrayOutputStream()
            extra.writeI32(0)  // layer mask data
            extra.writeI32(0)  // layer blending ranges
            writePascalName(extra, layer.name)
            writeUnicodeName(extra, layer.name)
            val extraBytes = extra.toByteArray()
            layerInfo.writeI32(extraBytes.size)
            layerInfo.write(extraBytes)
        }

        // Channel image data, in the exact order the channel records were written.
        for (payload in channelPayloads) {
            layerInfo.writeU16(COMPRESSION_RAW)
            layerInfo.write(payload)
        }

        val layerInfoBytes = layerInfo.toByteArray()
        val layerAndMask = ByteArrayOutputStream(layerInfoBytes.size + 16)
        layerAndMask.writeI32(layerInfoBytes.size)
        layerAndMask.write(layerInfoBytes)
        layerAndMask.writeI32(0)  // global layer mask info: none

        val layerAndMaskBytes = layerAndMask.toByteArray()
        out.writeI32(layerAndMaskBytes.size)
        out.write(layerAndMaskBytes)

        // ---------- Merged image data ----------
        out.writeU16(COMPRESSION_RAW)
        val mergedPixels = IntArray(width * height)
        merged.getPixels(mergedPixels, 0, width, 0, 0, width, height)
        val r = ByteArray(width * height)
        val g = ByteArray(width * height)
        val b = ByteArray(width * height)
        for (i in mergedPixels.indices) {
            val c = mergedPixels[i]
            r[i] = ((c ushr 16) and 0xFF).toByte()
            g[i] = ((c ushr 8) and 0xFF).toByte()
            b[i] = (c and 0xFF).toByte()
        }
        out.write(r)
        out.write(g)
        out.write(b)

        return out.toByteArray()
    }

    /** Pascal string, padded so (length byte + text) is a multiple of 4. */
    private fun writePascalName(target: ByteArrayOutputStream, name: String) {
        var bytes = name.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > 255) bytes = bytes.copyOf(255)
        target.writeU8(bytes.size)
        target.write(bytes)
        val written = 1 + bytes.size
        val pad = (4 - (written % 4)) % 4
        if (pad > 0) target.writeZeros(pad)
    }

    /** 'luni' additional layer info block: proper UTF-16 layer names. */
    private fun writeUnicodeName(target: ByteArrayOutputStream, name: String) {
        val utf16 = name.toByteArray(StandardCharsets.UTF_16BE)
        val payload = ByteArrayOutputStream()
        payload.writeI32(utf16.size / 2)
        payload.write(utf16)
        val pad = (4 - (payload.size() % 4)) % 4
        if (pad > 0) payload.writeZeros(pad)
        val payloadBytes = payload.toByteArray()

        target.writeAscii("8BIM")
        target.writeAscii("luni")
        target.writeI32(payloadBytes.size)
        target.write(payloadBytes)
    }

    // ---- little-endian/big-endian helpers (PSD is big-endian) ----

    private fun ByteArrayOutputStream.writeAscii(s: String) = write(s.toByteArray(StandardCharsets.US_ASCII))
    private fun ByteArrayOutputStream.writeZeros(n: Int) { repeat(n) { write(0) } }
    private fun ByteArrayOutputStream.writeU8(v: Int) = write(v and 0xFF)

    private fun ByteArrayOutputStream.writeU16(v: Int) {
        write((v ushr 8) and 0xFF)
        write(v and 0xFF)
    }

    private fun ByteArrayOutputStream.writeI16(v: Int) {
        write((v ushr 8) and 0xFF)
        write(v and 0xFF)
    }

    private fun ByteArrayOutputStream.writeI32(v: Int) {
        write((v ushr 24) and 0xFF)
        write((v ushr 16) and 0xFF)
        write((v ushr 8) and 0xFF)
        write(v and 0xFF)
    }
}
