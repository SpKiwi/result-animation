package com.example.animation

import android.annotation.SuppressLint
import android.graphics.Outline
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.example.animation.progress.ProgressLayout
import com.example.animation.stateful.button.PaymentButton
import com.google.android.material.textfield.TextInputLayout
import java.text.DecimalFormat
import java.util.*

class AnimationActivity : AppCompatActivity() {

    private val testView: View by lazy { findViewById<View>(R.id.testView) }
    private val testViewRestore: View by lazy { findViewById<View>(R.id.testViewRestore) }

    private val progressIndicator: ProgressLayout by lazy {
        findViewById<ProgressLayout>(R.id.progressIndicator)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.animation_activity)

        testView.setOnClickListener {
//            progressIndicator.alpha = 0.3f
            progressIndicator.startProgress(5_000, object : ProgressLayout.Callbacks {
                override fun onProgressEnd() {
                    Log.d("myLog", "PROGRESS END")
                }

                override fun onProgressCancel() {
                    Log.d("myLog", "PROGRESS CANCEL")
                }

                override fun onProgressStart() {
                    Log.d("myLog", "PROGRESS START")
                }
            })
        }
        progressIndicator.setOnClickListener {
            Log.d("myLog", "CLICK PROGRESSINDICATOR")
            progressIndicator.cancelProgress(false)
        }
        testViewRestore.setOnClickListener {
            progressIndicator.cancelProgress(false)
        }

        findViewById<TextView>(R.id.textToAnimate1).apply {
            text = SpannableString("*9672").apply {
                setSpan(android.text.style.ForegroundColorSpan(resources.getColor(R.color.purple_200)), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        findViewById<View>(R.id.outlineTest).apply {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    AppCompatResources.getDrawable(this@AnimationActivity, R.drawable.ic_check_mark)?.let { drawable ->
                        drawable.setTint(getColor(R.color.black))
                        this@AnimationActivity.findViewById<ImageView>(R.id.pidorViewImage).setImageDrawable(drawable)
                    }
                    val left = 0
                    val top = 0;
                    val right = view.width
                    val bottom = view.height
                    val cornerRadius = 16

                    // no corners
                    outline.setRoundRect(left, top, right, bottom, 0f)

//                     all corners
//                    outline.setRoundRect(left, top, right, bottom, cornerRadius.toFloat())

                    // top corners
//                    outline.setRoundRect(left, top, right, bottom+cornerRadius, cornerRadius.toFloat())

                    // bottom corners
//                    outline.setRoundRect(left, top - cornerRadius, right, bottom, cornerRadius.toFloat())

                    /* left corners
                    outline.setRoundRect(left, top, right + cornerRadius, bottom, cornerRadius.toFloat())*/

                    /* right corners
                    outline.setRoundRect(left - cornerRadius, top, right, bottom, cornerRadius.toFloat())*/

                    /* top left corner
                    outline.setRoundRect(left , top, right+ cornerRadius, bottom + cornerRadius, cornerRadius.toFloat())*/

                    /* top right corner
                    outline.setRoundRect(left - cornerRadius , top, right, bottom + cornerRadius, cornerRadius.toFloat())*/

                    /* bottom left corner
                    outline.setRoundRect(left, top - cornerRadius, right + cornerRadius, bottom, cornerRadius.toFloat())*/

                    /* bottom right corner
                    outline.setRoundRect(left - cornerRadius, top - cornerRadius, right, bottom, cornerRadius.toFloat())*/

                }
            }
            clipToOutline = true

        }

        findViewById<TextView>(R.id.outlineText).apply {
            setCreditsInfo(this@apply, CreditsInfoViewModel(1000, true))
        }

        findViewById<TextInputLayout>(R.id.textInputFirst).apply {
            error = "pidor"
        }

        // stateful button

        val paymentButton: PaymentButton = findViewById<PaymentButton>(R.id.stateful_button)

        findViewById<Button>(R.id.state_text).setOnClickListener {
            paymentButton.state = PaymentButton.State.TEXT
        }
        findViewById<Button>(R.id.state_progress).setOnClickListener {
            paymentButton.state = PaymentButton.State.PROGRESS
        }
        findViewById<Button>(R.id.state_result).setOnClickListener {
            paymentButton.state = PaymentButton.State.SUCCESS
        }

//        findViewById<TextInputEditText>(R.id.textInputFirstText).apply {
//            setError("pidor")
//        }
    }



}

class CreditsInfoViewModel(
    val credits: Int,
    val isVip: Boolean
)

fun setCreditsInfo(textView: TextView, creditsInfoViewModel: CreditsInfoViewModel) {
    if (creditsInfoViewModel.credits <= 0)
        return

    val coinFormatter = DecimalFormat().apply { groupingSize = 3 }
    val formattedCoins = coinFormatter.format(creditsInfoViewModel.credits)
    val imageSpan = ImageSpan(
        ContextCompat.getDrawable(textView.context, R.drawable.ic_close)!!.apply {
            setBounds(0, 0, 60, 60)
        }, ImageSpan.ALIGN_BASELINE
    )

    val spannableString: SpannableString = if (creditsInfoViewModel.isVip) {
        val formattedString = String.format(Locale.getDefault(), "+   %s", formattedCoins)
        SpannableString(formattedString).apply {
            setSpan(imageSpan, 2, 3, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    } else {
        val formattedString = String.format(Locale.getDefault(), "  %s", formattedCoins)
        SpannableString(formattedString).apply {
            setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
    textView.text = spannableString
}

/**
 * Used to guarantee a specific result in expressions
 **/
val <T> T.exhaustive: T
    get() = this