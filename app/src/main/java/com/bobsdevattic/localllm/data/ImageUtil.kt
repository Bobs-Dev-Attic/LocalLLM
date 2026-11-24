package com.bobsdevattic.localllm.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Imports, downscales, stores, encodes and decodes image attachments without any
 * external image library. Images are downscaled before storage and transmission
 * to bound request size and memory.
 */
object ImageUtil {
    private const val MAX_IMPORT_PX = 1024
    private const val JPEG_QUALITY = 85

    /** Copies [uri] into app storage as a downscaled JPEG; returns the file path or null. */
    fun importImage(context: Context, uri: Uri): String? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_IMPORT_PX)
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null
        val scaled = scaleToMax(decoded, MAX_IMPORT_PX)

        val dir = File(context.filesDir, "attachments").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        if (scaled != decoded) scaled.recycle()
        decoded.recycle()
        file.absolutePath
    }.getOrNull()

    /** Decodes a small bitmap for display at roughly [maxPx] on the longest side. */
    fun decodeThumbnail(path: String, maxPx: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxPx)
        }
        BitmapFactory.decodeFile(path, options)
    }.getOrNull()

    /** Returns a `data:` URL with the base64-encoded image for the OpenAI image_url field. */
    fun toDataUrl(path: String, mimeType: String = "image/jpeg"): String {
        val bytes = File(path).readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:$mimeType;base64,$base64"
    }

    fun delete(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    private fun scaleToMax(bitmap: Bitmap, maxPx: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxPx) return bitmap
        val ratio = maxPx.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun computeInSampleSize(width: Int, height: Int, maxPx: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxPx && h / 2 >= maxPx) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }
}
