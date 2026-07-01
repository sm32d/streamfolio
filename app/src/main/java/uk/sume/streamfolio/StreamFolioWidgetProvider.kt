package uk.sume.streamfolio

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.sume.streamfolio.data.local.AppDatabase

class StreamFolioWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.streamfolio_widget)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                // First try bookmarks
                var article = db.articleDao().getBookmarkedArticlesSync().firstOrNull()
                var isBookmark = true

                // Fallback to latest news
                if (article == null) {
                    article = db.articleDao().getLatestArticlesSync().firstOrNull()
                    isBookmark = false
                }

                if (article != null) {
                    views.setTextViewText(R.id.widget_title, article.title)
                    views.setTextViewText(R.id.widget_source, article.sourceName)
                    views.setTextViewText(
                        R.id.widget_category,
                        if (isBookmark) "Saved • ${article.category}" else "Latest • ${article.category}"
                    )

                    // Setup click intent to open specific article
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra("article_url", article.link)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                } else {
                    views.setTextViewText(R.id.widget_title, "No cached articles found.")
                    views.setTextViewText(R.id.widget_source, "Open StreamFolio to fetch news.")
                    views.setTextViewText(R.id.widget_category, "Welcome")

                    // Simple click intent to launch app main screen
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
