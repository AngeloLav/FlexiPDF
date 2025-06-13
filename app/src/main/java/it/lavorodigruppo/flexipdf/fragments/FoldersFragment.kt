/**
 * @file FoldersFragment.kt
 *
 * @brief Fragment che gestisce la visualizzazione della lista dei file PDF e cartelle.
 *
 * Questo Fragment è il cuore della sezione "Cartelle" dell'applicazione.
 * - Visualizza una lista di PdfFileItem utilizzando una RecyclerView e PdfFileAdapter.
 * - Interagisce con il PdfListViewModel per recuperare e osservare i dati dei PDF.
 * - Gestisce l'interazione con un FloatingActionButton per mostrare un popup menu per azioni rapide (importazione, creazione cartelle).
 * - Implementa la gestione degli WindowInsets per adattare il layout alle barre di sistema.
 * - Definisce un'interfaccia di callback (OnPdfPickerListener) per comunicare con l'Activity ospitante
 * per l'avvio del selettore di file PDF.
 * - Gestisce l'apertura del PDFViewerActivity al click su un elemento PDF.
 *
 */
package it.lavorodigruppo.flexipdf.fragments

import android.animation.ObjectAnimator
import android.content.Intent
import androidx.core.net.toUri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.activities.PDFViewerActivity
import it.lavorodigruppo.flexipdf.adapters.FileSystemAdapter
import it.lavorodigruppo.flexipdf.databinding.FragmentFoldersBinding // Assicurati che il nome del binding sia corretto
import it.lavorodigruppo.flexipdf.items.FolderItem
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import it.lavorodigruppo.flexipdf.viewmodels.FileSystemViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.widget.EditText
import com.google.android.material.snackbar.Snackbar
import it.lavorodigruppo.flexipdf.data.FileSystemDatasource

