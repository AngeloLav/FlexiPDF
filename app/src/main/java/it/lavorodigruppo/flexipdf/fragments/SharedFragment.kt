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
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import android.widget.TextView
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
import java.util.Locale
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns // Necessario per ottenere il nome del file da una Uri

class SharedFragment : Fragment() {

    private var pdfFileClickListener: OnPdfFileClickListener? = null

    private val pdfPickerLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                addPdfToCurrentCloudFolder(it)
                Toast.makeText(context, "PDF importato nella cartella cloud corrente.", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(context, "Nessun PDF selezionato.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPdfFileClickListener) {
            pdfFileClickListener = context
        } else {
            Log.e("SharedFragment", "$context deve implementare OnPdfFileClickListener.")
        }
    }

    private var _binding: FragmentSharedBinding? = null
    private val binding get() = _binding!!

    private lateinit var fileSystemAdapter: FileSystemAdapter

    private var _currentCloudFolder = MutableStateFlow<FolderItem?>(null)
    private val currentCloudFolder: StateFlow<FolderItem?> = _currentCloudFolder.asStateFlow()

    private var _isSelectionModeActive = MutableStateFlow(false)
    private val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    private val _selectedItems = MutableStateFlow<MutableSet<FileSystemItem>>(mutableSetOf())
    private val selectedItems: StateFlow<Set<FileSystemItem>> = _selectedItems.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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

