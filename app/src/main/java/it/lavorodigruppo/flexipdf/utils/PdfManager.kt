/**
 * @file PdfManager.kt
 *
 * @brief Gestore per la selezione di file PDF dal dispositivo e la persistenza dei permessi.
 *
 * Questa classe fornisce funzionalità per avviare un selettore di file di sistema,
 * permettendo all'utente di scegliere un documento PDF. Gestisce il risultato della selezione,
 * estrae l'URI e il nome visualizzato del file, e soprattutto si occupa di persistere
 * i permessi di accesso all'URI selezionato per garantire che l'applicazione possa
 * riaccedervi in futuro anche dopo il riavvio del dispositivo.
 *
 * @author Angelo
 * @date 31/05/25
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
 * @param activity L'istanza di AppCompatActivity che ospita questo gestore. È necessaria per
 * registrare l'ActivityResultLauncher.
 * @param onPdfSelected Una funzione lambda (List<Uri>) -> Unit che viene invocata quando
 * uno o più PDF vengono selezionati con successo. Riceve la lista degli URI dei PDF.
 */
class PdfManager(private val activity: AppCompatActivity,
                 private val onPdfSelected: (List<Uri>) -> Unit // <--- FIRMA DELLA CALLBACK CORRETTA: SOLO List<Uri>
) {

    /**
     * ActivityResultLauncher per avviare l'Activity di selezione del file PDF e gestire il suo risultato.
     */
    private val pickPdfFile = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedUris = mutableListOf<Uri>()

            // Gestisce la selezione multipla (clipData)
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    selectedUris.add(uri)
                    savePdfUri(uri) // Salva i permessi per ogni URI selezionato
                }
            }

            // Gestisce la selezione singola (data) se clipData non è presente
            result.data?.data?.let { uri ->
                if (selectedUris.isEmpty()) { // Aggiunge solo se non già gestito da clipData
                    selectedUris.add(uri)
                    savePdfUri(uri) // Salva i permessi per l'URI singolo
                }
            }

            if (selectedUris.isNotEmpty()) {
                // Invoca il callback fornito con la lista completa degli URI selezionati
                // La logica per ottenere i nomi e importare è nel ViewModel.
                onPdfSelected.invoke(selectedUris) // <--- CHIAMATA CORRETTA: PASSA SOLO selectedUris

                // Messaggio Toast semplificato, la conferma di importazione vera è nel ViewModel
                val toastMessage = if (selectedUris.size == 1) {
                    "1 PDF selezionato."
                } else {
                    "${selectedUris.size} PDF selezionati."
                }
                Toast.makeText(activity, toastMessage, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(activity, "Selezione PDF interrotta.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Avvia l'interfaccia di selezione del file PDF del sistema.
     * Permette la selezione multipla.
     */
    fun launchPdfPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Abilita la selezione multipla
        }
        pickPdfFile.launch(intent)
    }

    /**
     * Salva i permessi di accesso persistenti per un dato Uri.
     * Questo è cruciale per poter riaccedere al file selezionato dall'utente in futuro.
     *
     * @param uri L'URI del file PDF per il quale si desiderano persistire i permessi.
     */
    private fun savePdfUri(uri: Uri) {
        try {
            val contentResolver = activity.applicationContext.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION

            contentResolver.takePersistableUriPermission(uri, takeFlags)

        } catch (e: Exception) {
            Log.e("PdfManager", "Errore nel salvare i permessi per URI: ${uri.toString()}: ${e.message}", e)
            Toast.makeText(activity, "Errore permessi per PDF.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Metodo per aprire un singolo PDF (utile se lo chiami direttamente da qualche parte).
     * Nota: il ViewModel ora ha la logica per aprire il PDF e aggiornare lastModified.
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
            Toast.makeText(activity, "Impossibile aprire il PDF: $fileName", Toast.LENGTH_SHORT).show()
        }
    }
}