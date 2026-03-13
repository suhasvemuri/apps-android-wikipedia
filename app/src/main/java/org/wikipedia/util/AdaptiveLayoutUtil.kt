package org.wikipedia.util

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.flow.Flow
import org.wikipedia.settings.Prefs

object AdaptiveLayoutUtil {
    private const val LARGE_SCREEN_WIDTH_DP = 600
    private const val LARGE_SCREEN_CONTENT_WIDTH_DP = 960
    private const val COMPACT_READING_WIDTH_DP = 760
    private const val WIDE_READING_WIDTH_DP = 960
    private const val PINNED_SIDE_PANEL_WIDTH_DP = 320

    data class FoldInfo(
        val bounds: Rect,
        val isSeparating: Boolean
    )

    fun isLargeScreen(context: Context): Boolean {
        return context.resources.configuration.screenWidthDp >= LARGE_SCREEN_WIDTH_DP
    }

    fun shouldUseAdaptivePanels(context: Context): Boolean {
        return isLargeScreen(context) && Prefs.isLargeScreenPanelsEnabled
    }

    fun shouldPinArticleContents(context: Context): Boolean {
        return shouldUseAdaptivePanels(context)
    }

    fun preferredReadingWidthDp(): Int {
        return if (Prefs.isWideReadingLayoutEnabled) {
            WIDE_READING_WIDTH_DP
        } else {
            COMPACT_READING_WIDTH_DP
        }
    }

    fun contentHorizontalMarginPx(activity: Activity, maxWidthDp: Int = LARGE_SCREEN_CONTENT_WIDTH_DP): Int {
        val windowWidth = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity).bounds.width()
        val maxWidthPx = DimenUtil.dpToPx(maxWidthDp.toFloat()).toInt()
        return ((windowWidth - maxWidthPx) / 2).coerceAtLeast(0)
    }

    fun pinnedSidePanelWidthPx(): Int {
        return DimenUtil.dpToPx(PINNED_SIDE_PANEL_WIDTH_DP.toFloat()).toInt()
    }

    fun applyMaxWidth(view: View, maxWidthDp: Int) {
        view.doOnLayout {
            val activity = view.context as? Activity ?: return@doOnLayout
            val margin = contentHorizontalMarginPx(activity, maxWidthDp)
            view.updateLayoutParams<MarginLayoutParams> {
                leftMargin = margin
                rightMargin = margin
            }
        }
    }

    fun pagePaneMargins(activity: Activity, foldInfo: FoldInfo?): Pair<Int, Int> {
        val defaultMargin = contentHorizontalMarginPx(activity, preferredReadingWidthDp())
        if (foldInfo == null || !foldInfo.isSeparating || foldInfo.bounds.height() <= foldInfo.bounds.width()) {
            return defaultMargin to defaultMargin
        }

        val windowBounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity).bounds
        val outerMargin = DimenUtil.dpToPx(24f).toInt()
        return if (ViewCompat.getLayoutDirection(activity.window.decorView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
            (windowBounds.width() - foldInfo.bounds.right + outerMargin) to outerMargin
        } else {
            outerMargin to (windowBounds.width() - foldInfo.bounds.left + outerMargin)
        }
    }

    fun windowLayoutInfoFlow(activity: Activity): Flow<WindowLayoutInfo> {
        return WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity)
    }

    fun extractVerticalFoldInfo(windowLayoutInfo: WindowLayoutInfo?): FoldInfo? {
        val feature = windowLayoutInfo?.displayFeatures
            ?.filterIsInstance<FoldingFeature>()
            ?.firstOrNull { it.bounds.height() > it.bounds.width() } ?: return null
        return FoldInfo(feature.bounds, feature.isSeparating)
    }
}