    // --- Dati di esempio (placeholder) ---
    private val cloudFoldersRoot = mutableListOf<FileSystemItem>(
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
            parentFolderId = "google_drive_mock_id_1"
        ),
        PdfFileItem(
            displayName = "Report Q3.pdf",
            id = UUID.randomUUID().toString(),
            uriString = Uri.EMPTY.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis() - 172800000,
            isFavorite = true,
            parentFolderId = "google_drive_mock_id_1"
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
            parentFolderId = "dropbox_mock_id_2"
        ),
        PdfFileItem(
            displayName = "Contratti 2024.pdf",
            id = UUID.randomUUID().toString(),
            uriString = Uri.EMPTY.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis() - 345600000,
            isFavorite = true,
            parentFolderId = "dropbox_mock_id_2"
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
            parentFolderId = "onedrive_mock_id_3"
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
            parentFolderId = null
        ),
        PdfFileItem(
            displayName = "File annidato 2.pdf",
            id = UUID.randomUUID().toString(),
            uriString = Uri.EMPTY.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis() - 691200000,
            isFavorite = false,
            parentFolderId = null
        )
    )

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
        observeUIState()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBarsInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.bannerContentLayout.setPadding(
                binding.bannerContentLayout.paddingLeft,
                systemBarsInsets.top,
                binding.bannerContentLayout.paddingRight,
                systemBarsInsets.bottom
            )

            val orientation = resources.configuration.orientation
            val bottomPaddingForRecyclerView = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                0
            } else {
                navigationBarsInsets.bottom
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

    private fun toggleFavorite(pdfFile: PdfFileItem) {
        val currentDataSource = getCurrentDataSource()
        val originalPdf = currentDataSource?.find { it.id == pdfFile.id } as? PdfFileItem

        originalPdf?.let {
            it.isFavorite = !it.isFavorite
            updateRecyclerViewContent()
            Toast.makeText(context, "Stato preferito di '${it.displayName}' togglato.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupListeners() {
        binding.addButton.setOnClickListener {
            showAddCloudFolderDialog(isCreatingRootCloudFolder = true) // Chiamata per la creazione di cartella cloud root
        }

        binding.backButton.setOnClickListener {
            if (_isSelectionModeActive.value) {
                exitSelectionMode()
            } else {
                navigateBack()
            }
        }

        binding.floatingActionButton.setOnClickListener {
            rotateFabForward()
            showPopupMenu()
        }

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
        viewLifecycleOwner.lifecycleScope.launch {
            combine(currentCloudFolder, searchQuery.debounce(300)) { folder, query ->
                Pair(folder, query)
            }.collect { (folder, query) ->
                updateTitleAndButtonVisibility()
                updateRecyclerViewContent()
                Log.d("SharedFragment", "Stato UI aggiornato: Cartella=${folder?.displayName ?: "Root Cloud"}, Query='$query'")
            }
        }

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

    private fun toggleSelection(item: FileSystemItem) {
        val currentDataSource = getCurrentDataSource()
        val originalItem = currentDataSource?.find { it.id == item.id }

        if (originalItem != null) {
            when (originalItem) {
                is FolderItem -> originalItem.isSelected = !originalItem.isSelected
                is PdfFileItem -> originalItem.isSelected = !originalItem.isSelected
            }

            if (originalItem.isSelected) {
                _selectedItems.value.add(originalItem)
            } else {
                _selectedItems.value.remove(originalItem)
            }

            updateRecyclerViewContent()

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

    private fun toggleFavoriteSelectedPdfs() {
        val pdfsToToggle = _selectedItems.value.filterIsInstance<PdfFileItem>()
        val currentDataSource = getCurrentDataSource()

        pdfsToToggle.forEach { pdf ->
            val originalPdf = currentDataSource?.find { it.id == pdf.id } as? PdfFileItem
            originalPdf?.let {
                it.isFavorite = !it.isFavorite
            }
        }
        clearAllSelections()
        updateRecyclerViewContent()
        Toast.makeText(context, "Stato preferito aggiornato (placeholder).", Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentDataSource(): MutableList<FileSystemItem>? {
        return if (_currentCloudFolder.value == null) {
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
        }
    }

    // --- NUOVO: Funzione per ottenere il nome del file da una Uri ---
    private fun getFileNameFromUri(uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context?.contentResolver?.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/')
            if (cut != -1) {
                name = name?.substring(cut!! + 1)
            }
        }
        return name ?: "Documento senza nome.pdf"
    }
    // --- FINE NUOVO ---

    private fun addPdfToCurrentCloudFolder(uri: Uri) {
        val newPdf = PdfFileItem(
            displayName = getFileNameFromUri(uri), // Usa la nuova funzione
            id = UUID.randomUUID().toString(),
            uriString = uri.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis(),
            isFavorite = false,
            parentFolderId = _currentCloudFolder.value?.id // ID della cartella cloud corrente, o null
        )

        val targetList = getCurrentDataSource()

        targetList?.add(newPdf)
        updateRecyclerViewContent()
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
        binding.searchView.visibility = View.VISIBLE
    }

    private fun updateRecyclerViewContent() {
        val currentContent = getCurrentDataSource()?.toList() ?: emptyList()

        val filteredList = if (_searchQuery.value.isNotBlank()) {
            val query = _searchQuery.value.lowercase(Locale.getDefault())
            currentContent.filter { item ->
                item.displayName.lowercase(Locale.getDefault()).contains(query)
            }
        } else {
            currentContent
        }

        fileSystemAdapter.submitList(filteredList)
    }

    private fun enterFolder(folder: FolderItem) {
        _currentCloudFolder.value = folder
        clearAllSelections()
        _searchQuery.value = ""
        binding.searchView.setQuery("", false)
    }

    private fun navigateBack() {
        if (_currentCloudFolder.value != null) {
            _currentCloudFolder.value = null
            clearAllSelections()
            _searchQuery.value = ""
            binding.searchView.setQuery("", false)
        }
    }

    @SuppressLint("MissingInflatedId")
    // Rinominato il parametro per maggiore chiarezza sul contesto di creazione
    private fun showAddCloudFolderDialog(isCreatingRootCloudFolder: Boolean = false) {
        val builder = AlertDialog.Builder(requireContext())
        // Modifica il titolo in base al contesto
        builder.setTitle(if (isCreatingRootCloudFolder) getString(R.string.add_cloud) else "Crea Nuova Cartella")

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_cloud_folder, null)
        val folderNameEditText = view.findViewById<EditText>(R.id.folderNameEditText)
        val cloudParamEditText = view.findViewById<EditText>(R.id.cloudParamEditText)
        builder.setView(view)

        // Nascondi o mostra il campo "Cloud Parameter" in base al contesto
        if (isCreatingRootCloudFolder) {
            cloudParamEditText.visibility = View.VISIBLE
        } else {
            cloudParamEditText.visibility = View.GONE
        }

        builder.setPositiveButton(getString(R.string.create)) { dialog, _ ->
            val folderName = folderNameEditText.text.toString().trim()
            val cloudParam = if (isCreatingRootCloudFolder) cloudParamEditText.text.toString().trim() else null

            if (folderName.isNotEmpty()) {
                val newCloudFolder = FolderItem(
                    displayName = folderName,
                    // isCloudFolder dipende dal contesto di creazione
                    isCloudFolder = isCreatingRootCloudFolder,
                    cloudLinkParam = cloudParam,
                    parentFolderId = if (isCreatingRootCloudFolder) null else _currentCloudFolder.value?.id,
                    id = UUID.randomUUID().toString(),
                    isSelected = false
                )

                val targetList = getCurrentDataSource()
                targetList?.add(newCloudFolder)

                updateRecyclerViewContent()
                Toast.makeText(context, "Cartella '$folderName' aggiunta (placeholder).", Toast.LENGTH_SHORT).show()
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

                currentDataSource?.removeAll(itemsToDelete)

                clearAllSelections()
                updateRecyclerViewContent()
                Toast.makeText(context, "Elementi selezionati eliminati (placeholder).", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
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

    private fun showPopupMenu() {
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_popup_menu, null)

        val optionImportPdf = popupView.findViewById<TextView>(R.id.option_import_pdf)
        val optionCreateFolder = popupView.findViewById<TextView>(R.id.option_create_folder)

        val popupWindow = android.widget.PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val location = IntArray(2)
        binding.floatingActionButton.getLocationOnScreen(location)

        val fabX = location[0]
        val fabY = location[1]

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight
        val fabWidth = binding.floatingActionButton.width
        val fabHeight = binding.floatingActionButton.height


        val xOffset = fabX - popupWidth + fabWidth / 2
        val yOffset = fabY - popupHeight - (fabHeight / 2)

        popupWindow.showAtLocation(
            binding.root,
            android.view.Gravity.NO_GRAVITY,
            xOffset,
            yOffset
        )

        optionImportPdf.setOnClickListener {
            pdfPickerLauncher.launch("application/pdf")
            popupWindow.dismiss()
        }

        optionCreateFolder.setOnClickListener {
            // Chiamata per la creazione di una sottocartella (non una cloud folder root)
            showAddCloudFolderDialog(isCreatingRootCloudFolder = false)
            popupWindow.dismiss()
        }

        popupWindow.setOnDismissListener {
            rotateFabBackward()
        }
    }
}