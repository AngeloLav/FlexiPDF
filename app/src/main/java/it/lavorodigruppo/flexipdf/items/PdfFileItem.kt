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

package it.lavorodigruppo.flexipdf.items

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.net.toUri

/**
 * @property uriString La stringa che rappresenta l'URI del file PDF.
 * Viene memorizzata come Stringa e non direttamente come Uri perché
 * Uri non è direttamente serializzabile a JSON da Gson senza un TypeAdapter personalizzato,
 * e memorizzare l'URI come Stringa è più semplice per la persistenza.
 * @property displayName Il nome visualizzato del file PDF.
 * @property isSelected Indica se il file PDF è selezionato o meno dalla recyclerView.
 */

data class PdfFileItem(
    val uriString: String,
    val displayName: String,
    var isSelected: Boolean = false,
    var isFavorite: Boolean = false
)

/**
 * Funzione di estensione per List di PdfFileItem.
 * Converte una lista di PdfFileItem in una stringa JSON.
 *
 * Utilizzata dal PdfDatasource per salvare la lista dei PDF nelle SharedPreferences.
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
 * Utilizzata dal PdfDatasource per caricare la lista dei PDF dalle SharedPreferences.
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