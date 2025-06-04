// it.lavorodigruppo.flexipdf.items/FileSystemItem.kt
package it.lavorodigruppo.flexipdf.items

/**
 * Interfaccia comune per rappresentare un elemento nel file system (file PDF o cartella).
 * Permette di trattare entrambi i tipi di elementi in modo polimorfico nelle liste.
 */
interface FileSystemItem {
    val id: String // ID univoco dell'elemento
    val displayName: String // Nome dell'elemento (display name per PDF, nome per cartella)
    var isSelected: Boolean // Indica se l'elemento è selezionato nella UI
    val parentFolderId: String? // ID della cartella genitore (null per elementi nella root)
}