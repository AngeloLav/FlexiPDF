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
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

/**
 * @param activity L'istanza di AppCompatActivity che ospita questo gestore. È necessaria per
 * registrare l'ActivityResultLauncher e per accedere al ContentResolver.
 * @param onPdfSelected Una funzione lambda (Uri, String) -> Unit che viene invocata quando
 * un PDF viene selezionato con successo. Riceve l'URI del PDF e il suo nome visualizzato.
 */
class PdfManager(private val activity: AppCompatActivity, private val onPdfSelected: (Uri, String) -> Unit) {

    /**
     * ActivityResultLauncher per avviare l'Activity di selezione del file PDF e gestire il suo risultato.
     *
     * Il contratto [ActivityResultContracts.StartActivityForResult()] viene utilizzato per lanciare
     * un Intent e ricevere un risultato generico.
     *
     * Il blocco lambda viene eseguito quando l'Activity esterna restituisce un risultato.
     * - Se `result.resultCode` è Activity.RESULT_OK, significa che l'operazione è andata a buon fine.
     * - Si tenta di estrarre l'URI del PDF dai dati dell'Intent (`result.data?.data`).
     * - Se l'URI è valido, si procede con l'ottenimento del nome del file, la persistenza dei permessi
     * e l'invocazione del callback onPdfSelected.
     * - Se l'URI è nullo o il risultato non è Activity.RESULT_OK, viene mostrato un Toast all'utente.
     */
    private val pickPdfFile = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {

                // Ottiene il nome visualizzato del file dall'URI.
                val displayName = getFileName(it)

                // Salva i permessi di accesso persistenti per l'URI.
                savePdfUri(it)
                // Invoca il callback fornito per notificare l'Activity/ViewModel chiamante.
                onPdfSelected.invoke(it, displayName)

                // Uncommenta se vuoi che venga visualizzato il pdf appena selezionato
                // openPdfViewerActivity(it)
            } ?: run {
                // Se l'URI è nullo (es. l'utente ha selezionato ma il dato è vuoto).
                Toast.makeText(activity, "Nessun PDF selezionato", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Se l'operazione di selezione è stata interrotta (es. l'utente ha premuto indietro).
            Toast.makeText(activity, "Selezione PDF interrotta", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Avvia l'interfaccia di selezione del file PDF del sistema.
     * Crea un Intent con l'azione Intent.ACTION_OPEN_DOCUMENT, che permette all'utente
     * di selezionare un documento dal provider di documenti del dispositivo.
     * Limita la selezione ai soli file di tipo "application/pdf".
     */
    fun launchPdfPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            // Aggiunge la categoria per indicare che l'intent deve mostrare dati "apribili".
            addCategory(Intent.CATEGORY_OPENABLE)
            // Specifica il tipo MIME per filtrare solo i file PDF.
            type = "application/pdf"
        }
        // Avvia l'Activity di selezione file utilizzando il launcher registrato.
        pickPdfFile.launch(intent)
    }

    /**
     * Salva i permessi di accesso persistenti per un dato Uri.
     * Questo è cruciale per poter riaccedere al file selezionato dall'utente in futuro,
     * anche dopo il riavvio dell'applicazione o del dispositivo.
     * Utilizza ContentResolver.takePersistableUriPermission: rende il permesso di accesso a questo
     * URI persistente per l'App
     *
     * @param uri L'URI del file PDF per il quale si desiderano persistire i permessi.
     */
    private fun savePdfUri(uri: Uri) {
        try {
            // Ottiene il ContentResolver per interagire con i content provider.
            val contentResolver = activity.applicationContext.contentResolver
            // Definisce i flag per i permessi di lettura e scrittura.
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            // Prende i permessi persistenti per l'URI.
            contentResolver.takePersistableUriPermission(uri, takeFlags)

        } catch (e: Exception) {
            // Gestisce eventuali errori durante il salvataggio dei permessi,
            // loggando l'errore e mostrando un messaggio all'utente.
            Log.e("PDF_PICKER", "Error in saving the permission for URI: ${e.message}")
        }
    }

    /**
     * Estrae il nome visualizzato del file da un dato Uri.
     * Tenta prima di ottenere il nome tramite il ContentResolver,
     * che è il metodo più affidabile per i file selezionati.
     * Se fallisce, tenta di estrarlo dal percorso dell'URI.
     *
     * @param uri L'URI del file PDF.
     * @return Il nome visualizzato del file, o "Unknown PDF" se non è possibile determinarlo.
     */
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        // Se l'URI è di tipo 'content://' (tipico per i file selezionati dal selettore di sistema).
        if (uri.scheme == "content") {
            // Esegue una query sul ContentResolver per ottenere i metadati del file.
            activity.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                // Sposta il cursore alla prima riga (se presente).
                if (cursor.moveToFirst()) {
                    // Ottiene l'indice della colonna che contiene il nome visualizzato.
                    val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    // Se l'indice è valido, recupera il nome.
                    if (displayNameIndex != -1) {
                        result = cursor.getString(displayNameIndex)
                    }
                }
            }
        }
        // Se il nome non è stato trovato tramite ContentResolver
        if (result == null) {
            // Prende il percorso dell'URI come fallback.
            result = uri.path
            // Estrae il nome del file dalla fine del percorso.
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        // Restituisce il risultato trovato o "Unknown PDF" come fallback finale.
        return result ?: "Unknown PDF"
    }
}