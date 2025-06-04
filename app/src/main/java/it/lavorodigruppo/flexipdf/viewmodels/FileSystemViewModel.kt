/**
 * @file PdfListViewModel.kt
 *
 * @overview
 *
 * PdfListViewModel è un viewModel che gestisce i dati della lista dei file PDF nell'applicazione.
 *
 * Estendendo AndroidViewModel, questo ViewModel ha accesso al contesto dell'applicazione
 * tramite il costruttore. AndroidViewModel è una sottoclasse di ViewModel che ha una differenza fondamentale:
 * il suo costruttore accetta un oggetto Application. L'oggetto Application rappresenta l'intero ciclo di vita
 * dell'applicazione. A differenza dell'Activity o del Fragment Context, l'Application Context dura per tutta
 * la vita dell'applicazione e non viene distrutto e ricreato durante i cambiamenti di configurazione.
 *
 * Il suo scopo è:
 * - Mantenere lo stato della lista dei PDF (pdfFiles)
 * - Esporre la lista dei PDF alla UI tramite LiveData, permettendo un aggiornamento reattivo
 * quando i dati cambiano.
 * - Centralizzare la logica di business relativa alla gestione della lista dei PDF
 * (aggiunta, rimozione, caricamento/salvataggio), separandola dalla logica dell'interfaccia utente.
 *
 *
 */

// it.lavorodigruppo.flexipdf.viewmodels/FileSystemViewModel.kt
// it.lavorodigruppo.flexipdf.viewmodels/FileSystemViewModel.kt
// it.lavorodigruppo.flexipdf.viewmodels/FileSystemViewModel.kt
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Stack

class FileSystemViewModel(application: Application) : AndroidViewModel(application) {

    private val datasource = FileSystemDatasource(application)

    private val _allPdfFiles = MutableStateFlow<List<PdfFileItem>>(emptyList())
    private val _allFolders = MutableStateFlow<List<FolderItem>>(emptyList())

    private val _currentFolder = MutableStateFlow<FolderItem?>(null)
    val currentFolder: StateFlow<FolderItem?> = _currentFolder.asStateFlow()

    private val folderNavigationStack = Stack<String>()

    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<FileSystemItem>>(emptySet()) // Usiamo Set per unicità
    val selectedItems: StateFlow<Set<FileSystemItem>> = _selectedItems.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private val _itemsToMove = MutableStateFlow<List<FileSystemItem>>(emptyList())
    val itemsToMove: StateFlow<List<FileSystemItem>> = _itemsToMove.asStateFlow()

    private val _isMovingItems = MutableStateFlow(false)
    val isMovingItems: StateFlow<Boolean> = _isMovingItems.asStateFlow()

