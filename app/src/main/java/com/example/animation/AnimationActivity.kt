package com.example.animation

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Log
import android.view.*
import android.view.animation.PathInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.get
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import com.example.animation.progress.ProgressLayout
import com.example.animation.segmented.control.SegmentedController
import com.example.animation.stateful.button.PaymentButton
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import java.lang.Exception
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

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
            val drawable = background as GradientDrawable
            drawable.setColor(Color.BLACK)
            drawable.setStroke(10, Color.CYAN)

            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    AppCompatResources.getDrawable(this@AnimationActivity, R.drawable.ic_check_mark)?.let { drawable ->
                        drawable.setTint(getColor(R.color.black))
                        this@AnimationActivity.findViewById<ImageView>(R.id.pidorViewImage).setImageDrawable(drawable)
                    }
                    val left = 0
                    val top = 0
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
//            TimeUnit.DAYS.toMillis(10)
            text = getDayString(Date().time + TimeUnit.DAYS.toMillis(40))
        }

        findViewById<TextInputLayout>(R.id.textInputFirst).apply {
            error = "pidor"
        }

        // stateful button

        val paymentButton: PaymentButton = findViewById<PaymentButton>(R.id.stateful_button)
        paymentButton.setOnClickListener {
//            LiveMessageAdminPopUpController().showPopUpWindow(paymentButton)
            LiveMessageAdminPopUpController().showPopUp(paymentButton)
//            showPopupMenu(paymentButton)
            true
        }

        /* TODO starting from this */

        val controller = SegmentedController(
            layout = findViewById(R.id.container),
            radioGroup = findViewById(R.id.radioGroupTest)
        ).apply {
            setOnCheckedChangeListener(null)
        }

        val parent = findViewById<View>(R.id.radioGroupTest)
//        increaseTouchArea(parent, 300.0)
        val dayo = findViewById<View>(R.id.radioGroupDay)
//        increaseSelfTouchArea(dayo, 150.0)
        val weeko = findViewById<View>(R.id.radioGroupWeek)
//        increaseSelfTouchArea(weeko, 150.0)
        val montho = findViewById<View>(R.id.radioGroupMonth)
        increaseTouchArea(montho, 350.0)



        val tabLayout = findViewById<TabLayout>(R.id.tabLayout).apply {

            val textView = TextView(this@AnimationActivity) // LayoutInflater.from(this@AnimationActivity).inflate(R.layout.tab_view, null, false)
            textView.text = "pidarook"
            textView.setPadding(0, 0, 0, 0)
            val newTab = newTab().setCustomView(textView)
            addTab(newTab)
            setSelectedTabIndicatorHeight(textView.height)
        }
    }

    fun increaseTouchArea(view: View, value: Double) {
        val pixels = value.toInt()
        view.apply {
            (parent as? View)?.post {
                val rect = Rect()
                getHitRect(rect)
                rect.apply {
                    top -= pixels
                    left -= pixels
                    bottom += pixels
                    right += pixels
                }
                touchDelegate = TouchDelegate(rect, this)
            }
        }
    }

    fun increaseSelfTouchArea(view: View, value: Double) {
        val pixels = value.toInt()
        view.apply {
            (parent as? View)?.post {
                val rect = Rect()
                getHitRect(rect)
                rect.apply {
                    top -= pixels
                    left -= pixels
                    bottom += pixels
                    right += pixels
                }
                (parent as View).touchDelegate = TouchDelegate(rect, this)
            }
        }
    }

    private fun showPopupMenu(view: View) {
        val contextWrapper = ContextThemeWrapper(view.context, R.style.GlobalPopup)
        val popupMenu = PopupMenu(contextWrapper, view, Gravity.BOTTOM)
        popupMenu.inflate(R.menu.admin_popup_menu)
        popupMenu.show()
    }

    fun getDayString(timestamp: Long): String {
        val locale = getCurrentLocale(this)
        val formatter = SimpleDateFormat("MMM dd, yyyy", locale)
        val currentYearFormatter = SimpleDateFormat("MMM dd", locale)

        val f = if (isCurrentYear(timestamp)) currentYearFormatter else formatter
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val result = f.format(calendar.time).replace(", ", ",\n").capitalize()
        return result
    }

    private fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            context.resources.configuration.locale
        }
    }

    private fun isCurrentYear(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentYear = Calendar.getInstance().run { get(Calendar.YEAR) }

        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.YEAR) == currentYear
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