package com.agape.googledriveclone.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.agape.googledriveclone.model.FileDetails

object FileUtils {
    fun getPickFileIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
    }

    fun getFileDetails(uri: Uri, context: Context): FileDetails? {
        val fileName = getFileName(uri, context)
        val file = createTempFileFromUri(uri, context)
        val type = getFileType(uri, context)
        if (file != null && fileName != null && type != null) {
            return FileDetails(file, fileName, type)
        }
        return null
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri, context: Context): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor.use { it ->
                if (it != null && it.moveToFirst()) {
                    result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun createTempFileFromUri(uri: Uri, context: Context): java.io.File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = java.io.File(context.cacheDir, getFileName(uri, context) ?: "tempFile")
        tempFile.outputStream().use {
            inputStream.copyTo(it)
        }
        return tempFile
    }

    private fun getFileType(uri: Uri, context: Context): String? {
        return context.contentResolver.getType(uri)
    }
}
