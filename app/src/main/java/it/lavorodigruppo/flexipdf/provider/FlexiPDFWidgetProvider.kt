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
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
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
            val openFileName = getCurrentlyOpenFileFromApp(context)
            updateAppWidget(context, appWidgetId, openFileName)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "onReceive action: $action")

        if (ACTION_FILE_STATUS_CHANGED == action) {
            val openFileName = intent.getStringExtra(EXTRA_OPEN_FILE_NAME)
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(
                    ComponentName(
                        context,
                        FlexiPDFWidgetProvider::class.java
                    )
                )

            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                Log.d(TAG,"ACTION_FILE_STATUS_CHANGED for IDs: ${appWidgetIds.joinToString()}, " +
                        "File: $openFileName")
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetId, openFileName)
                }
            } else {
                Log.d(TAG, "ACTION_FILE_STATUS_CHANGE received but no widget IDs found.")
            }
        } else {
            super.onReceive(context, intent)
        }
    }


    //Funzione che fa in modo che il layout del widget venga utilizzato
    private fun updateAppWidget(context: Context, appWidgetId: Int, openFileName: String?) {
        Log.d(TAG, "Updating widget ID $appWidgetId. Open file: $openFileName")

        val launchIntent = Intent(context, MainActivity::class.java)

        val views = RemoteViews(context.packageName, R.layout.widget_zflip)

        views.setTextViewText(
            R.id.widget_standard_message,
            context.getString(R.string.widget_title)
        )

        //Controlla se un file è aperto per cambiare il contenuto del messaggio mostrato
        if (!openFileName.isNullOrEmpty()) {
            views.setTextViewText(R.id.different_message, getShortenedFileName(openFileName, 20))
            views.setViewVisibility(R.id.different_message, View.VISIBLE)
        } else {
            views.setTextViewText(
                R.id.different_message,
                context.getString(R.string.empty_widget)
            )
        }

        //Fa in modo che si apra l'applicazione quando viene cliccato il widget
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent =
            PendingIntent.getActivity(context, appWidgetId, launchIntent, pendingIntentFlags)
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
    }


    //Due funzioni ausiliarie che permettono di ottimizzare la visualizzazione del widget
    private fun getCurrentlyOpenFileFromApp(context: Context): String? {
        val nameFile = context.getSharedPreferences("FlexiPrefs", Context.MODE_PRIVATE)
        return nameFile.getString("currentOpenFileName", null)
    }

    //Fa in modo che nel widget non compaiano nomi troppo lunghi di file
    private fun getShortenedFileName(fileName: String, maxLength: Int): String {
        if (fileName.length <= maxLength) {
            return fileName
        }
        return "${fileName.substring(0, maxLength)}..."
    }

    companion object {
        const val ACTION_FILE_STATUS_CHANGED =
            "it.lavorodigruppo.flexipdf.ACTION_FILE_STATUS_CHANGED"
        const val EXTRA_OPEN_FILE_NAME = "it.lavorodigruppo.flexipdf.EXTRA_OPEN_FILE_NAME"
        private const val TAG = "FlexiPDFWidget"

        fun notifyFileStatusChanged(context: Context, openFileName: String?) {
            val intent = Intent(context, FlexiPDFWidgetProvider::class.java).apply {
                action = ACTION_FILE_STATUS_CHANGED
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
            } else {
                Log.d(TAG, "No widget instances found to update.")
            }
        }
    }
}