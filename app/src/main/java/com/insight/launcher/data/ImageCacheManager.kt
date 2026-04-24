package com.insight.launcher.data

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.File

class ImageCacheManager(private val context: Context) {
    private val currentFile = File(context.filesDir, "current_bg.jpg")
    private val nextFile = File(context.filesDir, "next_bg.jpg")

    fun getCurrentFile(): File? = if (currentFile.exists()) currentFile else null
    fun getNextFile(): File? = if (nextFile.exists()) nextFile else null

    /**
     * Move a próxima imagem para a posição atual.
     * @return O novo arquivo atual, ou null se nenhuma próxima imagem estava disponível.
     */
    fun rotateImages(): File? {
        if (nextFile.exists()) {
            if (currentFile.exists()) currentFile.delete()
            if (nextFile.renameTo(currentFile)) {
                return currentFile
            }
        }
        return if (currentFile.exists()) currentFile else null
    }

    /**
     * Baixa uma nova imagem e salva como a próxima imagem.
     */
    fun prefetchNextImage(onComplete: (Boolean) -> Unit = {}) {
        val randomSeed = System.currentTimeMillis() + (0..1000).random()
        val imageUrl = "https://loremflickr.com/1440/3088/cars,luxury,supercar/all?random=$randomSeed"

        Thread {
            try {
                // Download para o cache do Glide primeiro (sem persistência de disco do Glide)
                val futureTarget = Glide.with(context)
                    .asFile()
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .submit()

                val file = futureTarget.get()
                file.copyTo(nextFile, overwrite = true)
                Log.d("ImageCacheManager", "Next image prefetched to ${nextFile.absolutePath}")
                onComplete(true)
            } catch (e: Exception) {
                Log.e("ImageCacheManager", "Error prefetching image", e)
                onComplete(false)
            }
        }.start()
    }
}
