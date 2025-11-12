package com.code4galaxy.googlemapsdemo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat


// Utility: convert vector drawable into BitmapDescriptor for custom marker
fun bitmapFromVector(context: Context, vectorResId: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(context, vectorResId)!!
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}