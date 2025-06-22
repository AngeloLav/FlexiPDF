package it.lavorodigruppo.flexipdf.items

import java.util.UUID

/**
 * Rappresenta una cartella all'interno del sistema di gestione file dell'applicazione.
 * Questa `data class` modella le proprietà di una cartella, inclusi il suo identificatore,
 * il nome visualizzato, lo stato di selezione, il riferimento alla cartella genitore,
 * e proprietà specifiche che indicano se si tratta di una cartella "cloud" e l'eventuale parametro di collegamento cloud.
 * Implementa l'interfaccia `FileSystemItem` per consentire un trattamento polimorfico con altri elementi del file system.
 *
 * @property id Un identificatore univoco per questa cartella. Se non fornito esplicitamente,
 * viene generato automaticamente un UUID. Essenziale per `DiffUtil` e per la gestione interna.
 * @property displayName Il nome che verrà mostrato all'utente per questa cartella.
 * @property isSelected Un flag booleano che indica se la cartella è attualmente selezionata nell'interfaccia utente.
 * Utilizzato principalmente per la modalità di selezione multipla.
 * @property parentFolderId L'ID della cartella genitore in cui questa cartella è contenuta.
 * Sarà `null` se la cartella è una cartella di primo livello (radice) nel suo contesto.
 * @property isCloudFolder Un flag booleano che indica se questa cartella rappresenta un tipo di risorsa "cloud".
 * Di default è `false`, indicando una cartella locale o generica.
 * @property cloudLinkParam Un parametro opzionale (`String?`) che può essere utilizzato per memorizzare
 * informazioni aggiuntive relative a un collegamento cloud, come un ID specifico
 * o un token, se `isCloudFolder` è `true`. Di default è `null`.
 */
data class FolderItem(
    override val id: String = UUID.randomUUID().toString(),
    override val displayName: String,
    override var isSelected: Boolean = false,
    override val parentFolderId: String?,
    val isCloudFolder: Boolean = false,
    val cloudLinkParam: String? = null
) : FileSystemItem