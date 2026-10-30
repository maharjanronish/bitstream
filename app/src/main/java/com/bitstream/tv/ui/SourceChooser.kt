package com.bitstream.tv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.bitstream.tv.R
import com.bitstream.tv.provider.EmbedProvider
import com.bitstream.tv.provider.ProviderPrefs
import com.bitstream.tv.provider.Providers

private fun Int.dp(context: android.content.Context): Int =
    (this * context.resources.displayMetrics.density + 0.5f).toInt()

/**
 * On-play "Choose source" overlay for TV. Shows one button per registered
 * embed provider — a provider added to [Providers] appears here automatically,
 * no UI code changes.
 *
 * Remote: ◀/▶ move · OK select · BACK cancel. The last used source is
 * pre-focused, so repeated plays are a single extra OK press.
 *
 * When only one provider is registered the overlay is skipped entirely and
 * that provider plays directly (keeps a single-source build frictionless).
 */
class SourceChooser private constructor(
    private val activity: ComponentActivity,
    private val root: View,
    private val onPick: (EmbedProvider) -> Unit,
) {

    companion object {

        /**
         * Shows the overlay over [activity]; returns it (call [dismiss] to
         * close programmatically), or `null` when only one provider exists
         * and playback was started directly.
         */
        fun show(activity: ComponentActivity, title: String, onPick: (EmbedProvider) -> Unit): SourceChooser? {
            val providers = Providers.all()
            if (providers.size == 1) {
                onPick(providers.first())
                return null
            }

            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val root = LayoutInflater.from(activity)
                .inflate(R.layout.view_source_chooser, content, false)
            root.findViewById<TextView>(R.id.chooserTitle).text = title

            val chooser = SourceChooser(activity, root, onPick)
            chooser.build(providers)
            content.addView(
                root,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            return chooser
        }
    }

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = dismiss()
    }
    private var dismissed = false
    private var previousFocus: View? = activity.currentFocus

    private fun build(providers: List<EmbedProvider>) {
        val row = root.findViewById<LinearLayout>(R.id.chooserRow)
        val lastId = ProviderPrefs.lastId(activity)
        var focusTarget: Button? = null

        providers.forEachIndexed { index, provider ->
            val button = Button(activity).apply {
                background = ContextCompat.getDrawable(context, R.drawable.bg_source_button)
                text = provider.name
                textSize = 15f
                isAllCaps = false
                setTextColor(ContextCompat.getColorStateList(context, R.color.text_button_color))
                minWidth = 150.dp(activity)
                minHeight = 48.dp(activity)
                setPadding(24.dp(activity), 0, 24.dp(activity), 0)
                isFocusableInTouchMode = false
                setOnClickListener {
                    ProviderPrefs.remember(activity, provider.id)
                    dismiss()
                    onPick(provider)
                }
                setOnFocusChangeListener { _, hasFocus ->
                    val scale = if (hasFocus) 1.07f else 1f
                    animate().scaleX(scale).scaleY(scale).setDuration(130).start()
                }
            }
            row.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { if (index > 0) marginStart = 12.dp(activity) },
            )
            if (focusTarget == null && (provider.id == lastId || index == providers.lastIndex)) {
                focusTarget = button
            }
        }

        // Focus the remembered source (fall back to the first button).
        root.post {
            (focusTarget ?: row.getChildAt(0))?.requestFocus()
        }

        activity.onBackPressedDispatcher.addCallback(activity, backCallback)
        backCallback.isEnabled = true
        root.setOnClickListener { dismiss() }
    }

    /** Closes the overlay and returns focus to where it was before. */
    fun dismiss() {
        if (dismissed) return
        dismissed = true
        backCallback.remove()
        (root.parent as? ViewGroup)?.removeView(root)
        previousFocus?.takeIf { it.isAttachedToWindow }?.requestFocus()
    }
}
