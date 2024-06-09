package com.agape.googledriveclone.model

import java.io.File

data class FileDetails(
    val file: File,
    var fileName: String,
    var type: String
)