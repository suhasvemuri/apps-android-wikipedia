package org.wikipedia.bridge

import android.app.Activity
import android.content.Context
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.serialization.Serializable
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.extensions.getStrings
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel
import org.wikipedia.settings.LeadImageStyle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.AdaptiveLayoutUtil
import org.wikipedia.util.DimenUtil
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.roundToInt

object JavaScriptActionHandler {

    fun setTopMargin(top: Int): String {
        return setMargins(top + 16, 48)
    }

    fun setMargins(top: Int, bottom: Int): String {
        return "pcs.c1.Page.setMargins({ top:'${top}px', bottom:'${bottom}px' })"
    }

    fun getTextSelection(): String {
        return "pcs.c1.InteractionHandling.getSelectionInfo()"
    }

    fun getOffsets(): String {
        return "pcs.c1.Sections.getOffsets(document.body);"
    }

    fun getSections(): String {
        return "pcs.c1.Page.getTableOfContents()"
    }

    fun getProtection(): String {
        return "pcs.c1.Page.getProtection()"
    }

    fun getRevision(): String {
        return "pcs.c1.Page.getRevision();"
    }

    fun expandCollapsedTables(expand: Boolean): String {
        return "pcs.c1.Page.expandOrCollapseTables($expand);" +
                "var hideableSections = document.getElementsByClassName('pcs-section-hideable-header'); " +
                "for (var i = 0; i < hideableSections.length; i++) { " +
                "  pcs.c1.Sections.setHidden(hideableSections[i].parentElement.getAttribute('data-mw-section-id'), ${!expand});" +
                "}"
    }

    fun scrollToFooter(context: Context): String {
        return "window.scrollTo(0, document.getElementById('pcs-footer-container-menu').offsetTop - ${DimenUtil.getNavigationBarHeight(context)});"
    }

    fun scrollToAnchor(anchorLink: String): String {
        val anchor = anchorLink.substringAfter('#')
        return "var el = document.getElementById('$anchor');" +
                "window.scrollTo(0, el.offsetTop - (screen.height / 2));" +
                "setTimeout(function(){ el.style.backgroundColor='#fc3';" +
                "    setTimeout(function(){ el.style.backgroundColor=null; }, 500);" +
                "}, 250);"
    }

