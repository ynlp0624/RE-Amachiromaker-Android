package cc.nekoha.amachiromaker.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** File plumbing: gallery export for PNG, generic write/read for SAF-backed files (PSD, presets). */
object Exports {

    /**
     * Saves a PNG. API 29+ goes to the shared album `Pictures/Amachiromaker`;
     * older devices fall back to the app's own picture folder (no runtime permission needed).
     */
    fun savePng(context: Context, bitmap: Bitmap, displayName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Amachiromaker")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore rejected the new image")
            try {
                resolver.openOutputStream(uri)?.use { os ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)) {
                        throw IllegalStateException("PNG encode failed")
                    }
                } ?: throw IllegalStateException("cannot open output stream")
            } finally {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return "Pictures/Amachiromaker/$displayName"
        }

        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Amachiromaker")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, displayName)
        file.outputStream().use { os ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)) {
                throw IllegalStateException("PNG encode failed")
            }
        }
        return file.absolutePath
    }

    fun writeBytes(context: Context, uri: Uri, bytes: ByteArray) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: throw IllegalStateException("cannot open $uri")
    }

    fun readText(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    } catch (t: Throwable) {
        null
    }
}
