package com.example.animation.recycler

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animation.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

class RecyclerActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recycler_activity)

        val adaptero = RecyclerAdapter()

        val recyclero = findViewById<RecyclerView>(R.id.recyclerActivityRecycler).apply {
            adapter = adaptero
            layoutManager = LinearLayoutManager(
                this@RecyclerActivity,
                LinearLayoutManager.VERTICAL,
                false
            )
        }

        val disposable = Observable.interval(1000, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                adaptero.addItem()
                recyclero.smoothScrollToPosition(adaptero.itemCount - 1)
            }
        compositeDisposable.add(disposable)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

}