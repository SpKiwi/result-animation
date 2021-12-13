package com.example.animation

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            showPopupMenu(it)
        }
    }

    private fun showPopupMenu(view: View) {
        val contextWrapper = ContextThemeWrapper(view.context, R.style.GlobalPopup)
        val popupMenu = PopupMenu(contextWrapper, view, Gravity.BOTTOM)
        popupMenu.inflate(R.menu.main_menu)
        popupMenu.show()
    }
}