    fun prepareToScrollTo(anchorLink: String, highlight: Boolean): String {
        return "pcs.c1.Page.prepareForScrollToAnchor(\"${anchorLink.replace("\"", "\\\"")}\", { highlight: $highlight } )"
    }

    fun removeHighlights(): String {
        return "pcs.c1.Page.removeHighlightsFromHighlightedElements()"
    }

    fun setUp(context: Context, title: PageTitle, isPreview: Boolean, toolbarMargin: Int, messageCardHeight: Int): String {
        val app = WikipediaApp.instance
        val topActionBarHeight = if (isPreview) 0 else DimenUtil.roundedPxToDp(toolbarMargin.toFloat())
        val res = context.getStrings(title, intArrayOf(R.string.description_edit_add_description,
                R.string.table_infobox, R.string.table_other, R.string.table_close))
        var leadImageHeight = if (isPreview) 0 else
            (if (DimenUtil.isLandscape(context) || !Prefs.isImageDownloadEnabled) 0 else (DimenUtil.leadImageHeightForDevice(context) / DimenUtil.densityScalar).roundToInt() - topActionBarHeight)
        leadImageHeight = leadImageHeight + messageCardHeight
        val topMargin = topActionBarHeight + 16

        var fontFamily = Prefs.fontFamily
        if (fontFamily == context.getString(R.string.font_family_serif)) {
            fontFamily = "'Linux Libertine',Georgia,Times,serif"
        }

        return String.format(Locale.ROOT, "{" +
                "   \"platform\": \"android\"," +
                "   \"clientVersion\": \"${BuildConfig.VERSION_NAME}\"," +
                "   \"l10n\": {" +
                "       \"addTitleDescription\": \"${res[R.string.description_edit_add_description]}\"," +
                "       \"tableInfobox\": \"${res[R.string.table_infobox]}\"," +
                "       \"tableOther\": \"${res[R.string.table_other]}\"," +
                "       \"tableClose\": \"${res[R.string.table_close]}\"" +
                "   }," +
                "   \"theme\": \"${app.currentTheme.tag}\"," +
                "   \"bodyFont\": \"$fontFamily\"," +
                "   \"dimImages\": ${(app.currentTheme.isDark && Prefs.dimDarkModeImages)}," +
                "   \"margins\": { \"top\": \"%dpx\", \"bottom\": \"%dpx\" }," +
                "   \"leadImageHeight\": \"%dpx\"," +
                "   \"areTablesInitiallyExpanded\": ${isPreview || !Prefs.isCollapseTablesEnabled}," +
                "   \"textSizeAdjustmentPercentage\": \"100%%\"," +
                "   \"loadImages\": ${Prefs.isImageDownloadEnabled}," +
                "   \"userGroups\": ${JsonUtil.encodeToString(AccountUtil.groups)}," +
                "   \"isEditable\": ${!Prefs.readingFocusModeEnabled}" +
                "}", topMargin, 48, leadImageHeight)
    }

    fun setUpEditButtons(isEditable: Boolean, isProtected: Boolean): String {
        return "pcs.c1.Page.setEditButtons($isEditable, $isProtected)"
    }

    fun setFooter(model: PageViewModel): String {
        if (model.page == null) {
            return ""
        }
        val showTalkLink = model.page!!.title.namespace() !== Namespace.TALK
        val showMapLink = model.page!!.pageProperties.geo != null
        val editedDaysAgo = TimeUnit.MILLISECONDS.toDays(Date().time - model.page!!.pageProperties.lastModified.time)
        val langCode = model.title?.wikiSite?.languageCode ?: WikipediaApp.instance.appOrSystemLanguageCode

        // TODO: page-library also supports showing disambiguation ("similar pages") links and
        // "page issues". We should be mindful that they exist, even if we don't want them for now.
        return "pcs.c1.Footer.add({" +
                "   platform: \"android\"," +
                "   clientVersion: \"${BuildConfig.VERSION_NAME}\"," +
                "   menu: {" +
                "       items: [" +
                                "pcs.c1.Footer.MenuItemType.lastEdited, " +
                                (if (showTalkLink) "pcs.c1.Footer.MenuItemType.talkPage, " else "") +
                                (if (showMapLink) "pcs.c1.Footer.MenuItemType.coordinate, " else "") +
                                "pcs.c1.Footer.MenuItemType.pageIssues, " +
                "               pcs.c1.Footer.MenuItemType.referenceList " +
                "              ]," +
                "       fragment: \"pcs-menu\"," +
                "       editedDaysAgo: $editedDaysAgo" +
                "   }," +
                "   readMore: { " +
                "       itemCount: 3," +
                "       readMoreLazy: true," +
                "       langCode: \"$langCode\"," +
                "       fragment: \"pcs-read-more\"" +
                "   }" +
                "})"
    }

    fun appendReadMode(model: PageViewModel): String {
        if (model.page == null) {
            return ""
        }
        val apiBaseURL = model.title?.wikiSite!!.scheme() + "://" + model.title?.wikiSite!!.uri.authority!!.trimEnd('/')
        val langCode = model.title?.wikiSite?.languageCode ?: WikipediaApp.instance.appOrSystemLanguageCode
        return "pcs.c1.Footer.appendReadMore({" +
                "   platform: \"android\"," +
                "   clientVersion: \"${BuildConfig.VERSION_NAME}\"," +
                "   readMore: { " +
                "       itemCount: 3," +
                "       apiBaseURL: \"$apiBaseURL\"," +
                "       langCode: \"$langCode\"," +
                "       fragment: \"pcs-read-more\"" +
                "   }" +
                "})"
    }

    fun mobileWebChromeShim(marginTop: Int, marginBottom: Int): String {
        return "(function() {" +
                "let style = document.createElement('style');" +
                "style.innerHTML = '.header-chrome { visibility: hidden; margin-top: ${marginTop}px; height: 0px; } #page-secondary-actions { display: none; } .mw-footer { padding-bottom: ${marginBottom}px; } .page-actions-menu { display: none; } .minerva__tab-container { display: none; } .banner-container { display: none; }';" +
                "document.head.appendChild(style);" +
                "})();"
    }

    fun mobileWebSetDarkMode(): String {
        return "(function() {" +
                "document.documentElement.classList.add('skin-theme-clientpref-night');" +
                "})();"
    }

    fun applyAdaptiveArticleStyle(context: Context): String {
        val activity = context as? Activity ?: return ""
        val readingWidthPx = AdaptiveLayoutUtil.preferredReadingWidthPx(activity)
        val windowWidthPx = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity).bounds.width()
        val mediaWidthPx = min(
            windowWidthPx - DimenUtil.roundedDpToPx(48f),
            when (LeadImageStyle.fromPrefValue(Prefs.leadImageStyle)) {
                LeadImageStyle.HERO -> (readingWidthPx * 1.08f).roundToInt()
                LeadImageStyle.EDITORIAL -> (readingWidthPx * 0.86f).roundToInt()
                LeadImageStyle.COMPACT -> (readingWidthPx * 0.72f).roundToInt()
            }
        ).coerceAtLeast(DimenUtil.roundedDpToPx(320f))
        val captionWidthPx = (mediaWidthPx * 0.92f).roundToInt()
        val floatedTableWidthPx = min(
            windowWidthPx - DimenUtil.roundedDpToPx(64f),
            (readingWidthPx * 0.78f).roundToInt()
        ).coerceAtLeast(DimenUtil.roundedDpToPx(280f))
        val css = if (AdaptiveLayoutUtil.isLargeScreen(context)) {
            String.format(Locale.ROOT, """
                @media (min-width: 900px) {
                  body {
                    line-height: 1.72 !important;
                    text-rendering: optimizeLegibility;
                  }
                  body p,
                  body ul,
                  body ol,
                  body dl,
                  body blockquote {
                    max-width: min(100%%, %4${'$'}dpx) !important;
                    margin-left: auto !important;
                    margin-right: auto !important;
                  }
                  body p,
                  body ul,
                  body ol,
                  body dl {
                    margin-top: 0.78em !important;
                    margin-bottom: 0.92em !important;
                  }
                  body h1,
                  body h2,
                  body h3,
                  body h4,
                  body h5,
                  body h6 {
                    max-width: min(100%%, %4${'$'}dpx) !important;
                    margin: 1.4em auto 0.52em !important;
                    line-height: 1.18 !important;
                    letter-spacing: -0.01em;
                  }
                  body blockquote {
                    padding: 0.25em 1.1em !important;
                    border-left: 3px solid rgba(127, 127, 127, 0.35) !important;
                    opacity: 0.92;
                  }
                  body figure,
                  body .thumb,
                  body .gallery,
                  body .tsingle {
                    max-width: min(100%%, %1${'$'}dpx) !important;
                    margin-left: auto !important;
                    margin-right: auto !important;
                  }
                  body figure img,
                  body .thumb img,
                  body .gallerybox img,
                  body .tsingle img {
                    display: block !important;
                    max-width: 100%% !important;
                    height: auto !important;
                    margin-left: auto !important;
                    margin-right: auto !important;
                    border-radius: 12px;
                  }
                  body figcaption,
                  body .thumbcaption,
                  body .gallerytext {
                    max-width: min(100%%, %2${'$'}dpx) !important;
                    margin: 10px auto 0 !important;
                    line-height: 1.48 !important;
                    font-size: 0.94em !important;
                    opacity: 0.84;
                  }
                  body .android-adaptive-table-scroll {
                    overflow-x: auto !important;
                    overflow-y: hidden !important;
                    margin: 24px 0 !important;
                    padding: 0 0 4px !important;
                    -webkit-overflow-scrolling: touch;
                  }
                  body .android-adaptive-table-scroll > table {
                    margin: 0 !important;
                  }
                  body .android-adaptive-table-scroll::-webkit-scrollbar {
                    height: 8px;
                  }
                  body .android-adaptive-table-scroll::-webkit-scrollbar-thumb {
                    background: rgba(127, 127, 127, 0.36);
                    border-radius: 999px;
                  }
                  body table.infobox,
                  body table.sidebar {
                    max-width: min(100%%, %3${'$'}dpx) !important;
                  }
                  body table.infobox,
                  body table.sidebar,
                  body .android-adaptive-table-scroll table {
                    border-collapse: separate !important;
                    border-spacing: 0 !important;
                    border-radius: 12px;
                    overflow: hidden;
                  }
                }
            """.trimIndent(), mediaWidthPx, captionWidthPx, floatedTableWidthPx, readingWidthPx)
        } else {
            ""
        }

        return String.format(Locale.ROOT, """
            (function() {
              const styleId = 'android-adaptive-article-style';
              let style = document.getElementById(styleId);
              if (!style) {
                style = document.createElement('style');
                style.id = styleId;
                document.head.appendChild(style);
              }
              style.textContent = `%1${'$'}s`;

              document.querySelectorAll('table').forEach(function(table) {
                if (table.closest('.navbox') || table.closest('.metadata')) {
                  return;
                }
                let wrapper = table.parentElement && table.parentElement.classList.contains('android-adaptive-table-scroll')
                  ? table.parentElement
                  : null;
                if (!wrapper) {
                  wrapper = document.createElement('div');
                  wrapper.className = 'android-adaptive-table-scroll';
                  table.parentNode.insertBefore(wrapper, table);
                  wrapper.appendChild(table);
                }
              });
            })();
        """.trimIndent(), css.replace("`", "\\`"))
    }

    fun getElementAtPosition(x: Int, y: Int): String {
        return "(function() {" +
                "  let element = document.elementFromPoint($x, $y);" +
                "  let result = {};" +
                "  result.left = element.getBoundingClientRect().left;" +
                "  result.top = element.getBoundingClientRect().top;" +
                "  result.width = element.clientWidth;" +
                "  result.height = element.clientHeight;" +
                "  result.src = element.src;" +
                "  return result;" +
                "})();"
    }

    fun pauseAllMedia(): String {
        return "(function() {" +
                "var elements = document.getElementsByTagName('audio');" +
                "for(i=0; i<elements.length; i++) elements[i].pause();" +
                "})();"
    }

    @Serializable
    class ImageHitInfo(val left: Float = 0f, val top: Float = 0f, val width: Float = 0f, val height: Float = 0f,
                       val src: String = "")
}
