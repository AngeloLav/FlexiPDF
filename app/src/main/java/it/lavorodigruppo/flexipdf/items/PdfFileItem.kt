/**
 * @file PdfFileItem.kt
 *
 * @brief Definizioni per la rappresentazione di un file PDF e funzioni di utilità per la serializzazione JSON.
 *
 * Questo file contiene la data class PdfFileItem, che modella i dati essenziali di un file PDF
 * da memorizzare e visualizzare nell'applicazione.
 *
 * Contiene inoltre funzioni di estensione per la serializzazione (toJson) e deserializzazione (toPdfFileList)
 * di liste di PdfFileItem da/verso il formato JSON, utilizzando la libreria Gson.
 * Queste funzioni sono fondamentali per la persistenza dei dati tramite SharedPreferences nel PdfDatasource.
 *
 */

// it.lavorodigruppo.flexipdf.items/PdfFileItem.kt
package it.lavorodigruppo.flexipdf.items

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID // Importa UUID per generare ID univoci

/**
 * @property id ID univoco del file PDF. Generato automaticamente se non fornito.
 * @property uriString La stringa che rappresenta l'URI del file PDF.
 * @property displayName Il nome visualizzato del file PDF (sostituisce displayName).
 * @property isSelected Indica se il file PDF è selezionato o meno dalla UI.
 * @property lastModified La data e ora dell'ultima modifica/apertura del file (timestamp).
 * @property isFavorite Indica se il file PDF è contrassegnato come preferito.
 * @property parentFolderId L'ID della cartella genitore. Null se è nella root.
 */
data class PdfFileItem(
    override val id: String = UUID.randomUUID().toString(), // Genera un UUID come ID predefinito
    val uriString: String,
    override val displayName: String, // Cambiato da displayName a name per coerenza con FileSystemItem
    override var isSelected: Boolean = false,
    val lastModified: Long = 0,
    var isFavorite: Boolean = false,
    override val parentFolderId: String? // Aggiunto per la gestione delle cartelle
) : FileSystemItem // Implementa la nuova interfaccia

/**
 * Funzione di estensione per List di PdfFileItem.
 * Converte una lista di PdfFileItem in una stringa JSON.
 *
 * @return Una String JSON che rappresenta la lista di PdfFileItem.
 */
fun List<PdfFileItem>.toJson(): String {
    return Gson().toJson(this)
}

/**
 * Funzione di estensione per String.
 * Converte una stringa JSON in una List di PdfFileItem.
 *
 * @return Una List di PdfFileItem deserializzata dalla stringa JSON.
 * Restituisce una emptyList se la stringa non è un JSON valido o se si verifica un errore durante la deserializzazione.
 */
fun String.toPdfFileList(): List<PdfFileItem> {
    return try {
        Gson().fromJson(this, object : TypeToken<List<PdfFileItem>>() {}.type)
    } catch (e: Exception) {
        emptyList()
    }
}