    val recentPdfs: StateFlow<List<PdfFileItem>> = _allPdfFiles.map { allFiles ->
        allFiles.sortedByDescending { it.lastModified }.take(15)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val favoritePdfs: StateFlow<List<PdfFileItem>> = _allPdfFiles.map { allFiles ->
        allFiles.filter { it.isFavorite }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

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

            allFolders.filter { it.parentFolderId == targetParentId }
                .sortedBy { it.displayName.lowercase() }
                .forEach { folder ->
                    val updatedFolder = folder.copy(isSelected = selectedItems.any { it.id == folder.id })
                    itemsInCurrentFolder.add(updatedFolder)
                }

            allPdfs.filter { it.parentFolderId == targetParentId }
                .sortedBy { it.displayName.lowercase() }
                .forEach { pdf ->
                    val updatedPdf = pdf.copy(isSelected = selectedItems.any { it.id == pdf.id })
                    itemsInCurrentFolder.add(updatedPdf)
                }

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
                else -> { // <--- AGGIUNTO IL RAMO ELSE
                    Log.w("FSViewModel", "Tipo di FileSystemItem sconosciuto durante la selezione: ${item::class.simpleName}")
                    item // Restituisce l'elemento originale senza modifiche
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
            // Non è necessario un else qui, perché stiamo solo aggiornando liste specifiche
        }

        _isSelectionModeActive.value = _selectedItems.value.isNotEmpty()
        Log.d("FSViewModel", "toggleSelection: isSelectionModeActive dopo l'azione: ${_isSelectionModeActive.value}")
    }

    fun clearAllSelections() {
        Log.d("FSViewModel", "clearAllSelections: Deseleziono tutti gli elementi.")
        _selectedItems.value = emptySet()
        _isSelectionModeActive.value = false

        _allPdfFiles.value = _allPdfFiles.value.map { it.copy(isSelected = false) }
        _allFolders.value = _allFolders.value.map { it.copy(isSelected = false) }
        Log.d("FSViewModel", "clearAllSelections: isSelectionModeActive dopo l'azione: ${_isSelectionModeActive.value}")
    }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            val selected = _selectedItems.value.toList()

            val newPdfFiles = _allPdfFiles.value.toMutableList()
            val newFolders = _allFolders.value.toMutableList()

            for (item in selected) {
                when (item) {
                    is PdfFileItem -> newPdfFiles.removeIf { it.id == item.id }
                    is FolderItem -> {
                        newFolders.removeIf { it.id == item.id }
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

    fun createNewFolder(folderName: String) {
        viewModelScope.launch {
            val currentFolders = _allFolders.value.toMutableList()
            val parentId = _currentFolder.value?.id

            if (currentFolders.any { it.displayName.equals(folderName, ignoreCase = true) && it.parentFolderId == parentId }) {
                Log.w("FSViewModel", "Cartella con nome '$folderName' esiste già in questa posizione.")
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
                    val userMessage = MutableSharedFlow<String>()
                    userMessage.emit("Impossibile navigare in una cartella che stai spostando.")
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
     * Resetta la navigazione delle cartelle alla cartella root.
     * Svuota lo stack di navigazione e imposta la cartella corrente a null.
     */
    fun goToRoot() {
        Log.d("FSViewModel", "Navigando alla cartella root.")
        folderNavigationStack.clear() // Svuota lo stack
        _currentFolder.value = null // Imposta la cartella corrente a null (rappresenta la root)
        viewModelScope.launch {
            datasource.saveCurrentFolderId(FileSystemDatasource.ROOT_FOLDER_ID) // Salva l'ID della root
        }
        clearAllSelections() // Deseleziona tutto quando si cambia cartella
    }

    fun exitSelectionMode() {
        clearAllSelections()
        // _isSelectionModeActive.value = false // Già impostato a false in clearAllSelections
    }

    fun initiateMove() {
        if (_selectedItems.value.isNotEmpty()) {
            _itemsToMove.value = _selectedItems.value.toList() // Copia gli elementi selezionati
            _isMovingItems.value = true // Attiva la modalità di spostamento
            clearAllSelections() // Disattiva la modalità di selezione
            Log.d("FSViewModel", "Iniziato spostamento. Elementi da spostare: ${_itemsToMove.value.size}")
        } else {
            Log.w("FSViewModel", "Tentativo di iniziare spostamento senza elementi selezionati.")
        }
    }

    fun cancelMoveOperation() {
        Log.d("FSViewModel", "Operazione di spostamento annullata.")
        _itemsToMove.value = emptyList() // Svuota la lista
        _isMovingItems.value = false // Disattiva la modalità di spostamento
    }

    /**
     * Sposta gli elementi precedentemente selezionati (in _itemsToMove) nella cartella corrente.
     * Gestisce anche la prevenzione dello spostamento di una cartella in se stessa o nei suoi figli.
     */
    fun moveItemsToCurrentFolder() {
        viewModelScope.launch {
            val items = _itemsToMove.value.toList() // Copia per evitare modifiche concorrenti
            if (items.isEmpty()) {
                Log.w("FSViewModel", "Nessun elemento da spostare.")
                cancelMoveOperation()
                return@launch
            }

            val destinationFolderId = _currentFolder.value?.id // ID della cartella di destinazione
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
            cancelMoveOperation() // Termina l'operazione di spostamento
        }
    }


    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

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

    class FileSystemViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FileSystemViewModel::class.java)) {
                return FileSystemViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}