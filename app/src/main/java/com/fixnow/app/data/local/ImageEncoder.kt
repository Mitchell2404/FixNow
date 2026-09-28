package com.fixnow.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Convierte la foto de la cámara en una miniatura cuadrada (256 x 256, JPEG) codificada en Base64.
 * Pesa alrededor de 20-40 KB, así que cabe sin problema en un documento de Firestore (límite: 1 MiB).
 * Se usa en lugar de Firebase Storage, que exige el plan de pago Blaze.
 */
@Singleton
class ImageEncoder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun toSquareBase64(uriString: String, targetSizePx: Int = DEFAULT_SIZE_PX): String {
        val uri = Uri.parse(uriString)
        val resolver = context.contentResolver

        // 1) Leer solo las dimensiones, para no cargar una foto de 12 MP completa en memoria.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("No se pudo leer la foto")

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetSizePx && bounds.outHeight / (sample * 2) >= targetSizePx) {
            sample *= 2
        }

        // 2) Cargar la foto ya reducida.
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("No se pudo leer la foto")

        // 3) Enderezar según la orientación que guardó la cámara (EXIF).
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val upright = if (degrees == 0f) decoded else Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true
        )

        // 4) Recortar al centro en cuadrado y escalar.
        val side = minOf(upright.width, upright.height)
        val square = Bitmap.createBitmap(upright, (upright.width - side) / 2, (upright.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, targetSizePx, targetSizePx, true)

        // 5) JPEG + Base64.
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private companion object {
        const val DEFAULT_SIZE_PX = 256
        const val JPEG_QUALITY = 80
    }
}
