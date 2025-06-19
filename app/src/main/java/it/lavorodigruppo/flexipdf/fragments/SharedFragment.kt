/**
 * @file SharedFragment.kt
 *
 * @brief Fragment che rappresenta la schermata dei file condivisi.
 *
 * Questo Fragment è un segnaposto per la futura implementazione della funzionalità
 * di gestione dei file PDF condivisi. Attualmente, carica solo il layout di base.
 *
 */
package it.lavorodigruppo.flexipdf.fragments

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.adapters.FileSystemAdapter
import it.lavorodigruppo.flexipdf.databinding.FragmentSharedBinding
import it.lavorodigruppo.flexipdf.items.FileSystemItem
import it.lavorodigruppo.flexipdf.items.FolderItem
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import java.util.Locale // Importa Locale per la ricerca case-insensitive


class SharedFragment : Fragment() {

    private var pdfFileClickListener: OnPdfFileClickListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPdfFileClickListener) {
            pdfFileClickListener = context
        } else {
            Log.e("SharedFragment", "$context deve implementare OnPdfFileClickListener per la gestione dei PDF.")
        }
    }

    private var _binding: FragmentSharedBinding? = null
    private val binding get() = _binding!!

    private lateinit var fileSystemAdapter: FileSystemAdapter

    // --- Stati interni del Fragment (simulano un ViewModel per semplicità in questo esempio) ---
    private var _currentCloudFolder = MutableStateFlow<FolderItem?>(null)
    private val currentCloudFolder: StateFlow<FolderItem?> = _currentCloudFolder.asStateFlow()

    private var _isSelectionModeActive = MutableStateFlow(false)
    private val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    private val _selectedItems = MutableStateFlow<MutableSet<FileSystemItem>>(mutableSetOf())
    private val selectedItems: StateFlow<Set<FileSystemItem>> = _selectedItems.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()


    // ActionMode per la selezione contestuale (CAB)
    private var actionMode: ActionMode? = null
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater: MenuInflater = mode!!.menuInflater
            inflater.inflate(R.menu.contextual_action_bar_menu, menu)

            menu?.findItem(R.id.action_move)?.isVisible = false
            menu?.findItem(R.id.action_move_here)?.isVisible = false
            menu?.findItem(R.id.action_move_back)?.isVisible = false
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val selectedCount = _selectedItems.value.size
            mode?.title = "$selectedCount" + getString(R.string.selected)

            val deleteItem = menu?.findItem(R.id.action_delete_selected)
            val favoriteItem = menu?.findItem(R.id.action_favorite_selected)

            deleteItem?.isVisible = true
            favoriteItem?.isVisible = _selectedItems.value.any { it is PdfFileItem }

            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.action_delete_selected -> {
                    showDeleteConfirmationDialog()
                    true
                }
                R.id.action_favorite_selected -> {
                    toggleFavoriteSelectedPdfs()
                    mode?.finish()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            clearAllSelections()
        }
    }

    // --- Dati di esempio (placeholder) per simulare le cartelle e i file cloud ---
    // NOTA: In una vera applicazione, questi dati verrebbero gestiti da un Repository
    private val cloudFoldersRoot = mutableListOf<FolderItem>(
        FolderItem(
            displayName = "Google Drive",
            isCloudFolder = true,
            cloudLinkParam = "google_drive_mock_id_1",
            id = UUID.randomUUID().toString(),
            isSelected = false,
            parentFolderId = null
        ),
        FolderItem(
            displayName = "Dropbox Sync",
            isCloudFolder = true,
            cloudLinkParam = "dropbox_mock_id_2",
            id = UUID.randomUUID().toString(),
            isSelected = false,
            parentFolderId = null
        ),
        FolderItem(
            displayName = "OneDrive Shared",
            isCloudFolder = true,
            cloudLinkParam = "onedrive_mock_id_3",
            id = UUID.randomUUID().toString(),
            isSelected = false,
            parentFolderId = null
        )
    )

    private val googleDriveContent = mutableListOf<FileSystemItem>(
        PdfFileItem(
            displayName = "Documento Condiviso 1.pdf",
            id = UUID.randomUUID().toString(),
            uriString = Uri.EMPTY.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis() - 86400000,
            isFavorite = false,
            parentFolderId = "google_drive_mock_id_1" // Aggiunto parentFolderId consistente
        ),
        PdfFileItem(
            displayName = "Report Q3.pdf",
            id = UUID.randomUUID().toString(),
            uriString = Uri.EMPTY.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis() - 172800000,
            isFavorite = true,
            parentFolderId = "google_drive_mock_id_1" // Aggiunto parentFolderId consistente
        ),
        FolderItem(
            displayName = "Sottocartella Drive",
            id = UUID.randomUUID().toString(),
            isSelected = false,
            parentFolderId = "google_drive_mock_id_1"
        )
    )
    private val dropboxContent = mutableListOf<FileSystemItem>(
        PdfFileItem(
            displayName = "Presentazione Client.pdf",
            id = UUID.randomUUID().toString(),
            uriString = Uri.EMPTY.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis() - 259200000,
            isFavorite = false,
            parentFolderId = "dropbox_mock_id_2" // Aggiunto parentFolderId consistente
        ),
        PdfFileItem(
            displayName = "Contratti 2024.pdf",
            id = UUID.randomUUID().toString(),
            uriString = Uri.EMPTY.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis() - 345600000,
            isFavorite = true,
            parentFolderId = "dropbox_mock_id_2" // Aggiunto parentFolderId consistente
        )
    )
    private val oneDriveContent = mutableListOf<FileSystemItem>(
        PdfFileItem(
            displayName = "Manuale Prodotto.pdf",
            id = UUID.randomUUID().toString(),
            uriString = Uri.EMPTY.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis() - 432000000,
            isFavorite = false,
            parentFolderId = "onedrive_mock_id_3" // Aggiunto parentFolderId consistente
        ),
        FolderItem(
            displayName = "Progetti Team",
            id = UUID.randomUUID().toString(),
            isSelected = false,
            parentFolderId = "onedrive_mock_id_3"
        )
    )
    private val nestedFolderContent = mutableListOf<FileSystemItem>(
        PdfFileItem(
            displayName = "File annidato 1.pdf",
            id = UUID.randomUUID().toString(),
            uriString = Uri.EMPTY.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis() - 604800000,
            isFavorite = false,
            parentFolderId = null // Se è una sottocartella senza genitore nel mock, lascialo null
        ),
        PdfFileItem(
            displayName = "File annidato 2.pdf",
            id = UUID.randomUUID().toString(),
            uriString = Uri.EMPTY.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis() - 691200000,
            isFavorite = false,
            parentFolderId = null // Se è una sottocartella senza genitore nel mock, lascialo null
        )
    )
    // --- Fine Dati di esempio ---


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSharedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        // NOTA: observeUIState ora include la combinazione di currentCloudFolder e searchQuery
        // Non è necessario chiamare updateRecyclerViewContent() qui, verrà chiamato dal collect.
        observeUIState()

        // --- GESTIONE WINDOW INSETS per il banner superiore, RecyclerView e FAB ---
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBarsInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.bannerContentLayout.setPadding(
                binding.bannerContentLayout.paddingLeft,
                systemBarsInsets.top,
                binding.bannerContentLayout.paddingRight,
                systemBarsInsets.bottom // Inset inferiore al banner per uniformità
            )

            val orientation = resources.configuration.orientation
            val bottomPaddingForRecyclerView = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                0
            } else {
                navigationBarsInsets.bottom // Usa solo navigationBarsInsets.bottom per la RecyclerView
            }

            binding.pdfRecyclerView.setPadding(
                systemBarsInsets.left,
                binding.pdfRecyclerView.paddingTop,
                systemBarsInsets.right,
                bottomPaddingForRecyclerView
            )

            val fabLayoutParams = binding.floatingActionButton.layoutParams as? ViewGroup.MarginLayoutParams
            if (fabLayoutParams != null) {
                val defaultMarginEnd = 16.dpToPx(requireContext())
                val defaultMarginBottom = 16.dpToPx(requireContext())

                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    fabLayoutParams.marginEnd = defaultMarginEnd + navigationBarsInsets.right
                    fabLayoutParams.bottomMargin = defaultMarginBottom
                } else {
                    fabLayoutParams.marginEnd = defaultMarginEnd
                    fabLayoutParams.bottomMargin = defaultMarginBottom + navigationBarsInsets.bottom
                }
                binding.floatingActionButton.layoutParams = fabLayoutParams
            }
            insets
        }
    }

    private fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    private fun setupRecyclerView() {
        fileSystemAdapter = FileSystemAdapter(
            onItemClick = { item -> handleItemClick(item) },
            onItemLongClick = { item -> handleItemLongClick(item) },
            onSelectionToggle = { item -> toggleSelection(item) },
            onFavoriteToggle = { pdfFile -> toggleFavorite(pdfFile) },
            onItemDoubleClick = { pdfFile -> handlePdfDoubleClick(pdfFile) }
        )
        binding.pdfRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = fileSystemAdapter
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupListeners() {
        binding.addButton.setOnClickListener {
            showAddCloudFolderDialog()
        }

        binding.backButton.setOnClickListener {
            if (_isSelectionModeActive.value) {
                exitSelectionMode()
            } else {
                navigateBack()
            }
        }

        binding.floatingActionButton.setOnClickListener {
            Toast.makeText(context, "FAB cliccato! (Aggiungi elemento a: ${_currentCloudFolder.value?.displayName ?: "radice"})", Toast.LENGTH_SHORT).show()
        }

        // Configurazione e listener per la SearchView
        val searchEditText = binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.let {
            it.background = null
            it.setTextColor(requireContext().getColor(android.R.color.white))
            it.setHintTextColor(requireContext().getColor(android.R.color.darker_gray))
            Log.d("SharedFragment", "SearchView EditText configurato.")
        } ?: run {
            Log.e("SharedFragment", "Errore: Impossibile trovare l'EditText interno della SearchView!")
        }

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                _searchQuery.value = query ?: ""
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                _searchQuery.value = newText ?: ""
                return true
            }
        })
    }

    private fun observeUIState() {
        // Combina lo stato della cartella corrente e la query di ricerca
        viewLifecycleOwner.lifecycleScope.launch {
            combine(currentCloudFolder, searchQuery.debounce(300)) { folder, query ->
                Pair(folder, query)
            }.collect { (folder, query) ->
                updateTitleAndButtonVisibility()
                updateRecyclerViewContent() // Questa funzione ora userà entrambi gli stati
                Log.d("SharedFragment", "Stato UI aggiornato: Cartella=${folder?.displayName ?: "Root Cloud"}, Query='$query'")
            }
        }

        // Osserva la modalità selezione per attivare/disattivare la CAB
        viewLifecycleOwner.lifecycleScope.launch {
            isSelectionModeActive.collect { isActive ->
                Log.d("SharedFragment", "isSelectionModeActive cambiato a: $isActive")
                if (isActive) {
                    if (actionMode == null) {
                        actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(actionModeCallback)
                    }
                    actionMode?.title = "${_selectedItems.value.size}" + getString(R.string.selected)
                    actionMode?.invalidate()
                } else {
                    actionMode?.finish()
                    Log.d("SharedFragment", "CAB chiusa perché non in modalità selezione.")
                }
                fileSystemAdapter.setSelectionMode(isActive)
            }
        }

        // Osserva gli elementi selezionati per aggiornare il titolo della CAB
        viewLifecycleOwner.lifecycleScope.launch {
            selectedItems.collect { selected ->
                actionMode?.title = "${selected.size}" + getString(R.string.selected)
                actionMode?.invalidate()
            }
        }
    }

    private fun handleItemClick(item: FileSystemItem) {
        if (_isSelectionModeActive.value) {
            toggleSelection(item)
            return
        }

        when (item) {
            is FolderItem -> {
                Log.d("SharedFragment", "Cartella cliccata: ${item.displayName}. isCloudFolder: ${item.isCloudFolder}")
                enterFolder(item)
            }
            is PdfFileItem -> {
                Log.d("SharedFragment", "PDF cliccato: ${item.displayName}. Notifico l'Activity tramite callback.")
                pdfFileClickListener?.onPdfFileClicked(item.uriString.toUri())
            }
        }
    }

    private fun handleItemLongClick(item: FileSystemItem): Boolean {
        toggleSelection(item)
        return true
    }

    private fun handlePdfDoubleClick(pdfFile: PdfFileItem) {
        pdfFileClickListener?.onPdfFileClickedForceActivity(pdfFile.uriString.toUri())
    }

    // --- NUOVA LOGICA: Aggiornamento stato sugli oggetti originali ---
    private fun toggleSelection(item: FileSystemItem) {
        // Trova la lista dati corrente
        val currentDataSource = getCurrentDataSource()
        val originalItem = currentDataSource?.find { it.id == item.id }

        if (originalItem != null) {
            // Aggiorna lo stato isSelected sull'oggetto originale nella lista dati
            when (originalItem) {
                is FolderItem -> originalItem.isSelected = !originalItem.isSelected
                is PdfFileItem -> originalItem.isSelected = !originalItem.isSelected
            }

            // Aggiorna il set di elementi selezionati
            if (originalItem.isSelected) {
                _selectedItems.value.add(originalItem)
            } else {
                _selectedItems.value.remove(originalItem)
            }

            // Forziamo l'aggiornamento dell'UI
            updateRecyclerViewContent()

            // Aggiorna lo stato della modalità selezione
            val hasSelection = _selectedItems.value.isNotEmpty()
            if (hasSelection && !_isSelectionModeActive.value) {
                _isSelectionModeActive.value = true
            } else if (!hasSelection && _isSelectionModeActive.value) {
                _isSelectionModeActive.value = false
            }
            actionMode?.invalidate()
        }
    }

    private fun clearAllSelections() {
        val currentDataSource = getCurrentDataSource()
        currentDataSource?.forEach { item ->
            if (item.isSelected) {
                when (item) {
                    is FolderItem -> item.isSelected = false
                    is PdfFileItem -> item.isSelected = false
                }
            }
        }
        _selectedItems.value.clear()
        _isSelectionModeActive.value = false
    }

    private fun exitSelectionMode() {
        clearAllSelections()
        actionMode?.finish()
    }

    // --- NUOVA LOGICA: Aggiornamento stato preferito sugli oggetti originali ---
    private fun toggleFavoriteSelectedPdfs() {
        val pdfsToToggle = _selectedItems.value.filterIsInstance<PdfFileItem>()
        val currentDataSource = getCurrentDataSource()

        pdfsToToggle.forEach { pdf ->
            val originalPdf = currentDataSource?.find { it.id == pdf.id } as? PdfFileItem
            originalPdf?.let {
                it.isFavorite = !it.isFavorite // Aggiorna lo stato sul PDF originale
            }
        }
        clearAllSelections()
        updateRecyclerViewContent() // Forziamo l'aggiornamento dell'UI
        Toast.makeText(context, "Stato preferito aggiornato (placeholder).", Toast.LENGTH_SHORT).show()
    }

    // Helper per ottenere la lista di dati corrente
    private fun getCurrentDataSource(): MutableList<FileSystemItem>? {
        return (if (_currentCloudFolder.value == null) {
            cloudFoldersRoot
        } else {
            when (_currentCloudFolder.value?.displayName) {
                "Google Drive" -> googleDriveContent
                "Dropbox Sync" -> dropboxContent
                "OneDrive Shared" -> oneDriveContent
                "Sottocartella Drive" -> nestedFolderContent
                "Progetti Team" -> nestedFolderContent
                else -> null
            }
        }) as MutableList<FileSystemItem>?
    }

    private fun updateTitleAndButtonVisibility() {
        val folder = _currentCloudFolder.value
        val title = folder?.displayName ?: getString(R.string.fragmentThree)
        binding.settingsTitleTextView.text = title

        if (folder != null) {
            binding.backButton.visibility = View.VISIBLE
            binding.addButton.visibility = View.GONE
            binding.floatingActionButton.visibility = View.VISIBLE
        } else {
            binding.backButton.visibility = View.GONE
            binding.addButton.visibility = View.VISIBLE
            binding.floatingActionButton.visibility = View.GONE
        }
        // La search view è sempre visibile in questo fragment
        binding.searchView.visibility = View.VISIBLE
    }

    private fun updateRecyclerViewContent() {
        val currentContent = if (_currentCloudFolder.value == null) {
            cloudFoldersRoot.toList()
        } else {
            // Ottieni la lista del contenuto della cartella corrente
            when (_currentCloudFolder.value?.displayName) {
                "Google Drive" -> googleDriveContent
                "Dropbox Sync" -> dropboxContent
                "OneDrive Shared" -> oneDriveContent
                "Sottocartella Drive" -> nestedFolderContent
                "Progetti Team" -> nestedFolderContent
                else -> emptyList()
            }.toList()
        }

        val filteredList = if (_searchQuery.value.isNotBlank()) {
            val query = _searchQuery.value.lowercase(Locale.getDefault())
            currentContent.filter { item ->
                item.displayName.lowercase(Locale.getDefault()).contains(query)
            }
        } else {
            currentContent
        }

        // Non è più necessario copiare gli elementi qui, perché gli aggiornamenti
        // di isSelected/isFavorite avvengono sugli oggetti originali nelle liste.
        // DiffUtil si accorgerà comunque delle modifiche grazie all'implementazione di equals()
        // nelle data class.
        fileSystemAdapter.submitList(filteredList)
    }

    private fun enterFolder(folder: FolderItem) {
        _currentCloudFolder.value = folder
        clearAllSelections()
        _searchQuery.value = "" // Resetta la query di ricerca quando cambi cartella
    }

    private fun navigateBack() {
        if (_currentCloudFolder.value != null) {
            _currentCloudFolder.value = null
            clearAllSelections()
            _searchQuery.value = "" // Resetta la query di ricerca quando torni indietro
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showAddCloudFolderDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.add_cloud))

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_cloud_folder, null)
        val folderNameEditText = view.findViewById<EditText>(R.id.folderNameEditText)
        val cloudParamEditText = view.findViewById<EditText>(R.id.cloudParamEditText)
        builder.setView(view)

        builder.setPositiveButton(getString(R.string.create)) { dialog, _ ->
            val folderName = folderNameEditText.text.toString().trim()
            val cloudParam = cloudParamEditText.text.toString().trim()

            if (folderName.isNotEmpty()) {
                val newCloudFolder = FolderItem(
                    displayName = folderName,
                    isCloudFolder = true,
                    cloudLinkParam = cloudParam,
                    parentFolderId = null,
                    id = UUID.randomUUID().toString(),
                    isSelected = false
                )
                cloudFoldersRoot.add(newCloudFolder)
                updateRecyclerViewContent()
                Toast.makeText(context, "Cartella cloud '$folderName' aggiunta (placeholder).", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Il nome della cartella non può essere vuoto.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.cancel()
        }
        builder.create().show()
    }

    private fun showDeleteConfirmationDialog() {
        val selectedCount = _selectedItems.value.size
        val messageResId = if (selectedCount == 1) {
            R.string.one_deletion_message
        } else {
            R.string.multiple_deletion_message
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_pdf_description))
            .setMessage(messageResId)
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                val itemsToDelete = _selectedItems.value.toList()
                val currentDataSource = getCurrentDataSource()

                currentDataSource?.removeAll(itemsToDelete) // Rimuove gli elementi dalle liste originali

                clearAllSelections() // Cancella le selezioni
                updateRecyclerViewContent() // Aggiorna la UI
                Toast.makeText(context, "Elementi selezionati eliminati (placeholder).", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // --- NUOVA LOGICA: Gestione preferiti per singolo PDF (chiamato dall'adapter) ---
    private fun toggleFavorite(pdfFile: PdfFileItem) {
        val currentDataSource = getCurrentDataSource()
        val originalPdf = currentDataSource?.find { it.id == pdfFile.id } as? PdfFileItem

        originalPdf?.let {
            it.isFavorite = !it.isFavorite // Togglie lo stato sull'oggetto originale
            updateRecyclerViewContent() // Aggiorna la UI per riflettere il cambiamento
            Toast.makeText(context, "Stato preferito di '${it.displayName}' togglato.", Toast.LENGTH_SHORT).show()
        }
        // Nota: non clearAllSelections() qui, solo se l'azione è stata fatta tramite CAB per PDF multipli
    }


    override fun onDestroyView() {
        super.onDestroyView()
        actionMode?.finish()
        actionMode = null
        _binding = null
        pdfFileClickListener = null
    }

    private fun rotateFabForward() {
        ObjectAnimator.ofFloat(binding.floatingActionButton, "rotation", 0f, 90f).apply {
            duration = 500
            interpolator = OvershootInterpolator()
            start()
        }
    }

    private fun rotateFabBackward() {
        ObjectAnimator.ofFloat(binding.floatingActionButton, "rotation", 90f, 0f).apply {
            duration = 500
            interpolator = OvershootInterpolator()
            start()
        }
    }
}