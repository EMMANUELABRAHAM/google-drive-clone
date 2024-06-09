package com.agape.googledriveclone.utils

import android.content.Context
import android.widget.Toast

object AppUtils {
    fun String.showToast(context: Context) {
        Toast.makeText(context, this, Toast.LENGTH_SHORT).show()
    }
}
