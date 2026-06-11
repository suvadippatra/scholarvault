package com.scholarvault.ui.tools.pdf_nup

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff

class BitmapPool(val width: Int, val height: Int) {
    private var pooledBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(pooledBitmap)
    
    fun getBitmap(): Bitmap = pooledBitmap
    
    fun getCanvas(): Canvas = canvas
    
    fun clearBitmap() {
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC)
    }
    
    fun release() {
        pooledBitmap.recycle()
    }
}
