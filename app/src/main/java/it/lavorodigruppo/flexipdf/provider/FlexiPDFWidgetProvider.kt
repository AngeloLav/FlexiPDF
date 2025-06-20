package it.lavorodigruppo.flexipdf.provider

/** @file FlexiPDFWidgetProvider.kt
 * Il seguente file implementa la classe AppWidgetProvider, estendendo la classe Broadcast Receiver.
 * Vengono sovrascritti i seguenti metodi:
 *
 * onEnabled(context: Context?) viene chiamata quando la prima istanza della app viene creata; anche
 * se ne vengono istanziati più oggetti widget, la classe viene chiamata un'unica volta. Vengono usati
 * degli oggetti di tipo NotificationUtil, in modo da creare e gestire le notifiche specifiche.
 *
 * onDisabled(context: Context?) viene chiamata quando l'ultimo widget dell'app in questione viene eliminata,
 * quindi si fa un clean up del lavoro fatto.
 *
 * onAppWidgetOptionsChanged() viene chiamata nella prima creazione del widget e ogni volta che si fa
 * un ridimensionamento dell' interfaccia.
 *
 * onUpdate() è una funzione richiamata in un intervallo definito negli attributi di AppWidgetInfo
 * (@see @file res/xml/app_flexipdf_widget_zflip.xml l'attributo updatePeriodMillis="86400000").
 * Inoltre, viene chiamata ogni volta che un altro widget viene aggiunto. All'interno è stato usata
 * una funzione definita nell'ultima parte del codice getRemoteView() che compie il compito di collegare
 * il layout quando nuovi widget vengono inizializzati. La funzione ritorna in uscita un oggetto di tipo
 * RemoteViews, una classe di android.widget, che permette di definire i layout da mostrare, specificando
 * le risorse layout necessarie.
 *
*/


import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import it.lavorodigruppo.flexipdf.NotificationUtil
import it.lavorodigruppo.flexipdf.R

class FlexiPDFWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        context?:return
        NotificationUtil(context).createNotificationChannel()
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        context?: return
        NotificationUtil(context).cancelOngoingNotification(NotificationUtil.NOTIFICATION_ID)
    }

    override fun onAppWidgetOptionsChanged(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetId: Int, newOptions: Bundle?) {
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

        appWidgetIds.forEach {
            val remoteViews = getRemoteViews(context, it)
            appWidgetManager.updateAppWidget(it, remoteViews)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun getRemoteViews(context: Context, id: Int): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_zflip).apply{
            //STILL NEEDS TO BE IMPLEMENTED
        }
    }

}