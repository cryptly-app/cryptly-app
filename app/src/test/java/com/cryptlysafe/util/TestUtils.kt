package com.cryptlysafe.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

object TestUtils {
    fun createTestBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    }

    fun createTestImageFile(context: Context, name: String): File {
        val bitmap = createTestBitmap(1920, 1080)
        val file = File(context.filesDir, name)
        saveBitmapToFile(bitmap, file)
        return file
    }

    fun verifyImageCompression(file: File, maxWidth: Int, maxHeight: Int): Boolean {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth <= maxWidth && options.outHeight <= maxHeight
    }

    fun createTestFiles(directory: File, count: Int, prefix: String = "test"): List<File> {
        return (0 until count).map { index ->
            val file = File(directory, "${prefix}_$index.jpg")
            file.createNewFile()
            file.setLastModified(System.currentTimeMillis() - (index * 1000))
            file
        }
    }
} 