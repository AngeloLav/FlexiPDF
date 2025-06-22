// it.lavorodigruppo.flexipdf.items/FileSystemItem.kt
package it.lavorodigruppo.flexipdf.items

/**
 * Interfaccia comune che rappresenta un elemento generico all'interno del sistema di gestione file dell'applicazione.
 * Questa interfaccia permette di trattare in modo polimorfico sia i file PDF che le cartelle,
 * consentendo di gestirli in liste e operazioni comuni indipendentemente dal loro tipo specifico.
 */
interface FileSystemItem {
    /**
     * Un identificatore univoco per l'elemento del file system.
     * Viene utilizzato per distinguere in modo univoco ogni file o cartella all'interno del sistema,
     * essenziale per operazioni come l'identificazione, la selezione e la manipolazione degli elementi.
     */
    val id: String
    /**
     * Il nome visualizzabile dell'elemento, che sarà mostrato all'utente nell'interfaccia.
     * Per i file PDF, sarà il nome del file (es. "documento.pdf"); per le cartelle, sarà il nome della cartella.
     */
    val displayName: String
    /**
     * Un flag booleano che indica se l'elemento è attualmente selezionato nell'interfaccia utente.
     * Questa proprietà è cruciale per la gestione della selezione multipla, consentendo di tenere traccia
     * degli elementi su cui l'utente intende eseguire un'azione (es. eliminazione, spostamento).
     */
    var isSelected: Boolean
    /**
     * L'ID della cartella genitore in cui si trova questo elemento.
     * Se l'elemento si trova nella directory radice del "cloud" o di una sezione principale,
     * questo valore sarà `null`. Viene utilizzato per ricostruire la gerarchia del file system.
     */
    val parentFolderId: String?
}