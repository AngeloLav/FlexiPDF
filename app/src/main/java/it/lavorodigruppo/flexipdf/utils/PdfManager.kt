/**
 * @file PdfManager.kt
 *
 * @brief Gestore per la selezione di file PDF dal dispositivo e la persistenza dei permessi.
 *
 * Questa classe fornisce funzionalità per avviare un selettore di file di sistema,
 * permettendo all'utente di scegliere uno o più documenti PDF. Gestisce il risultato della selezione,
 * estrae gli URI e i nomi visualizzati dei file, e soprattutto si occupa di persistere
 * i permessi di accesso agli URI selezionati per garantire che l'applicazione possa
 * riaccedervi in futuro anche dopo il riavvio del dispositivo.
 *
 */
package it.lavorodigruppo.flexipdf.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Classe responsabile della gestione dell'interazione con il selettore di file del sistema
 * per permettere all'utente di scegliere documenti PDF, e di persistere i permessi necessari
 * per l'accesso futuro a tali file.
 *
 * @param activity L'istanza di `AppCompatActivity` che ospita questo gestore.
 * È necessaria per registrare l'`ActivityResultLauncher` e per mostrare `Toast`.
 * @param onPdfSelected Una funzione lambda che viene invocata quando uno o più PDF
 * vengono selezionati con successo dall'utente. Riceve una `List<Uri>`
 * contenente gli URI di tutti i PDF scelti.
 */
class PdfManager(private val activity: AppCompatActivity,
                 private val onPdfSelected: (List<Uri>) -> Unit
) {

    /**
     * `ActivityResultLauncher` utilizzato per avviare l'Activity di selezione del file PDF del sistema
     * e per gestire il risultato dell'interazione dell'utente con essa.
     * Questa callback viene invocata quando l'utente ha completato la selezione dei documenti (o l'ha annullata).
     * Se la selezione ha successo (`Activity.RESULT_OK`), estrae tutti gli URI selezionati
     * (gestendo sia la selezione singola che quella multipla tramite `clipData` e `data`)
     * e persiste i permessi per ogni URI. Infine, invoca la callback `onPdfSelected` fornita
     * nel costruttore con la lista degli URI raccolti.
     */
    private val pickPdfFile = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedUris = mutableListOf<Uri>()

            // Gestisce la selezione multipla tramite clipData
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    selectedUris.add(uri)
                    savePdfUri(uri)
                }
            }

            // Gestisce la selezione singola tramite data se clipData non è presente
            result.data?.data?.let { uri ->
                if (selectedUris.isEmpty()) {
                    selectedUris.add(uri)
                    savePdfUri(uri)
                }
            }

            if (selectedUris.isNotEmpty()) {
                onPdfSelected.invoke(selectedUris)

                val toastMessage = if (selectedUris.size == 1) {
                    "1 PDF selected."
                } else {
                    "${selectedUris.size} PDFs selected."
                }
                Toast.makeText(activity, toastMessage, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(activity, "PDF selection interrupted.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Avvia l'interfaccia standard del selettore di file del sistema operativo Android.
     * Configura un `Intent` con l'azione `ACTION_OPEN_DOCUMENT` e il tipo MIME "application/pdf"
     * per filtrare i file selezionabili e abilita la selezione multipla (`Intent.EXTRA_ALLOW_MULTIPLE`).
     * L'interazione con l'utente e il recupero del risultato sono gestiti dal `pickPdfFile` launcher.
     */
    fun launchPdfPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickPdfFile.launch(intent)
    }

    /**
     * Salva i permessi di accesso persistenti per un dato URI di un file.
     * Questo è un passo cruciale per la persistenza dell'accesso ai documenti,
     * in quanto permette all'applicazione di riaccedere al file selezionato dall'utente
     * anche dopo il riavvio dell'applicazione o del dispositivo, senza dover chiedere
     * nuovamente all'utente di selezionare il file.
     * Utilizza `ContentResolver.takePersistableUriPermission` per acquisire il permesso di lettura.
     *
     * @param uri L'URI del file PDF per il quale si desiderano persistere i permessi.
     */
    private fun savePdfUri(uri: Uri) {
        try {
            val contentResolver = activity.applicationContext.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION

            contentResolver.takePersistableUriPermission(uri, takeFlags)

        } catch (e: Exception) {
            Log.e("PdfManager", "Errore nel salvare i permessi per URI: ${uri.toString()}: ${e.message}", e)
            Toast.makeText(activity, "PDF permission errors", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Tenta di aprire un singolo documento PDF utilizzando un `Intent` implicito.
     * Crea un `Uri` dal `uriString` fornito e un `Intent.ACTION_VIEW` con il tipo MIME "application/pdf".
     * Aggiunge il flag `FLAG_GRANT_READ_URI_PERMISSION` per garantire i permessi di lettura
     * all'applicazione che aprirà il PDF. Questo metodo è utile per avviare un visualizzatore PDF esterno.
     *
     * @param uriString La rappresentazione in stringa dell'URI del file PDF da aprire.
     * @param fileName Il nome del file PDF, utilizzato principalmente per i messaggi di errore (`Toast`).
     */
    fun openPdf(uriString: String, fileName: String) {
        try {
            val uri = Uri.parse(uriString)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PdfManager", "Impossibile aprire il PDF: $fileName", e)
            Toast.makeText(activity, "Impossible to open: $fileName", Toast.LENGTH_SHORT).show()
        }
    }
}