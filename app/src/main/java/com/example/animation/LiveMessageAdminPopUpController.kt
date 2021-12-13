package com.example.animation

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.*
import android.widget.PopupMenu
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import java.lang.Exception
import java.lang.reflect.Field
import java.lang.reflect.Method

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setForceShowIcon(true)
            } else {
                forcePopupIconsForOldDevices(this)
            }
            show()
        }
    }

    private fun forcePopupIconsForOldDevices(popup: PopupMenu) {
        try {
            val fields: Array<Field> = popup.javaClass.declaredFields
            fields.forEach { field ->
                if (field.name == "mPopup") {
                    field.isAccessible = true
                    val menuPopupHelper: Any = field.get(popup) ?: return
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons: Method = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                    return
                }
            }
        } catch (e: Exception) {
            "${e.message}"
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