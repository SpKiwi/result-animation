package com.example.animation.cursor

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animation.R
import com.example.animation.cursor.adapter.CursorAdapter
import com.example.animation.cursor.adapter.CursorItem

class CursorActivity : AppCompatActivity() {

    private val fm: FragmentManager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cursor_activity)

        val pg = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (pg) {
            formList()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 42)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        formList()
    }

    fun formList() {
        val itemList = getLocalImagePaths().map { CursorItem(it) }
        findViewById<RecyclerView>(R.id.imageRecycler).apply {
            layoutManager = GridLayoutManager(this@CursorActivity, 4)
            adapter = CursorAdapter().apply {
                elements = itemList
            }
        }

    }

    fun getLocalImagePaths(): List<Uri> {
        val result = mutableListOf<Uri>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)

        contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(0)
                    )
                )
            }
        }

        return result
    }

}