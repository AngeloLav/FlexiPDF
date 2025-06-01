/**
 * @file PdfDatasource.kt
 *
 * @brief Gestore della persistenza della lista dei file PDF tramite SharedPreferences.
 *
 * Questo datasource è responsabile di caricare e salvare la lista degli oggetti PdfFileItem
 * utilizzando SharedPreferences, un meccanismo di storage leggero basato su coppie chiave-valore
 * ideale per dati semplici e piccoli. È una classe che isola la logica di accesso ai dati dal resto dell'applicazione
 * Utilizza SharedPreferences per memorizzare la lista come una stringa JSON.
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
package it.lavorodigruppo.flexipdf.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import it.lavorodigruppo.flexipdf.items.toJson
import it.lavorodigruppo.flexipdf.items.toPdfFileList

class PdfDatasource(context: Context) {

    /*
     * L'istanza di SharedPreferences utilizzata per leggere e scrivere dati.
     * Vengono create/ottenute utilizzando il nome "pdf_prefs" e la modalità Context.MODE_PRIVATE,
     * il che significa che il file delle preferenze è accessibile solo da questa applicazione.
     */
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("pdf_prefs", Context.MODE_PRIVATE)

    /*
     * Chiave stringa utilizzata per identificare e recuperare la lista dei PDF salvata
     * all'interno di sharedPreferences.
     */
    private val PDF_LIST_KEY = "pdf_list_key"

    /**
     * Carica la lista dei PdfFileItem salvati dalle SharedPreferences.
     *
     * Recupera la stringa JSON associata a PDF_LIST_KEY.
     * Se la stringa esiste e può essere convertita in una lista (toPdfFileList),
     * restituisce quella lista. Altrimenti, restituisce una emptyList (lista vuota),
     * garantendo che il chiamante riceva sempre una lista non nulla.
     *
     * @return Una List di PdfFileItem caricata dalle preferenze, o una lista vuota se non presente o invalida.
     */
    fun loadPdfFiles(): List<PdfFileItem> {
        val json = sharedPreferences.getString(PDF_LIST_KEY, null)
        // L'operatore '?.toPdfFileList()' tenta la conversione solo se 'json' non è null.
        // L'operatore Elvis '?: emptyList()' fornisce una lista vuota se la conversione fallisce o 'json' era null.
        return json?.toPdfFileList() ?: emptyList()
    }

    /**
     * Salva la lista corrente di PdfFileItem nelle SharedPreferences.
     *
     * La lista viene prima convertita in una stringa JSON (toJson) e poi salvata
     * con la chiave PDF_LIST_KEY.
     *
     * @param pdfFiles La List di PdfFileItem da salvare.
     */
    fun savePdfFiles(pdfFiles: List<PdfFileItem>) {
        sharedPreferences.edit {
            putString(PDF_LIST_KEY, pdfFiles.toJson())
        }
    }
}