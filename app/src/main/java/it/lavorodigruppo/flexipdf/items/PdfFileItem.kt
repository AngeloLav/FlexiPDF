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

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Rappresenta un singolo file PDF all'interno del sistema di gestione file dell'applicazione.
 * Questa `data class` modella le proprietà essenziali di un file PDF, permettendone
 * la memorizzazione, visualizzazione e manipolazione all'interno dell'interfaccia utente.
 * Implementa l'interfaccia `FileSystemItem` per consentire un trattamento polimorfico con le cartelle.
 *
 * @property id Un identificatore univoco per questo file PDF. Se non fornito esplicitamente,
 * viene generato automaticamente un UUID. È fondamentale per `DiffUtil` e per la gestione interna.
 * @property uriString La rappresentazione in formato stringa dell'URI (Uniform Resource Identifier)
 * del file PDF. Questo URI permette di localizzare il file sul dispositivo per l'apertura e la lettura.
 * @property displayName Il nome che verrà mostrato all'utente per questo file PDF.
 * Di solito corrisponde al nome del file originale.
 * @property isSelected Un flag booleano che indica se il file PDF è attualmente selezionato nell'interfaccia utente.
 * Questa proprietà è utilizzata per gestire la selezione multipla e le azioni contestuali.
 * @property lastModified Il timestamp (in millisecondi dal 1° gennaio 1970 UTC)
 * dell'ultima modifica o dell'ultima apertura del file. Utile per l'ordinamento o la visualizzazione di informazioni.
 * @property isFavorite Un flag booleano che indica se il file PDF è stato contrassegnato dall'utente come "preferito".
 * Questa proprietà permette di filtrare e visualizzare rapidamente i file importanti.
 * @property parentFolderId L'ID della cartella genitore in cui questo file PDF è contenuto.
 * Se il file si trova nella directory radice del "cloud" o di una sezione principale, questo valore sarà `null`.
 * È essenziale per la ricostruzione della gerarchia del file system.
 */
data class PdfFileItem(
    override val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    override val displayName: String,
    override var isSelected: Boolean = false,
    val lastModified: Long = 0,
    var isFavorite: Boolean = false,
    override val parentFolderId: String?
) : FileSystemItem

/**
 * Funzione di estensione per una `List` di `PdfFileItem`.
 * Il suo ruolo è quello di convertire l'intera lista di oggetti `PdfFileItem` in una singola stringa JSON.
 * Questo è fondamentale per la persistenza dei dati, ad esempio salvando la lista in `SharedPreferences`
 * o in un database dove i dati vengono memorizzati come stringhe. Utilizza la libreria Gson per la serializzazione.
 *
 * @return Una `String` in formato JSON che rappresenta l'intera lista di `PdfFileItem`.
 */
fun List<PdfFileItem>.toJson(): String {
    return Gson().toJson(this)
}

/**
 * Funzione di estensione per una `String`.
 * Il suo ruolo è quello di deserializzare una stringa JSON e convertirla nuovamente in una `List` di `PdfFileItem`.
 * Questa funzione è utilizzata per recuperare i dati persistenti (ad esempio da `SharedPreferences`)
 * e ricostruire la lista di oggetti `PdfFileItem` su cui l'applicazione può operare.
 * Gestisce anche potenziali errori di deserializzazione, restituendo una lista vuota in caso di problemi.
 *
 * @return Una `List<PdfFileItem>` deserializzata dalla stringa JSON.
 * Restituisce una `emptyList()` se la stringa non è un JSON valido o se si verifica un errore
 * durante il processo di deserializzazione.
 */
fun String.toPdfFileList(): List<PdfFileItem> {
    return try {
        Gson().fromJson(this, object : TypeToken<List<PdfFileItem>>() {}.type)
    } catch (e: Exception) {
        emptyList()
    }
}