class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    private lateinit var fileSystemViewModel: FileSystemViewModel
    private lateinit var fileSystemAdapter: FileSystemAdapter

    private var actionMode: ActionMode? = null
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater: MenuInflater = mode!!.menuInflater
            inflater.inflate(R.menu.contextual_action_bar_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val isMoving = fileSystemViewModel.isMovingItems.value
            val selectedCount = fileSystemViewModel.selectedItems.value.size
            val itemsToMoveCount = fileSystemViewModel.itemsToMove.value.size

            // Voci di menu per la modalità di selezione standard
            val deleteItem = menu?.findItem(R.id.action_delete_selected)
            val favoriteItem = menu?.findItem(R.id.action_favorite_selected)
            val moveItem = menu?.findItem(R.id.action_move)

            // Voci di menu per la modalità di spostamento
            val moveHereItem = menu?.findItem(R.id.action_move_here)
            val cancelMoveItem = menu?.findItem(R.id.action_cancel_move)
            val moveBackItem = menu?.findItem(R.id.action_move_back)

            if (isMoving) {
                // Modalità di spostamento: mostra "Sposta qui" e "Annulla"
                deleteItem?.isVisible = false
                favoriteItem?.isVisible = false
                moveItem?.isVisible = false

                moveHereItem?.isVisible = true
                cancelMoveItem?.isVisible = true


                // Rendi visibile "Indietro" solo se non siamo nella root
                moveBackItem?.isVisible = fileSystemViewModel.currentFolder.value != null

                mode?.title = resources.getQuantityString(R.plurals.move_items_message, itemsToMoveCount, itemsToMoveCount)
            } else {
                // Modalità di selezione standard: mostra "Elimina", "Preferito", "Sposta"
                deleteItem?.isVisible = true
                favoriteItem?.isVisible = fileSystemViewModel.selectedItems.value.any { it is PdfFileItem }
                moveItem?.isVisible = true

                moveHereItem?.isVisible = false
                cancelMoveItem?.isVisible = false
                moveBackItem?.isVisible = false

                mode?.title = "$selectedCount" + getString(R.string.selected)

                // Aggiorna il titolo di "Seleziona tutto"
                val currentFolderId = fileSystemViewModel.currentFolder.value?.id ?: FileSystemDatasource.ROOT_FOLDER_ID
              //  val allItemsInCurrentFolder = fileSystemViewModel.filteredAndDisplayedItems.value.filter { it.parentFolderId == currentFolderId }

            }
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.action_delete_selected -> {
                    showDeleteConfirmationDialog()
                    true
                }
                R.id.action_favorite_selected -> {
                    fileSystemViewModel.toggleFavoriteSelectedPdfs()
                    mode?.finish()
                    true
                }
                R.id.action_move -> {
                    fileSystemViewModel.initiateMove() // Inizia l'operazione di spostamento
                    mode?.invalidate() // Invalida la CAB per mostrare le nuove opzioni
                    Snackbar.make(binding.root, getString(R.string.moving_items), Snackbar.LENGTH_LONG).show()
                    true
                }
                R.id.action_move_here -> { // GESTIONE "SPOSTA QUI"
                    fileSystemViewModel.moveItemsToCurrentFolder()
                    Snackbar.make(binding.root, getString(R.string.successful_moving_operation), Snackbar.LENGTH_SHORT).show()
                    mode?.finish()
                    true
                }
                R.id.action_cancel_move -> { // GESTIONE "ANNULLA SPOSTAMENTO"
                    fileSystemViewModel.cancelMoveOperation()
                    Snackbar.make(binding.root, getString(R.string.failed_moving_operation), Snackbar.LENGTH_SHORT).show()
                    mode?.finish() // Chiude la CAB
                    true
                }
                R.id.action_move_back -> { // GESTIONE "INDIETRO" IN MODALITÀ SPOSTAMENTO
                    fileSystemViewModel.goBack()
                    mode?.invalidate() // Invalida la CAB per aggiornare la visibilità del pulsante "Indietro" (se si torna alla root)
                    true
                }
                else -> false
            }
        }


        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            fileSystemViewModel.clearAllSelections()
            fileSystemViewModel.cancelMoveOperation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileSystemViewModel = ViewModelProvider(requireActivity(),
            FileSystemViewModel.FileSystemViewModelFactory(requireActivity().application)
        )[FileSystemViewModel::class.java]

        fileSystemViewModel.goToRoot()

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        fileSystemAdapter = FileSystemAdapter(
            onItemClick = onItemClick@{ item ->

                if (fileSystemViewModel.isMovingItems.value) {
                    if (item is PdfFileItem) {
                        // Se siamo in modalità spostamento e clicchiamo un PDF, non aprirlo.
                        // Invece, mostra un messaggio all'utente chiamando il ViewModel.
                        fileSystemViewModel.showUserMessage("Clicca su una cartella per navigare o 'Sposta qui' per confermare.")
                        Log.d("FoldersFragment", "Tentativo di aprire PDF in modalità spostamento, impedito: ${item.displayName}")
                        return@onItemClick // Esci dalla lambda onItemClick
                    }
                    // Se è una cartella in modalità spostamento, permetti la navigazione
                    // La logica di enterFolder gestirà se la navigazione è permessa o meno.
                    else if (item is FolderItem) {
                        fileSystemViewModel.enterFolder(item)
                        return@onItemClick // Esci dalla lambda onItemClick dopo aver gestito la cartella
                    }
                }

                // Questa parte del codice viene eseguita solo se:
                // 1. Non siamo in modalità spostamento (isMovingItems.value è false)
                // 2. Siamo in modalità spostamento, ma l'elemento cliccato non è un PdfFileItem
                //    (cioè, è una FolderItem e la navigazione è stata permessa sopra).
                when (item) {
                    is PdfFileItem -> {
                        val pdfUri = item.uriString.toUri()
                        Log.d("FoldersFragment", "Tentativo di aprire PDF con URI: $pdfUri")

                        val intent = Intent(requireContext(), PDFViewerActivity::class.java).apply {
                            putExtra("pdf_uri", pdfUri)
                            putExtra("pdf_display_name", item.displayName)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                        Toast.makeText(requireContext(), "Apertura PDF: ${item.displayName}", Toast.LENGTH_SHORT).show()
                    }
                    is FolderItem -> {
                        // Questa parte verrà eseguita solo se non eravamo in modalità spostamento
                        // o se la navigazione in modalità spostamento è stata gestita sopra.
                        fileSystemViewModel.enterFolder(item)
                    }
                }
            },
            onItemLongClick = { item ->
                fileSystemViewModel.toggleSelection(item)
                true
            },
            onSelectionToggle = { item ->
                fileSystemViewModel.toggleSelection(item)
            },
            onFavoriteToggle = { pdfFile ->
                Log.d("FoldersFragment", "Received favorite toggle for: ${pdfFile.displayName}")
                fileSystemViewModel.toggleFavorite(pdfFile)
            }
        )

        binding.pdfRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = fileSystemAdapter
        }
    }

    private fun setupListeners() {
        // --- RIPRISTINATO IL COMPORTAMENTO DEL FAB ---
        binding.floatingActionButton.setOnClickListener {
            rotateFabForward()
            showPopupMenu()
        }

        val searchEditText = binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.let {
            it.background = null // Questo è cruciale per rimuovere la sottolineatura predefinita
            it.setTextColor(requireContext().getColor(android.R.color.white)) // <--- CAMBIATO QUI: Testo bianco
            it.setHintTextColor(requireContext().getColor(android.R.color.darker_gray)) // Suggerimento grigio scuro
            Log.d("FoldersFragment", "SearchView EditText configurato (background, testo, suggerimento).")
        } ?: run {
            Log.e("FoldersFragment", "Errore: Impossibile trovare l'EditText interno della SearchView!")
        }

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                fileSystemViewModel.setSearchQuery(query ?: "")
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                fileSystemViewModel.setSearchQuery(newText ?: "")
                return true
            }
        })

        binding.backButton.setOnClickListener {
            if (fileSystemViewModel.isSelectionModeActive.value) {
                fileSystemViewModel.exitSelectionMode()
            } else {
                fileSystemViewModel.goBack()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            fileSystemViewModel.filteredAndDisplayedItems.collectLatest { items ->
                Log.d("FoldersFragment", "Aggiornamento lista: ${items.size} elementi.")
                Log.d("FoldersFragment", "Adapter riceve lista: ${items.size} elementi.")
                fileSystemAdapter.submitList(items)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            fileSystemViewModel.currentFolder.collectLatest { folder ->
                val title = folder?.displayName ?: getString(R.string.fragmentTwo)
                binding.settingsTitleTextView.text = title

                if (folder != null) {
                    binding.backButton.visibility = View.VISIBLE
                } else {
                    binding.backButton.visibility = View.GONE
                }
                Log.d("FoldersFragment", "Cartella corrente cambiata: ${folder?.displayName ?: "Root"}")

                if (actionMode != null) {
                    actionMode?.invalidate() // Forza la CAB a ri-valutare le voci di menu (es. pulsante "Indietro")
                    Log.d("FoldersFragment", "CAB invalidata a causa del cambio cartella.")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            fileSystemViewModel.isSelectionModeActive.collectLatest { isActive ->
                Log.d("FoldersFragment", "isSelectionModeActive cambiato a: $isActive")
                if (isActive) {
                    if (actionMode == null) {
                        actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(actionModeCallback)
                    }
                    actionMode?.title = "${fileSystemViewModel.selectedItems.value.size}" + getString(R.string.selected)
                    actionMode?.invalidate() // Invalida per aggiornare le voci di menu
                } else {
                    // Chiude la CAB solo se NON siamo in modalità di spostamento
                    if (!fileSystemViewModel.isMovingItems.value) { // <--- AGGIUNTA QUESTA CONDIZIONE
                        actionMode?.finish()
                        Log.d("FoldersFragment", "CAB chiusa perché non in modalità selezione e non in modalità spostamento.")
                    } else {
                        Log.d("FoldersFragment", "CAB non chiusa perché in modalità spostamento.")
                    }
                }
                fileSystemAdapter.setSelectionMode(isActive)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            fileSystemViewModel.selectedItems.collectLatest { selectedItems ->
                actionMode?.title = "${selectedItems.size}" + getString(R.string.selected)
                actionMode?.invalidate()
            }
        }
    }

    private fun showNewFolderDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.new_folder)

        val input = EditText(requireContext())
        builder.setView(input)

        builder.setPositiveButton(R.string.create) { dialog, _ ->
            val folderName = input.text.toString().trim()
            if (folderName.isNotBlank()) {
                fileSystemViewModel.createNewFolder(folderName)
            } else {
                Toast.makeText(requireContext(), getString(R.string.message_empty_name), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun showDeleteConfirmationDialog() {
        val selectedCount = fileSystemViewModel.selectedItems.value.size
        val message = if (selectedCount == 1) {
            R.string.one_deletion_message
        } else {
            R.string.multiple_deletion_message
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_pdf_description)
            .setMessage(message)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                fileSystemViewModel.deleteSelectedItems()
                dialog.dismiss()
                actionMode?.finish()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        actionMode?.finish()
        actionMode = null
        _binding = null

    }

    // --- Metodi per il FAB Popup ---
    private fun showPopupMenu() {
        val popupBinding = it.lavorodigruppo.flexipdf.databinding.CustomPopupMenuBinding.inflate(LayoutInflater.from(requireContext()))
        val popupView = popupBinding.root

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

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
        val popupWidth = popupView.measuredWidth
        val fabWidth = binding.floatingActionButton.width

        val xOffset = fabX - popupWidth + fabWidth / 3
        val yOffset = fabY - popupView.measuredHeight

        val margin = (15 * resources.displayMetrics.density).toInt()
        val finalYOffset = yOffset - margin

        popupWindow.showAtLocation(
            binding.root,
            android.view.Gravity.NO_GRAVITY,
            xOffset,
            finalYOffset
        )

        popupBinding.optionImportPdf.setOnClickListener {
            (activity as? OnPdfPickerListener)?.launchPdfPicker()
            popupWindow.dismiss()
        }

        popupBinding.optionCreateFolder.setOnClickListener {
            showNewFolderDialog() // Chiama il dialog per creare la cartella
            popupWindow.dismiss()
        }

        // Aggiungi un listener per quando il popup si chiude (es. cliccando fuori)
        popupWindow.setOnDismissListener {
            rotateFabBackward() // Riporta il FAB alla posizione originale
        }
    }

    private fun rotateFabForward() {
        ObjectAnimator.ofFloat(binding.floatingActionButton, "rotation", 0f, 90f).apply {
            duration = 500
            interpolator = android.view.animation.OvershootInterpolator()
            start()
        }
    }

    private fun rotateFabBackward() {
        ObjectAnimator.ofFloat(binding.floatingActionButton, "rotation", 90f, 0f).apply {
            duration = 500
            interpolator = android.view.animation.OvershootInterpolator()
            start()
        }
    }
}