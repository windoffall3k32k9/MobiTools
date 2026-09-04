package com.example.baseproject.utils

import android.app.Dialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
object ViewEx {

    var WIDTH_SCREEN = 0
    var HEIGHT_SCREEN = 0

    fun ImageView.tintColor(color : Int){
        this.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
    fun ImageView.tintColorRes(colorRes : Int){
        this.setColorFilter(ContextCompat.getColor(this.context, colorRes), PorterDuff.Mode.SRC_IN)
    }
    fun TextView.setTextColorRes(colorRes : Int){
        this.setTextColor(ContextCompat.getColor(this.context, colorRes))
    }

    fun View.gone(){
        visibility = View.GONE
    }

    fun View.invisible(){
        visibility = View.INVISIBLE
    }

    fun View.visible(){
        visibility = View.VISIBLE
    }

//    fun Dialog.transparentDialog() {
//        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
//    }
}