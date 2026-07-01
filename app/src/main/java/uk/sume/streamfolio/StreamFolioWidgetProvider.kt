package uk.sume.streamfolio

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.sume.streamfolio.data.local.AppDatabase

class StreamFolioWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val isRefresh = intent.getBooleanExtra("is_refresh", false)
            if (isRefresh) {
                Toast.makeText(context, "Refreshing StreamFolio widget...", Toast.LENGTH_SHORT).show()
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (appWidgetIds != null) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    for (id in appWidgetIds) {
                        appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
                    }
                }
            }
        }
    }

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
        val sharedPrefs = context.getSharedPreferences("StreamFolioWidgetPrefs", Context.MODE_PRIVATE)
        val format = sharedPrefs.getString("widget_format_$appWidgetId", "spotlight") ?: "spotlight"
        val category = sharedPrefs.getString("widget_category_$appWidgetId", "all") ?: "all"

        // Setup common refresh pending intent
        val refreshIntent = Intent(context, StreamFolioWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            putExtra("is_refresh", true)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId + 10000,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (format == "list") {
            val views = RemoteViews(context.packageName, R.layout.streamfolio_widget_list)
            
            // Set category display label
            val displayCat = when (category) {
                "all" -> "All Categories"
                "bookmarks" -> "Saved Bookmarks"
                else -> category
            }
            views.setTextViewText(R.id.widget_category, displayCat)

            // Setup remote service adapter
            val serviceIntent = Intent(context, StreamFolioWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty_view)

            // Setup pending intent template for list item clicks
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntent)

            // Bind refresh click
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        } else {
            val views = RemoteViews(context.packageName, R.layout.streamfolio_widget)
            
            // Bind refresh click
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    
                    // Fetch based on category configuration
                    var article = when (category) {
                        "bookmarks" -> db.articleDao().getBookmarkedArticlesSync().firstOrNull()
                        "all" -> db.articleDao().getLatestArticlesSync().firstOrNull()
                        else -> db.articleDao().getArticlesByCategorySync(category).firstOrNull()
                    }
                    var isBookmark = category == "bookmarks"

                    // If configured feed is empty, fallback gracefully
                    if (article == null) {
                        article = db.articleDao().getBookmarkedArticlesSync().firstOrNull()
                        isBookmark = true
                    }
                    if (article == null) {
                        article = db.articleDao().getLatestArticlesSync().firstOrNull()
                        isBookmark = false
                    }

                    if (article != null) {
                        views.setTextViewText(R.id.widget_title, article.title)
                        views.setTextViewText(R.id.widget_source, article.sourceName)
                        views.setTextViewText(
                            R.id.widget_category,
                            if (isBookmark) "Saved • ${article.category}" else "${category.uppercase()} • ${article.category}"
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
}
