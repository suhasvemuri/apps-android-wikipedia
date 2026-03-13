package org.wikipedia.navtab

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.core.view.updateMarginsRelative
import com.google.android.material.navigation.NavigationBarView
import org.wikipedia.R
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

internal object NavTabViewUtil {
    fun populateMenu(menu: Menu) {
        menu.clear()
        NavTab.entries.forEachIndexed { index, tab ->
            menu.add(Menu.NONE, tab.id, index, tab.text).setIcon(tab.icon)
        }
    }

    fun setOverlayDot(navigationBarView: NavigationBarView, context: Context, tab: NavTab, enabled: Boolean) {
        val itemView = navigationBarView.findViewById<ViewGroup>(tab.id) ?: return
        val imageView = itemView.findViewById<View?>(com.google.android.material.R.id.navigation_bar_item_icon_view)
        val imageParent = (imageView?.parent as? ViewGroup)?.parent as? ViewGroup
        var overlayDotView: ImageView? = itemView.findViewById(R.id.nav_tab_overlay_dot)
        if (overlayDotView == null && imageParent != null) {
            overlayDotView = ImageView(context)
            overlayDotView.id = R.id.nav_tab_overlay_dot
            val dotSize = DimenUtil.roundedDpToPx(6f)
            val params = FrameLayout.LayoutParams(dotSize, dotSize)
            params.gravity = Gravity.CENTER
            val margin = DimenUtil.roundedDpToPx(8f)
            params.updateMarginsRelative(start = margin, bottom = margin)
            overlayDotView.layoutParams = params
            overlayDotView.setBackgroundResource(R.drawable.shape_circle)
            overlayDotView.backgroundTintList = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.destructive_color))
            imageParent.addView(overlayDotView)
        }
        overlayDotView?.isVisible = enabled
    }
}
