// it.lavorodigruppo.flexipdf.items/FolderItem.kt
package it.lavorodigruppo.flexipdf.items

import java.util.UUID // Importa UUID per generare ID univoci

/**
 * Rappresenta una cartella nel file system dell'applicazione.
 *
 * @property id ID univoco della cartella. Generato automaticamente se non fornito.
 * @property displayName Il nome della cartella.
 * @property parentFolderId L'ID della cartella genitore. Null se è una cartella di primo livello (root).
 * @property isSelected Indica se la cartella è selezionata nella UI.
 */
data class FolderItem(
    override val id: String = UUID.randomUUID().toString(), // Genera un UUID come ID predefinito
    override val displayName: String,
    override var isSelected: Boolean = false,
    override val parentFolderId: String?, // ID della cartella genitore (null per la root)
    val isCloudFolder: Boolean = false, // Nuovo: default a false per cartelle locali
    val cloudLinkParam: String? = null // Nuovo: default a null
) : FileSystemItem // Implementa la nuova interfaccia