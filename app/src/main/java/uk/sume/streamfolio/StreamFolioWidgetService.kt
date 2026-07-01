package uk.sume.streamfolio

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlinx.coroutines.runBlocking
import uk.sume.streamfolio.data.local.AppDatabase
import uk.sume.streamfolio.data.model.Article

class StreamFolioWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return StreamFolioWidgetFactory(this.applicationContext, intent)
    }
}

class StreamFolioWidgetFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private var articles: List<Article> = emptyList()

    override fun onCreate() {
        // No-op
    }

    override fun onDataSetChanged() {
        val db = AppDatabase.getDatabase(context)
        val sharedPrefs = context.getSharedPreferences("StreamFolioWidgetPrefs", Context.MODE_PRIVATE)
        val category = sharedPrefs.getString("widget_category_$appWidgetId", "all") ?: "all"
        val limit = sharedPrefs.getInt("widget_limit_$appWidgetId", 10)

        runBlocking {
            try {
                val queryResult = when (category) {
                    "all" -> db.articleDao().getLatestArticlesSync()
                    "bookmarks" -> db.articleDao().getBookmarkedArticlesSync()
                    else -> db.articleDao().getArticlesByCategorySync(category)
                }
                articles = queryResult.take(limit)
            } catch (e: Exception) {
                e.printStackTrace()
                articles = emptyList()
            }
        }
    }

    override fun onDestroy() {
        articles = emptyList()
    }

    override fun getCount(): Int = articles.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= articles.size) {
            return RemoteViews(context.packageName, R.layout.streamfolio_widget_item)
        }
        val article = articles[position]
        val views = RemoteViews(context.packageName, R.layout.streamfolio_widget_item)
        views.setTextViewText(R.id.widget_item_title, article.title)
        views.setTextViewText(R.id.widget_item_source, article.sourceName)

        // Setup fillInIntent for ListView item click template
        val fillInIntent = Intent().apply {
            putExtra("article_url", article.link)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
