/**
 * @file FileSystemDatasource.kt
 *
 * @brief Gestore della persistenza della lista dei file PDF tramite SharedPreferences.
 *
 * Questo datasource è responsabile di caricare e salvare la lista degli oggetti PdfFileItem
 * utilizzando SharedPreferences, un meccanismo di storage leggero basato su coppie chiave-valore
 * ideale per dati semplici e piccoli. È una classe che isola la logica di accesso ai dati dal resto dell'applicazione
 * SharedPreferences memorizza la lista come una stringa JSON.
 *
 * Cos'è SharedPreferences?
 * SharedPreferences è un'API fornita dal framework Android che permette alle applicazioni di
 * memorizzare e recuperare piccole quantità di dati primitivi (come stringhe, interi, booleani, float e long)
 * in coppie chiave-valore persistenti. Questi dati vengono salvati in file XML all'interno della
 * directory privata dell'applicazione sul dispositivo. È comunemente usato per:
 * - Memorizzare stati semplici (es. "utente ha già visto il tutorial").
 * - Memorizzare piccole quantità di dati strutturati dopo serializzazione (come la lista di PDF in formato JSON).
 *
 * Le operazioni di conversione da/verso JSON sono gestite da funzioni di estensione
 * definite per PdfFileItem e List<PdfFileItem>.
 *      @see
 *      it.lavorodigruppo.flexipdf/items/PdfFileItem.kt
 *
 */
// it.lavorodigruppo.flexipdf.data/FileSystemDatasource.kt
package it.lavorodigruppo.flexipdf.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.lavorodigruppo.flexipdf.items.FolderItem // Importa il tuo FolderItem
import it.lavorodigruppo.flexipdf.items.PdfFileItem // Importa il tuo PdfFileItem
import it.lavorodigruppo.flexipdf.items.toJson // Assicurati che toJson/toPdfFileList siano qui o importati
import it.lavorodigruppo.flexipdf.items.toPdfFileList

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log // Importa Log per i messaggi di debug

class FileSystemDatasource(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "FlexiPdfData" // Nome consolidato per le preferenze
        private const val KEY_FOLDERS = "folders_v1" // Nuova chiave per le cartelle
        private const val KEY_PDF_FILES = "pdfFiles_v1" // Nuova chiave per i PDF (importante per pulire i vecchi dati)
        private const val KEY_CURRENT_FOLDER_ID = "currentFolderId_v1" // ID della cartella corrente (non path)

        // ID speciali per la root
        const val ROOT_FOLDER_ID = "root"
    }

    // --- Gestione PDF Files ---
    suspend fun loadPdfFiles(): MutableList<PdfFileItem> = withContext(Dispatchers.IO) {
        val json = sharedPreferences.getString(KEY_PDF_FILES, null)
        val loadedList = try {
            json?.toPdfFileList() ?: mutableListOf()
        } catch (e: Exception) {
            // Logga l'errore e restituisci una lista vuota per evitare crash
            Log.e("FileSystemDatasource", "Errore durante il caricamento dei PDF: ${e.message}", e)
            // IMPORTANTE: Se c'è un errore di deserializzazione, cancelliamo i dati vecchi
            // per evitare crash futuri all'avvio.
            sharedPreferences.edit() { remove(KEY_PDF_FILES) }
            mutableListOf()
        }
        // Resetta isSelected a false per tutti i file caricati.
        return@withContext loadedList.map { it.copy(isSelected = false) }.toMutableList()
    }

    suspend fun savePdfFiles(pdfFiles: List<PdfFileItem>) = withContext(Dispatchers.IO) {
        sharedPreferences.edit {
            putString(KEY_PDF_FILES, pdfFiles.toJson())
        }
    }

    // --- Gestione Cartelle ---
    suspend fun loadFolders(): MutableList<FolderItem> = withContext(Dispatchers.IO) {
        val json = sharedPreferences.getString(KEY_FOLDERS, null)
        val loadedList = try {
            json?.let {
                val type = object : TypeToken<MutableList<FolderItem>>() {}.type
                gson.fromJson<MutableList<FolderItem>>(it, type)
            } ?: mutableListOf()
        } catch (e: Exception) {
            // Logga l'errore e restituisci una lista vuota per evitare crash
            Log.e("FileSystemDatasource", "Errore durante il caricamento delle cartelle: ${e.message}", e)
            sharedPreferences.edit() { remove(KEY_FOLDERS) }
            mutableListOf()
        }
        return@withContext loadedList.map { it.copy(isSelected = false) }.toMutableList()
    }

    suspend fun saveFolders(folders: List<FolderItem>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(folders)
        sharedPreferences.edit() { putString(KEY_FOLDERS, json) }
    }

    // --- Gestione Cartella Corrente (usiamo ID al posto del path) ---
    suspend fun saveCurrentFolderId(folderId: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit() { putString(KEY_CURRENT_FOLDER_ID, folderId) }
    }

    suspend fun loadCurrentFolderId(): String = withContext(Dispatchers.IO) {
        return@withContext sharedPreferences.getString(KEY_CURRENT_FOLDER_ID, ROOT_FOLDER_ID) ?: ROOT_FOLDER_ID
    }
}