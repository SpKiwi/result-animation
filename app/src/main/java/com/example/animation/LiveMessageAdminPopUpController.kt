package com.example.animation

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

class LiveMessageAdminPopUpController {

    @SuppressLint("RestrictedApi")
    fun showPopUp(view: View?) {
        if (view == null)
            return

        val contextWrapper = ContextThemeWrapper(view.context, R.style.GlobalPopup)
        PopupMenu(contextWrapper, view, Gravity.BOTTOM).run {
            inflate(R.menu.admin_popup_menu)
//            (menu as MenuBuilder).setOptionalIconsVisible(true)
//            menu.findItem(R.id.adminMenuMute)?.let {
//                it.actionView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//                    override fun onGlobalLayout() {
//                        println()
//                        it.actionView.viewTreeObserver.removeOnGlobalLayoutListener(this)
//                    }
//                })
//            }
//            setOnMenuItemClickListener {
//                println("")
//                true
//            }
            show()
        }
    }

    @SuppressLint("InflateParams")
    fun showPopUpWindow(view: View) {
        val context = view.context
        val window = PopupWindow(context)
        window.contentView = LayoutInflater.from(context).inflate(R.layout.testo, null)
        window.isOutsideTouchable = true
        window.isFocusable = true
//        val rp = ContextCompat.getDrawable(context, R.drawable.tango_menu)
//        window.setBackgroundDrawable(rp)
//        val dr = ColorDrawable(android.graphics.Color.CYAN)
//        window.setBackgroundDrawable(dr)
        window.showAsDropDown(view, 0, 0, Gravity.BOTTOM)

    }

}