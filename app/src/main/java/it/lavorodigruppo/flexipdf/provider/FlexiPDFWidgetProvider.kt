package it.lavorodigruppo.flexipdf.provider

/** @file FlexiPDFWidgetProvider.kt
 * Il seguente file implementa la classe AppWidgetProvider, estendendo la classe Broadcast Receiver.
 * Vengono sovrascritti i seguenti metodi:
 *
 * onEnabled(context: Context?) viene chiamata quando la prima istanza della app viene creata; anche
 * se ne vengono istanziati più oggetti widget, la classe viene chiamata un'unica volta.
 *
 * onDisabled(context: Context?) viene chiamata quando l'ultimo widget dell'app in questione viene eliminata,
 * quindi si fa un clean up del lavoro fatto.
 *
 * onAppWidgetOptionsChanged() viene chiamata nella prima creazione del widget e ogni volta che si fa
 * un ridimensionamento dell' interfaccia.
 *
 * onUpdate() è una funzione richiamata in un intervallo definito negli attributi di AppWidgetInfo
 * (@see @file res/xml/app_flexipdf_widget_zflip.xml l'attributo updatePeriodMillis="86400000").
 * Inoltre, viene chiamata ogni volta che un altro widget viene aggiunto.
 *
 * onReceive() è la funzione che tiene conto degli aggiornamenti e, usando la funzione privata updateAppWidget,
 * si fanno gli aggiornamenti necessari. Nella dichiarazione della funzione updateAppWidget vengono
 * usate due altre funzioni ausiliarie: getCurrentlyOpenFileFromApp() che ripesca il nome del file aperto
 * e getShortenedFileName() che permette di visualizzare nel widget un nome più ridotto di caratteri.
 *
 * Per le funzioni ausiliarie specifiche necessarie per il funzionamento del widget leggere i commenti
 * anticipanti le funzioni stesse.
 */


import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.activities.MainActivity

class FlexiPDFWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        context ?: return
        Log.d(TAG, "Widget Provider is enabled")
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        context ?: return
        Log.d(TAG, "Widget Provider is disabled")
        val prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d(TAG, "Cleared SharedPreferences")
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        if (context != null && appWidgetManager != null) {

            val prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val isFileOpen = prefs.getBoolean(WIDGET_IS_FILE_OPEN, false)
            val openFileName = prefs.getString(WIDGET_OPEN_FILE_NAME, null)
            updateAppWidget(context, appWidgetManager, appWidgetId, isFileOpen, openFileName)
        }
        Log.d(TAG, "Widget ID $appWidgetId updated due to options change.")
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?,
    ) {

        context ?: return
        appWidgetManager ?: return
        appWidgetIds ?: return

        Log.d(TAG, "onUpdate called for IDs: ${appWidgetIds.joinToString()}")
        for (appWidgetId in appWidgetIds) {
            val prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val isFileOpen = prefs.getBoolean(WIDGET_IS_FILE_OPEN, false)
            val openFileName = prefs.getString(WIDGET_OPEN_FILE_NAME, null)
            updateAppWidget(context, appWidgetManager, appWidgetId, isFileOpen, openFileName)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "onReceive action: $action")

        if (ACTION_FILE_STATUS_CHANGED == action) {
                val isFileOpen = intent.getBooleanExtra(EXTRA_IS_FILE_OPEN_ON_CLOSED_ZFLIP, false)
                val openFileName = intent.getStringExtra(EXTRA_OPEN_FILE_NAME)
                val appWidgetManager = AppWidgetManager.getInstance(context)

                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    ?: appWidgetManager.getAppWidgetIds(
                        ComponentName(context, FlexiPDFWidgetProvider::class.java)
                    )

                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    Log.d(
                        TAG,
                        "ACTION_FILE_STATUS_CHANGED for IDs: ${appWidgetIds.joinToString()}, " +
                                "IsOpen: $isFileOpen, File: $openFileName"
                    )
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(
                            context,
                            appWidgetManager,
                            appWidgetId,
                            isFileOpen,
                            openFileName
                        )
                    }
                }
        }
    }


    /**
     * Qui si trova la dichiarazione esplicita di una funzione ausiliaria che fa l'aggiornamento
     * specifico del widget istanziato precedentemente.
     */
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        isFileOpen: Boolean,
        openFileName: String?
    ) {
        Log.d(TAG, "Updating widget ID $appWidgetId. Open file: $openFileName")

        val views = RemoteViews(context.packageName, R.layout.widget_zflip)

        if (isFileOpen) {
            views.setViewVisibility(R.id.widget_icon, View.VISIBLE)
            views.setViewVisibility(R.id.widget_standard_message, View.VISIBLE)
            val displayText = if (!openFileName.isNullOrEmpty()) {
                getShortenedFileName(openFileName, 20) // Shorten if necessary
            } else {
                context.getString(R.string.widget_title)
            }
            views.setTextViewText(R.id.widget_standard_message, displayText)

            views.setViewVisibility(R.id.widget_standard_message, View.GONE)
            views.setViewVisibility(R.id.different_message, View.GONE)
        } else {

            views.setViewVisibility(R.id.widget_icon, View.GONE)
            views.setViewVisibility(R.id.widget_standard_message, View.GONE)


            views.setViewVisibility(R.id.widget_standard_message, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_standard_message,
                context.getString(R.string.widget_title) // Your default title
            )

            views.setViewVisibility(R.id.different_message, View.VISIBLE)
            views.setTextViewText(
                R.id.different_message,
                context.getString(R.string.empty_widget)
            )
        }

        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent =
            PendingIntent.getActivity(context, appWidgetId, launchIntent, pendingIntentFlags)
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        appWidgetManager?.updateAppWidget(appWidgetId, views)
    }


    /**
     * Funzione ausiliaria che permette di mostrare un widget con non più di @param maxLength caratteri,
     * in modo da avere una visualizzazione al più concisa e leggibile.
     */
    private fun getShortenedFileName(fileName: String, maxLength: Int): String {
        if (fileName.length <= maxLength) {
            return fileName
        }
        return "${fileName.substring(0, maxLength)}..."
    }


    /**
     * Qua sotto è definito un companion object che contiene alcune chiavi e dei valori necessari per
     * la gestione delle preferenze della applicazione e dei broadcast. Si specificano anche
     * le azioni che si possono verificare con i broadcast.
     * Le prime sono necessarie per il broadcast, mentre le altre per le sharedPreferences.
     * Si definisce anche la funzione @fun notifyFileStatusChanged() che viene utilizzata per mettere
     * nel contesto giusto e con le corrette nomenclature tutte le funzionalità del widget.
     */
    companion object {

        const val ACTION_FILE_STATUS_CHANGED = "it.lavorodigruppo.flexipdf.ACTION_FILE_STATUS_CHANGED"
        const val EXTRA_IS_FILE_OPEN_ON_CLOSED_ZFLIP = "isFileOpenOnClosedZFlip"
        const val EXTRA_OPEN_FILE_NAME = "openFileName"

        const val WIDGET_PREFS_NAME = "FlexiPDFWidgetPrefs"
        const val WIDGET_IS_FILE_OPEN = "isThereAnOpenFile"
        const val WIDGET_OPEN_FILE_NAME = "openFileName"

        private const val TAG = "FlexiPDFWidget"

        fun notifyFileStatusChanged(context: Context, isFileOpen:Boolean, openFileName: String?) {
            val intent = Intent(context, FlexiPDFWidgetProvider::class.java).apply {
                action = ACTION_FILE_STATUS_CHANGED
                putExtra(EXTRA_IS_FILE_OPEN_ON_CLOSED_ZFLIP, isFileOpen)
                putExtra(EXTRA_OPEN_FILE_NAME, openFileName)
            }

            val componentName = ComponentName(context, FlexiPDFWidgetProvider::class.java)
            intent.component = componentName
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                context.sendBroadcast(intent)
                Log.d(
                    TAG,
                    "Sent broadcast to update widget. Open file: $openFileName, IDs: ${appWidgetIds.joinToString()}"
                )
            }
            else {
                Log.d(TAG, "No widget instances to update.")
            }
        }
    }
}