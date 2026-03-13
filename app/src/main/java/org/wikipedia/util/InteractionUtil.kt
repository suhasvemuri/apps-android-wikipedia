package org.wikipedia.util

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

object InteractionUtil {
    fun performSubtleHaptic(view: View?) {
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun installSoftPress(vararg views: View?) {
        views.forEach { view ->
            view ?: return@forEach
            view.setOnTouchListener { touchedView, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> animatePressed(touchedView, true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animatePressed(touchedView, false)
                }
                false
            }
        }
    }

    fun tuneRecyclerMotion(recyclerView: RecyclerView?) {
        recyclerView?.itemAnimator?.apply {
            addDuration = 160
            changeDuration = 120
            moveDuration = 140
            removeDuration = 100
        }
    }

    fun animatePanelVisibility(view: View?, visible: Boolean) {
        view ?: return
        if (visible) {
            if (!view.isVisible) {
                view.alpha = 0f
                view.translationY = DimenUtil.dpToPx(10f)
                view.isVisible = true
            }
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180)
                .start()
        } else if (view.isVisible) {
            view.animate()
                .alpha(0f)
                .translationY(DimenUtil.dpToPx(6f))
                .setDuration(140)
                .withEndAction {
                    view.isVisible = false
                    view.translationY = 0f
                }
                .start()
        }
    }

    private fun animatePressed(view: View, pressed: Boolean) {
        view.animate()
            .scaleX(if (pressed) 0.98f else 1f)
            .scaleY(if (pressed) 0.98f else 1f)
            .alpha(if (pressed) 0.92f else 1f)
            .setDuration(if (pressed) 80 else 140)
            .start()
    }
}
