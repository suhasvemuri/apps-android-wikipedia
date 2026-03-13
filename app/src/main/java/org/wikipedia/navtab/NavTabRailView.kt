package org.wikipedia.navtab

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.navigationrail.NavigationRailView

class NavTabRailView(context: Context, attrs: AttributeSet) : NavigationRailView(context, attrs) {
    init {
        NavTabViewUtil.populateMenu(menu)
    }

    fun setOverlayDot(tab: NavTab, enabled: Boolean) {
        NavTabViewUtil.setOverlayDot(this, context, tab, enabled)
    }
}
