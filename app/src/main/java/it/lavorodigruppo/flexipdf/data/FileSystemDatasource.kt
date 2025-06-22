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
 * @see
 * it.lavorodigruppo.flexipdf/items/PdfFileItem.kt
 *
 */
package it.lavorodigruppo.flexipdf.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.lavorodigruppo.flexipdf.items.FolderItem
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import it.lavorodigruppo.flexipdf.items.toJson
import it.lavorodigruppo.flexipdf.items.toPdfFileList

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Gestisce il caricamento e il salvataggio dei dati relativi al file system (file PDF e cartelle)
 * utilizzando SharedPreferences. Serializza e deserializza gli oggetti da/verso il formato JSON
 * per la persistenza.
 *
 * @param context Il contesto dell'applicazione, necessario per accedere a SharedPreferences.
 */
class FileSystemDatasource(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "FlexiPdfData"
        private const val KEY_FOLDERS = "folders_v1"
        private const val KEY_PDF_FILES = "pdfFiles_v1"
        private const val KEY_CURRENT_FOLDER_ID = "currentFolderId_v1"

        const val ROOT_FOLDER_ID = "root"
    }

    /**
     * Carica la lista di `PdfFileItem` salvati in SharedPreferences.
     * La lista viene deserializzata da una stringa JSON. In caso di errore di deserializzazione,
     * i dati corrotti vengono rimossi e viene restituita una lista vuota.
     * Tutti i file PDF caricati hanno il loro stato `isSelected` impostato su `false`.
     *
     * @return Una `MutableList` di `PdfFileItem` caricati, o una lista vuota se non ci sono dati o si verifica un errore.
     */
    suspend fun loadPdfFiles(): MutableList<PdfFileItem> = withContext(Dispatchers.IO) {
        val json = sharedPreferences.getString(KEY_PDF_FILES, null)
        val loadedList = try {
            json?.toPdfFileList() ?: mutableListOf()
        } catch (e: Exception) {
            Log.e("FileSystemDatasource", "Errore durante il caricamento dei PDF: ${e.message}", e)
            sharedPreferences.edit() { remove(KEY_PDF_FILES) }
            mutableListOf()
        }
        return@withContext loadedList.map { it.copy(isSelected = false) }.toMutableList()
    }

    /**
     * Salva la lista fornita di `PdfFileItem` in SharedPreferences.
     * La lista viene serializzata in una stringa JSON prima di essere salvata.
     *
     * @param pdfFiles La lista di `PdfFileItem` da salvare.
     */
    suspend fun savePdfFiles(pdfFiles: List<PdfFileItem>) = withContext(Dispatchers.IO) {
        sharedPreferences.edit {
            putString(KEY_PDF_FILES, pdfFiles.toJson())
        }
    }

    /**
     * Carica la lista di `FolderItem` salvati in SharedPreferences.
     * La lista viene deserializzata da una stringa JSON. In caso di errore di deserializzazione,
     * i dati corrotti vengono rimossi e viene restituita una lista vuota.
     * Tutte le cartelle caricate hanno il loro stato `isSelected` impostato su `false`.
     *
     * @return Una `MutableList` di `FolderItem` caricati, o una lista vuota se non ci sono dati o si verifica un errore.
     */
    suspend fun loadFolders(): MutableList<FolderItem> = withContext(Dispatchers.IO) {
        val json = sharedPreferences.getString(KEY_FOLDERS, null)
        val loadedList = try {
            json?.let {
                val type = object : TypeToken<MutableList<FolderItem>>() {}.type
                gson.fromJson<MutableList<FolderItem>>(it, type)
            } ?: mutableListOf()
        } catch (e: Exception) {
            Log.e("FileSystemDatasource", "Errore durante il caricamento delle cartelle: ${e.message}", e)
            sharedPreferences.edit() { remove(KEY_FOLDERS) }
            mutableListOf()
        }
        return@withContext loadedList.map { it.copy(isSelected = false) }.toMutableList()
    }

    /**
     * Salva la lista fornita di `FolderItem` in SharedPreferences.
     * La lista viene serializzata in una stringa JSON prima di essere salvata.
     *
     * @param folders La lista di `FolderItem` da salvare.
     */
    suspend fun saveFolders(folders: List<FolderItem>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(folders)
        sharedPreferences.edit() { putString(KEY_FOLDERS, json) }
    }

    /**
     * Salva l'ID della cartella corrente in SharedPreferences.
     * Questo permette di ripristinare la posizione di navigazione dell'utente all'avvio successivo dell'app.
     *
     * @param folderId L'ID della cartella corrente da salvare.
     */
    suspend fun saveCurrentFolderId(folderId: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit() { putString(KEY_CURRENT_FOLDER_ID, folderId) }
    }

    /**
     * Carica l'ID della cartella corrente da SharedPreferences.
     * Se non è presente un ID salvato, o se il valore è nullo, restituisce `ROOT_FOLDER_ID` come predefinito.
     *
     * @return L'ID della cartella corrente caricata, o `ROOT_FOLDER_ID` se non specificato.
     */
    suspend fun loadCurrentFolderId(): String = withContext(Dispatchers.IO) {
        return@withContext sharedPreferences.getString(KEY_CURRENT_FOLDER_ID, ROOT_FOLDER_ID) ?: ROOT_FOLDER_ID
    }
}