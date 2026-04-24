package com.insight.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.ByteArrayOutputStream
import androidx.core.content.edit

class ImageCacheManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("image_cache_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT = "current_image_base64"
        private const val KEY_NEXT = "next_image_base64"
    }

    fun getCurrentBase64(): String? = prefs.getString(KEY_CURRENT, null)
    fun getNextBase64(): String? = prefs.getString(KEY_NEXT, null)

    /**
     * Move a próxima imagem para a posição atual e limpa a próxima.
     */
    fun rotateImages(): String? {
        val next = getNextBase64()
        if (next != null) {
            prefs.edit {
                putString(KEY_CURRENT, next)
                    .remove(KEY_NEXT)
            }
            return next
        }
        return getCurrentBase64()
    }

    /**
     * Baixa a imagem, converte para Base64 (WebP) e salva nas SharedPreferences.
     */
    fun prefetchNextImage(onComplete: (Boolean) -> Unit = {}) {
        val randomSeed = System.currentTimeMillis() + (0..1000).random()
        val imageUrl = "https://loremflickr.com/2160/3840/cars,luxury,supercar/all?random=$randomSeed"

        Thread {
            try {
                // Download do Bitmap via Glide
                val bitmap = Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .submit()
                    .get()

                // Compressão eficiente para WebP
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.WEBP, 75, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                prefs.edit { putString(KEY_NEXT, base64String) }
                Log.d("ImageCacheManager", "Next image saved to SharedPreferences (Base64)")
                onComplete(true)
            } catch (e: Exception) {
                Log.e("ImageCacheManager", "Error prefetching image to Base64", e)
                onComplete(false)
            }
        }.start()
    }
}
