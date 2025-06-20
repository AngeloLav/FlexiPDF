/**
 * @file FileSystemViewModel.kt
 *
 * @overview
 *
 * `FileSystemViewModel` è un `AndroidViewModel` responsabile della gestione e dell'esposizione
 * dello stato del file system dell'applicazione (file PDF e cartelle) all'interfaccia utente.
 * Estendendo `AndroidViewModel`, ha accesso al contesto dell'applicazione,
 * garantendo che possa interagire con risorse e persistenza dati per l'intera vita dell'applicazione.
 *
 * Scopo principale:
 * - Mantenere lo stato attuale di tutti i file PDF e le cartelle, inclusi quelli selezionati e in fase di spostamento.
 * - Esporre i dati pertinenti alla UI tramite `StateFlow` e `Flow`, permettendo aggiornamenti reattivi.
 * - Centralizzare la logica di business relativa alla gestione del file system (aggiunta, rimozione,
 * spostamento, navigazione tra cartelle, ricerca, gestione preferiti) separandola dalla logica dell'interfaccia utente.
 * - Gestire la persistenza dei dati caricando e salvando file e cartelle tramite `FileSystemDatasource`.
 *
 *
 */
package it.lavorodigruppo.flexipdf.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.lavorodigruppo.flexipdf.data.FileSystemDatasource
import it.lavorodigruppo.flexipdf.items.FileSystemItem
import it.lavorodigruppo.flexipdf.items.FolderItem
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Stack

/**
 * ViewModel principale per la gestione del file system dell'applicazione, inclusi file PDF e cartelle.
 * Fornisce dati osservabili alla UI e gestisce le operazioni di business logiche relative ai file.
 *
 * @param application L'istanza dell'applicazione, fornita automaticamente dal framework Android
 * quando si estende `AndroidViewModel`. Permette l'accesso a risorse di sistema e al `ContentResolver`.
 */
class FileSystemViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Un `MutableSharedFlow` utilizzato per emettere messaggi monouso all'interfaccia utente.
     * Questi messaggi sono tipicamente notifiche (come `Toast`) che devono essere visualizzate
     * una sola volta per un evento specifico.
     */
    private val _userMessage = MutableSharedFlow<String>()

    /**
     * `Flow` pubblico che espone i messaggi utente per l'osservazione da parte della UI.
     * La UI può raccogliere gli eventi da questo `Flow` per mostrare messaggi all'utente.
     */
    val userMessage: Flow<String> = _userMessage.asSharedFlow()

    /**
     * Emette un messaggio da visualizzare all'utente tramite l'interfaccia utente (es. un `Toast`).
     * Questo metodo viene chiamato da altri componenti del ViewModel per comunicare feedback all'utente.
     * L'emissione avviene su un `MutableSharedFlow` all'interno di una coroutine.
     *
     * @param message La stringa del messaggio da visualizzare.
     */
    fun showUserMessage(message: String) {
        viewModelScope.launch {
            _userMessage.emit(message)
        }
    }

    /**
     * Istanza del `FileSystemDatasource` responsabile del caricamento e del salvataggio
     * dei file PDF e delle cartelle persistenti. Questo separa la logica di accesso ai dati
     * dal ViewModel.
     */
    private val datasource = FileSystemDatasource(application)

    /**
     * `MutableStateFlow` interno che mantiene lo stato corrente di tutti i `PdfFileItem`
     * caricati nell'applicazione. Viene aggiornato ogni volta che la lista dei PDF cambia.
     */
    private val _allPdfFiles = MutableStateFlow<List<PdfFileItem>>(emptyList())

    /**
     * `MutableStateFlow` interno che mantiene lo stato corrente di tutti i `FolderItem`
     * caricati nell'applicazione. Viene aggiornato ogni volta che la lista delle cartelle cambia.
     */
    private val _allFolders = MutableStateFlow<List<FolderItem>>(emptyList())

    /**
     * `MutableStateFlow` interno che rappresenta la cartella attualmente visualizzata.
     * Se `null`, indica che l'utente si trova nella directory radice.
     */
    private val _currentFolder = MutableStateFlow<FolderItem?>(null)

    /**
     * `StateFlow` pubblico che espone la cartella corrente all'interfaccia utente.
     * La UI osserva questo `StateFlow` per aggiornare la visualizzazione in base alla cartella attuale.
     */
    val currentFolder: StateFlow<FolderItem?> = _currentFolder.asStateFlow()

    /**
     * Stack utilizzato per tenere traccia della cronologia di navigazione delle cartelle.
     * Permette all'utente di tornare indietro alle cartelle precedenti.
     * Contiene gli ID delle cartelle.
     */
    private val folderNavigationStack = Stack<String>()

    /**
     * `MutableStateFlow` interno che indica se la modalità di selezione multipla è attiva.
     * Questa modalità viene attivata quando l'utente inizia a selezionare elementi.
     */
    private val _isSelectionModeActive = MutableStateFlow(false)

    /**
     * `StateFlow` pubblico che espone lo stato della modalità di selezione all'interfaccia utente.
     * La UI può usare questo per mostrare/nascondere barre degli strumenti contestuali.
     */
    val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    /**
     * `MutableStateFlow` interno che mantiene l'insieme degli `FileSystemItem` (PDF o cartelle)
     * attualmente selezionati dall'utente. Viene utilizzato un `Set` per garantire l'unicità degli elementi.
     */
    private val _selectedItems = MutableStateFlow<Set<FileSystemItem>>(emptySet())

    /**
     * `StateFlow` pubblico che espone l'insieme degli elementi selezionati all'interfaccia utente.
     * La UI può osservare questo per aggiornare le visualizzazioni degli elementi selezionati.
     */
    val selectedItems: StateFlow<Set<FileSystemItem>> = _selectedItems.asStateFlow()

    /**
     * `MutableStateFlow` interno che memorizza la stringa di ricerca corrente inserita dall'utente.
     * Usato per filtrare gli elementi visualizzati.
     */
    private val _searchQuery = MutableStateFlow("")

    /**
     * `MutableStateFlow` interno che contiene la lista degli `FileSystemItem` che sono stati contrassegnati
     * per essere spostati. Questi elementi sono quelli che l'utente ha selezionato prima di avviare
     * l'operazione di spostamento.
     */
    private val _itemsToMove = MutableStateFlow<List<FileSystemItem>>(emptyList())

    /**
     * `StateFlow` pubblico che espone la lista degli elementi in attesa di essere spostati all'interfaccia utente.
     * Questo è utile per visualizzare quali elementi sono stati selezionati per l'operazione di spostamento.
     */
    val itemsToMove: StateFlow<List<FileSystemItem>> = _itemsToMove.asStateFlow()

    /**
     * `MutableStateFlow` interno che indica se l'applicazione è attualmente in modalità di spostamento.
     * Questa modalità viene attivata quando l'utente seleziona elementi e sceglie di spostarli.
     */
    private val _isMovingItems = MutableStateFlow(false)

    /**
     * `StateFlow` pubblico che espone lo stato della modalità di spostamento all'interfaccia utente.
     * La UI può usarlo per modificare il comportamento (es. disabilitare certe azioni, cambiare icone).
     */
    val isMovingItems: StateFlow<Boolean> = _isMovingItems.asStateFlow()

    /**
     * `StateFlow` che emette una lista dei file PDF aperti o modificati più di recente.
     * Mappa da `_allPdfFiles`, ordina per `lastModified` in ordine decrescente e prende i primi 15 elementi.
     * Viene aggiornato ogni volta che `_allPdfFiles` cambia.
     */
    val recentPdfs: StateFlow<List<PdfFileItem>> = _allPdfFiles.map { allFiles ->
        allFiles.sortedByDescending { it.lastModified }.take(15)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    /**
     * `StateFlow` che emette una lista dei file PDF contrassegnati come preferiti.
     * Mappa da `_allPdfFiles`, filtrando solo gli elementi con `isFavorite` a `true`.
     * Viene aggiornato ogni volta che `_allPdfFiles` cambia.
     */
    val favoritePdfs: StateFlow<List<PdfFileItem>> = _allPdfFiles.map { allFiles ->
        allFiles.filter { it.isFavorite }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    /**
     * `StateFlow` che combina e filtra le liste di tutti i PDF, tutte le cartelle,
     * la cartella corrente, la query di ricerca e gli elementi selezionati per produrre
     * la lista finale di `FileSystemItem` da visualizzare nell'interfaccia utente.
     * Questa logica determina quali elementi (file e cartelle) sono visibili in base
     * alla navigazione della cartella e ai criteri di ricerca, e aggiorna il loro stato di selezione.
     */
    val filteredAndDisplayedItems: StateFlow<List<FileSystemItem>> =
        combine(_allPdfFiles, _allFolders, _currentFolder, _searchQuery, _selectedItems) {
                allPdfs, allFolders, currentFolder, query, selectedItems ->
            val currentFolderId = currentFolder?.id ?: FileSystemDatasource.ROOT_FOLDER_ID
            val targetParentId = if (currentFolderId == FileSystemDatasource.ROOT_FOLDER_ID) {
                null
            } else {
                currentFolderId
            }

            val itemsInCurrentFolder = mutableListOf<FileSystemItem>()

            // Filtra e aggiunge le cartelle della cartella corrente, aggiornando lo stato di selezione
            allFolders.filter { it.parentFolderId == targetParentId }
                .sortedBy { it.displayName.lowercase() }
                .forEach { folder ->
                    val updatedFolder = folder.copy(isSelected = selectedItems.any { it.id == folder.id })
                    itemsInCurrentFolder.add(updatedFolder)
                }

            // Filtra e aggiunge i PDF della cartella corrente, aggiornando lo stato di selezione
            allPdfs.filter { it.parentFolderId == targetParentId }
                .sortedBy { it.displayName.lowercase() }
                .forEach { pdf ->
                    val updatedPdf = pdf.copy(isSelected = selectedItems.any { it.id == pdf.id })
                    itemsInCurrentFolder.add(updatedPdf)
                }

            // Applica il filtro di ricerca se la query non è vuota
            val finalFilteredList = if (query.isNotBlank()) {
                itemsInCurrentFolder.filter {
                    it.displayName.lowercase().contains(query.lowercase())
                }
            } else {
                itemsInCurrentFolder
            }
            finalFilteredList
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /**
     * Blocco di inizializzazione del ViewModel.
     * All'avvio, lancia una coroutine per caricare tutti i file PDF e le cartelle
     * persistenti utilizzando il `FileSystemDatasource`. Carica anche l'ID della cartella
     * corrente salvata per ripristinare l'ultima posizione dell'utente.
     */
    init {
        viewModelScope.launch {
            Log.d("FSViewModel", "INIT: Avvio caricamento dati.")
            _allPdfFiles.value = datasource.loadPdfFiles()
            _allFolders.value = datasource.loadFolders()

            val savedFolderId = datasource.loadCurrentFolderId()
            if (savedFolderId != FileSystemDatasource.ROOT_FOLDER_ID) {
                _currentFolder.value = _allFolders.value.find { it.id == savedFolderId }
                _currentFolder.value?.let {
                    if (it.id != FileSystemDatasource.ROOT_FOLDER_ID) {
                        folderNavigationStack.push(FileSystemDatasource.ROOT_FOLDER_ID)
                    }
                }
            }
            Log.d("FSViewModel", "INIT: Dati caricati. PDF: ${_allPdfFiles.value.size}, Cartelle: ${_allFolders.value.size}")
            Log.d("FSViewModel", "INIT: Cartella corrente ID: ${_currentFolder.value?.id ?: FileSystemDatasource.ROOT_FOLDER_ID}")
            Log.d("FSViewModel", "INIT: filteredAndDisplayedItems (dopo init): ${filteredAndDisplayedItems.value.size} elementi.")
        }
    }

    // --- Operazioni su FileSystemItem (PDF e Cartelle) ---

    /**
     * Alterna lo stato di selezione di un `FileSystemItem` (file PDF o cartella).
     * Se l'elemento è già selezionato, lo deseleziona; altrimenti, lo seleziona.
     * Aggiorna l'insieme `_selectedItems` e lo stato di `_isSelectionModeActive`.
     * Aggiorna anche l'attributo `isSelected` dell'elemento corrispondente nelle liste principali (`_allPdfFiles` o `_allFolders`).
     *
     * @param item L'elemento `FileSystemItem` di cui alternare lo stato di selezione.
     */
    fun toggleSelection(item: FileSystemItem) {
        val currentSelected = _selectedItems.value.toMutableSet()
        val isCurrentlySelected = currentSelected.any { it.id == item.id }

        if (isCurrentlySelected) {
            currentSelected.removeIf { it.id == item.id }
            Log.d("FSViewModel", "Deselezionato: ${item.displayName}. Totale selezionati: ${currentSelected.size}")
        } else {
            val newItem = when (item) {
                is PdfFileItem -> item.copy(isSelected = true)
                is FolderItem -> item.copy(isSelected = true)
                else -> {
                    Log.w("FSViewModel", "Tipo di FileSystemItem sconosciuto durante la selezione: ${item::class.simpleName}")
                    item
                }
            }
            currentSelected.add(newItem)
            Log.d("FSViewModel", "Selezionato: ${item.displayName}. Totale selezionati: ${currentSelected.size}")
        }
        _selectedItems.value = currentSelected

        // Aggiorna lo stato isSelected nell'elemento originale in _allPdfFiles o _allFolders
        when (item) {
            is PdfFileItem -> {
                _allPdfFiles.value = _allPdfFiles.value.map { pdf ->
                    if (pdf.id == item.id) pdf.copy(isSelected = !pdf.isSelected) else pdf
                }
            }
            is FolderItem -> {
                _allFolders.value = _allFolders.value.map { folder ->
                    if (folder.id == item.id) folder.copy(isSelected = !folder.isSelected) else folder
                }
            }
        }

        _isSelectionModeActive.value = _selectedItems.value.isNotEmpty()
        Log.d("FSViewModel", "toggleSelection: isSelectionModeActive dopo l'azione: ${_isSelectionModeActive.value}")
    }

    /**
     * Deseleziona tutti gli `FileSystemItem` precedentemente selezionati e disattiva la modalità di selezione.
     * Resetta l'insieme `_selectedItems` a vuoto e imposta `_isSelectionModeActive` a `false`.
     * Inoltre, aggiorna l'attributo `isSelected` a `false` per tutti i PDF e le cartelle nelle liste principali.
     */
    fun clearAllSelections() {
        Log.d("FSViewModel", "clearAllSelections: Deseleziono tutti gli elementi.")
        _selectedItems.value = emptySet()
        _isSelectionModeActive.value = false

        _allPdfFiles.value = _allPdfFiles.value.map { it.copy(isSelected = false) }
        _allFolders.value = _allFolders.value.map { it.copy(isSelected = false) }
        Log.d("FSViewModel", "clearAllSelections: isSelectionModeActive dopo l'azione: ${_isSelectionModeActive.value}")
    }

    /**
     * Elimina tutti gli `FileSystemItem` attualmente selezionati.
     * Questa operazione include la rimozione ricorsiva di cartelle e del loro contenuto.
     * Dopo l'eliminazione, le liste `_allPdfFiles` e `_allFolders` vengono aggiornate
     * e lo stato di selezione viene resettato. I cambiamenti vengono salvati nel `Datasource`.
     */
    fun deleteSelectedItems() {
        viewModelScope.launch {
            val selected = _selectedItems.value.toList()

            val newPdfFiles = _allPdfFiles.value.toMutableList()
            val newFolders = _allFolders.value.toMutableList()

            for (item in selected) {
                when (item) {
                    is PdfFileItem -> newPdfFiles.removeIf { it.id == item.id }
                    is FolderItem -> {
                        // Rimuove la cartella selezionata
                        newFolders.removeIf { it.id == item.id }
                        // Trova e rimuove ricorsivamente tutti gli elementi (PDF e sottocartelle) all'interno di questa cartella
                        val itemsToDelete = getItemsInFolderRecursive(item.id)
                        newPdfFiles.removeIf { pdf -> itemsToDelete.any { it.id == pdf.id } }
                        newFolders.removeIf { folder -> itemsToDelete.any { it.id == folder.id } }
                    }
                }
            }

            datasource.savePdfFiles(newPdfFiles)
            datasource.saveFolders(newFolders)

            _allPdfFiles.value = newPdfFiles
            _allFolders.value = newFolders
            clearAllSelections()
        }
    }

    /**
     * Funzione helper privata che recupera ricorsivamente tutti gli `FileSystemItem`
     * (file PDF e cartelle) contenuti all'interno di una specificata cartella e delle sue sottocartelle.
     * Utile per operazioni di eliminazione o spostamento ricorsive.
     *
     * @param folderId L'ID della cartella di cui si vogliono recuperare gli elementi interni.
     * @return Una `List<FileSystemItem>` contenente tutti gli elementi (file e cartelle)
     * diretti e indiretti all'interno della cartella specificata.
     */
    private fun getItemsInFolderRecursive(folderId: String): List<FileSystemItem> {
        val items = mutableListOf<FileSystemItem>()
        val directChildrenPdfs = _allPdfFiles.value.filter { it.parentFolderId == folderId }
        val directChildrenFolders = _allFolders.value.filter { it.parentFolderId == folderId }

        items.addAll(directChildrenPdfs)
        items.addAll(directChildrenFolders)

        for (childFolder in directChildrenFolders) {
            items.addAll(getItemsInFolderRecursive(childFolder.id))
        }
        return items
    }

    /**
     * Importa uno o più file PDF dagli URI forniti.
     * Per ogni URI, estrae il nome del file, crea un nuovo `PdfFileItem` e lo aggiunge
     * alla lista di tutti i PDF se non è già presente nella cartella corrente.
     * I nuovi PDF vengono associati all'ID della cartella corrente (o `null` per la root).
     * Dopo l'aggiunta, la lista `_allPdfFiles` viene aggiornata e i cambiamenti vengono salvati.
     *
     * @param uris Una `List<Uri>` rappresentante gli URI dei file PDF da importare.
     */
    fun importPdfs(uris: List<Uri>) {
        viewModelScope.launch {
            val currentAllPdfs = _allPdfFiles.value.toMutableList()
            val newPdfItems = mutableListOf<PdfFileItem>()
            val currentFolderId = _currentFolder.value?.id

            for (uri in uris) {
                val displayName = getFileName(uri)
                val newPdfItem = PdfFileItem(
                    uriString = uri.toString(),
                    displayName = displayName,
                    isSelected = false,
                    lastModified = System.currentTimeMillis(),
                    isFavorite = false,
                    parentFolderId = currentFolderId
                )

                if (!currentAllPdfs.any { existingPdf ->
                        existingPdf.uriString == newPdfItem.uriString && existingPdf.parentFolderId == newPdfItem.parentFolderId
                    }) {
                    newPdfItems.add(newPdfItem)
                    Log.d("FSViewModel", "Aggiunto nuovo PDF per l'importazione: ${newPdfItem.displayName} in cartella: ${currentFolderId ?: "Root"}")
                } else {
                    Log.d("FSViewModel", "PDF già presente nella cartella corrente: ${newPdfItem.displayName} (URI: ${newPdfItem.uriString})")
                }
            }

            if (newPdfItems.isNotEmpty()) {
                currentAllPdfs.addAll(newPdfItems)
                datasource.savePdfFiles(currentAllPdfs)
                _allPdfFiles.value = currentAllPdfs
                Log.d("FSViewModel", "Added ${newPdfItems.size} new PDF(s).")
            } else {
                Log.d("FSViewModel", "No new PDF to add.")
            }
        }
    }

    /**
     * Alterna lo stato di "preferito" per un singolo `PdfFileItem`.
     * Trova il PDF nella lista `_allPdfFiles`, ne inverte il flag `isFavorite`,
     * e aggiorna la lista. I cambiamenti vengono poi salvati nel `Datasource`.
     *
     * @param pdfFile L'oggetto `PdfFileItem` di cui alternare lo stato di preferito.
     */
    fun toggleFavorite(pdfFile: PdfFileItem) {
        viewModelScope.launch {
            Log.d("FSViewModel", "Toggling favorite for: ${pdfFile.displayName}, current favorite: ${pdfFile.isFavorite}")
            val updatedPdfs = _allPdfFiles.value.map {
                if (it.id == pdfFile.id) {
                    it.copy(isFavorite = !it.isFavorite)
                } else {
                    it
                }
            }
            _allPdfFiles.value = updatedPdfs
            datasource.savePdfFiles(updatedPdfs)
            Log.d("FSViewModel", "Favorite toggled. New state for ${pdfFile.displayName}: ${updatedPdfs.find { it.id == pdfFile.id }?.isFavorite}")
        }
    }

    /**
     * Alterna lo stato di "preferito" per tutti i `PdfFileItem` attualmente selezionati.
     * Itera sugli elementi selezionati, trova i corrispondenti PDF nella lista principale,
     * e ne inverte il flag `isFavorite`. Dopo l'aggiornamento, i cambiamenti vengono salvati
     * e tutte le selezioni vengono cancellate.
     */
    fun toggleFavoriteSelectedPdfs() {
        viewModelScope.launch {
            val currentPdfs = _allPdfFiles.value.toMutableList()
            val selectedPdfs = _selectedItems.value.filterIsInstance<PdfFileItem>()

            selectedPdfs.forEach { selectedPdf ->
                val index = currentPdfs.indexOfFirst { it.id == selectedPdf.id }
                if (index != -1) {
                    currentPdfs[index] = selectedPdf.copy(isFavorite = !selectedPdf.isFavorite)
                }
            }
            datasource.savePdfFiles(currentPdfs)
            _allPdfFiles.value = currentPdfs
            clearAllSelections()
        }
    }

    /**
     * Crea una nuova cartella con il nome specificato all'interno della cartella corrente.
     * Controlla se una cartella con lo stesso nome esiste già nella posizione corrente (case-insensitive).
     * Se il nome è unico, crea un nuovo `FolderItem`, lo aggiunge alla lista `_allFolders`,
     * e salva i cambiamenti nel `Datasource`.
     *
     * @param folderName Il nome da assegnare alla nuova cartella.
     */
    fun createNewFolder(folderName: String) {
        viewModelScope.launch {
            val currentFolders = _allFolders.value.toMutableList()
            val parentId = _currentFolder.value?.id

            if (currentFolders.any { it.displayName.equals(folderName, ignoreCase = true) && it.parentFolderId == parentId }) {
                Log.w("FSViewModel", "Cartella con nome '$folderName' esiste già in questa posizione.")
                showUserMessage("Una cartella con questo nome esiste già qui.")
                return@launch
            }

            val newFolder = FolderItem(
                displayName = folderName,
                parentFolderId = parentId
            )
            currentFolders.add(newFolder)
            datasource.saveFolders(currentFolders)
            _allFolders.value = currentFolders
            Log.d("FSViewModel", "Creata nuova cartella: ${newFolder.displayName} con ID: ${newFolder.id}")
        }
    }

    /**
     * Naviga all'interno della cartella specificata.
     * Prima di navigare, controlla se è attiva la modalità di spostamento e se la cartella
     * di destinazione è tra gli elementi che si stanno spostando (o una sua sottocartella),
     * impedendo la navigazione in tal caso.
     * Se la navigazione è permessa, l'ID della cartella corrente viene salvato nello stack
     * di navigazione, la `_currentFolder` viene aggiornata e l'ID della nuova cartella corrente
     * viene persistito. Tutte le selezioni correnti vengono cancellate.
     *
     * @param folder L'oggetto `FolderItem` della cartella in cui si desidera entrare.
     */
    fun enterFolder(folder: FolderItem) {

        if (_isMovingItems.value) {
            val itemsBeingMoved = _itemsToMove.value
            // Controlla se la cartella che si sta cercando di entrare è tra gli elementi da spostare
            // o se è una sottocartella di una cartella che si sta spostando.
            val isFolderBeingMoved = itemsBeingMoved.any { it.id == folder.id }
            val isChildOfFolderBeingMoved = itemsBeingMoved.filterIsInstance<FolderItem>().any { movedFolder ->
                getItemsInFolderRecursive(movedFolder.id).any { it.id == folder.id }
            }

            if (isFolderBeingMoved || isChildOfFolderBeingMoved) {
                viewModelScope.launch {
                    showUserMessage("Impossibile navigare in una cartella che stai spostando.")
                }
                Log.w("FSViewModel", "Tentativo di navigare in una cartella in fase di spostamento: ${folder.displayName}")
                return // Impedisci la navigazione
            }
        }

        val currentFolderId = _currentFolder.value?.id
        if (currentFolderId != null) {
            folderNavigationStack.push(currentFolderId)
        }
        _currentFolder.value = folder
        viewModelScope.launch {
            datasource.saveCurrentFolderId(folder.id)
        }
        clearAllSelections()
    }

    /**
     * Torna alla cartella precedente nella gerarchia di navigazione.
     * Recupera l'ID della cartella precedente dallo stack di navigazione.
     * Se lo stack è vuoto, significa che si sta tornando alla directory radice (null).
     * Aggiorna `_currentFolder` e salva l'ID della nuova cartella corrente.
     * Tutte le selezioni correnti vengono cancellate.
     */
    fun goBack() {
        if (folderNavigationStack.isNotEmpty()) {
            val previousFolderId = folderNavigationStack.pop()
            _currentFolder.value = _allFolders.value.find { it.id == previousFolderId }
            viewModelScope.launch {
                datasource.saveCurrentFolderId(previousFolderId)
            }
        } else {
            // Se lo stack è vuoto, siamo tornati alla root
            _currentFolder.value = null
            viewModelScope.launch {
                datasource.saveCurrentFolderId(FileSystemDatasource.ROOT_FOLDER_ID)
            }
        }
        clearAllSelections()
    }

    /**
     * Resetta la navigazione delle cartelle, riportando l'utente alla cartella root.
     * Svuota completamente lo stack di navigazione e imposta la cartella corrente a `null`
     * (che rappresenta la root). L'ID della root viene poi salvato come cartella corrente.
     * Tutte le selezioni vengono cancellate.
     */
    fun goToRoot() {
        Log.d("FSViewModel", "Navigando alla cartella root.")
        folderNavigationStack.clear()
        _currentFolder.value = null
        viewModelScope.launch {
            datasource.saveCurrentFolderId(FileSystemDatasource.ROOT_FOLDER_ID)
        }
        clearAllSelections()
    }

    /**
     * Esce dalla modalità di selezione multipla.
     * Questo metodo invoca `clearAllSelections()` che si occupa di deselezionare tutti gli elementi
     * e di impostare `_isSelectionModeActive` a `false`.
     */
    fun exitSelectionMode() {
        clearAllSelections()
    }

    /**
     * Inizia l'operazione di spostamento degli elementi.
     * Se ci sono elementi selezionati, li copia nella lista `_itemsToMove`,
     * imposta `_isMovingItems` a `true` e poi `clearAllSelections()` per uscire dalla modalità di selezione.
     * Se non ci sono elementi selezionati, viene mostrato un messaggio di avvertimento.
     */
    fun initiateMove() {
        if (_selectedItems.value.isNotEmpty()) {
            _itemsToMove.value = _selectedItems.value.toList()
            _isMovingItems.value = true
            clearAllSelections()
            Log.d("FSViewModel", "Iniziato spostamento. Elementi da spostare: ${_itemsToMove.value.size}")
        } else {
            Log.w("FSViewModel", "Tentativo di iniziare spostamento senza elementi selezionati.")
            showUserMessage("Seleziona gli elementi da spostare.")
        }
    }

    /**
     * Annulla l'operazione di spostamento corrente.
     * Svuota la lista degli elementi da spostare (`_itemsToMove`) e imposta `_isMovingItems` a `false`.
     */
    fun cancelMoveOperation() {
        Log.d("FSViewModel", "Operazione di spostamento annullata.")
        _itemsToMove.value = emptyList()
        _isMovingItems.value = false
    }

    /**
     * Sposta gli elementi precedentemente selezionati (e memorizzati in `_itemsToMove`)
     * nella cartella attualmente visualizzata (`_currentFolder`).
     * Gestisce la prevenzione dello spostamento di una cartella in se stessa o in una sua sottocartella,
     * e la prevenzione dello spostamento di elementi che già esistono nella cartella di destinazione.
     * Dopo lo spostamento, aggiorna le liste `_allPdfFiles` e `_allFolders` e salva i cambiamenti.
     */
    fun moveItemsToCurrentFolder() {
        viewModelScope.launch {
            val items = _itemsToMove.value.toList()
            if (items.isEmpty()) {
                Log.w("FSViewModel", "Nessun elemento da spostare.")
                cancelMoveOperation()
                return@launch
            }

            val destinationFolderId = _currentFolder.value?.id
            val destinationFolderName = _currentFolder.value?.displayName ?: "Root"
            Log.d("FSViewModel", "Tentativo di spostare ${items.size} elementi nella cartella: $destinationFolderName (${destinationFolderId ?: "Root"})")

            val updatedPdfs = _allPdfFiles.value.toMutableList()
            val updatedFolders = _allFolders.value.toMutableList()

            var movedCount = 0
            var skippedCount = 0

            for (item in items) {
                // Prevenire lo spostamento di una cartella in se stessa o nei suoi figli
                if (item is FolderItem && (item.id == destinationFolderId || getItemsInFolderRecursive(item.id).any { it.id == destinationFolderId })) {
                    Log.w("FSViewModel", "Impossibile spostare la cartella '${item.displayName}' in se stessa o in una sua sottocartella.")
                    skippedCount++
                    showUserMessage("Impossibile spostare '${item.displayName}' nella destinazione selezionata.")
                    continue
                }

                // Controlla se un elemento con lo stesso URI/nome esiste già nella cartella di destinazione
                val alreadyExistsInDestination = when (item) {
                    is PdfFileItem -> updatedPdfs.any { it.uriString == item.uriString && it.parentFolderId == destinationFolderId }
                    is FolderItem -> updatedFolders.any { it.displayName.equals(item.displayName, ignoreCase = true) && it.parentFolderId == destinationFolderId }
                    else -> false
                }

                if (alreadyExistsInDestination) {
                    Log.w("FSViewModel", "Elemento '${item.displayName}' già presente nella cartella di destinazione. Spostamento saltato.")
                    skippedCount++
                    showUserMessage("Elemento '${item.displayName}' già presente nella destinazione. Spostamento saltato.")
                    continue
                }

                // Esegui lo spostamento
                when (item) {
                    is PdfFileItem -> {
                        val index = updatedPdfs.indexOfFirst { it.id == item.id }
                        if (index != -1) {
                            updatedPdfs[index] = item.copy(parentFolderId = destinationFolderId)
                            movedCount++
                            Log.d("FSViewModel", "Spostato PDF '${item.displayName}' in '$destinationFolderName'.")
                        }
                    }
                    is FolderItem -> {
                        val index = updatedFolders.indexOfFirst { it.id == item.id }
                        if (index != -1) {
                            updatedFolders[index] = item.copy(parentFolderId = destinationFolderId)
                            movedCount++
                            Log.d("FSViewModel", "Spostata cartella '${item.displayName}' in '$destinationFolderName'.")
                        }
                    }
                }
            }

            // Salva i cambiamenti
            datasource.savePdfFiles(updatedPdfs)
            datasource.saveFolders(updatedFolders)

            // Aggiorna gli StateFlow
            _allPdfFiles.value = updatedPdfs
            _allFolders.value = updatedFolders

            Log.d("FSViewModel", "Operazione di spostamento completata. Spostati: $movedCount, Saltati: $skippedCount.")
            showUserMessage("Spostati $movedCount elementi. Saltati $skippedCount elementi.")
            cancelMoveOperation()
        }
    }

    /**
     * Imposta la stringa di ricerca per filtrare gli elementi visualizzati.
     * Questa stringa viene utilizzata nel `filteredAndDisplayedItems` `StateFlow`
     * per mostrare solo gli elementi il cui nome contiene la query.
     *
     * @param query La stringa di testo da utilizzare per la ricerca.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Funzione helper privata che recupera il nome del file da un dato `Uri`.
     * Tenta prima di ottenere il `DISPLAY_NAME` tramite il `ContentResolver`.
     * Se questo fallisce (es. per URI di file diretti), estrae il nome dal percorso URI.
     *
     * @param uri L'URI del file di cui recuperare il nome.
     * @return Il nome del file come `String`, o "Unknown PDF" se non è possibile determinarlo.
     */
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = cursor.getString(displayNameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "Unknown PDF"
    }

    /**
     * Factory personalizzato per `FileSystemViewModel`.
     * Questo è necessario quando il ViewModel richiede parametri nel suo costruttore (come `Application`),
     * poiché la creazione standard non supporta costruttori con argomenti.
     * Assicura che `FileSystemViewModel` sia istanziato correttamente.
     *
     * @param application L'istanza dell'applicazione da passare al ViewModel.
     */
    class FileSystemViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        /**
         * Crea una nuova istanza del ViewModel.
         *
         * @param modelClass La classe del ViewModel da creare.
         * @return Una nuova istanza di `FileSystemViewModel`.
         * @throws IllegalArgumentException se la `modelClass` fornita non è assegnabile a `FileSystemViewModel`.
         */
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FileSystemViewModel::class.java)) {
                return FileSystemViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}