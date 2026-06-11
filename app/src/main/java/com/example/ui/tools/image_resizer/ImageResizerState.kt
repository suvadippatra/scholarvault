package com.scholarvault.ui.tools.image_resizer

import android.graphics.Bitmap
import android.net.Uri

data class AdvancedImageItem(
    val uri: Uri,
    val origName: String,
    val origBitmap: Bitmap,
    var croppedBitmap: Bitmap? = null,
    var processedBitmap: Bitmap? = null,
    
    // Config
    var removeBackground: Boolean = false,
    var bgTolerance: Float = 10f,
    var bgColor: Int = android.graphics.Color.WHITE,
    
    var targetWidth: String = "",
    var targetHeight: String = "",
    var targetDpi: String = "300",
    var targetKB: String = "",
    var format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    var lockAspect: Boolean = true,
    var customName: String = origName,
    val origSizeKB: Long = 0,
    
    // Result
    var resultUri: Uri? = null,
    var resultSizeKB: Long = 0
)

enum class CustomBgColor(val colorName: String, val colorValue: Int) {
    WHITE("White", android.graphics.Color.WHITE),
    LIGHT_BLUE("Light Blue", android.graphics.Color.rgb(173, 216, 230)),
    TRANSPARENT("Transparent", android.graphics.Color.TRANSPARENT),
    CUSTOM("Custom", android.graphics.Color.MAGENTA);
}
