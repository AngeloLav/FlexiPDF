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
import android.content.Context
import android.content.res.Configuration
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.adapters.FileSystemAdapter
import it.lavorodigruppo.flexipdf.databinding.FragmentFoldersBinding
import it.lavorodigruppo.flexipdf.items.FolderItem
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import it.lavorodigruppo.flexipdf.viewmodels.FileSystemViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.Snackbar
import it.lavorodigruppo.flexipdf.data.FileSystemDatasource

class FoldersFragment(
) : Fragment() {

    private var pdfFileClickListener: OnPdfFileClickListener? = null

    // Override del metodo onAttach per ottenere il riferimento all'Activity (che implementa il listener)
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPdfFileClickListener) {
            pdfFileClickListener = context
        } else {
            // È buona pratica lanciare un'eccezione se l'Activity non implementa il listener richiesto.
            // Puoi anche solo loggare un avviso se preferisci.
            Log.e("FoldersFragment", "$context must implement OnPdfFileClickListener")
        }
    }

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
            val moveBackItem = menu?.findItem(R.id.action_move_back)

            if (isMoving) {
                // Modalità di spostamento: mostra "Sposta qui" e "Annulla"
                deleteItem?.isVisible = false
                favoriteItem?.isVisible = false
                moveItem?.isVisible = false

                moveHereItem?.isVisible = true

                moveBackItem?.isVisible = fileSystemViewModel.currentFolder.value != null


                mode?.title = resources.getQuantityString(R.plurals.move_items_message, itemsToMoveCount, itemsToMoveCount)
            } else {
                // Modalità di selezione standard: mostra "Elimina", "Preferito", "Sposta"
                deleteItem?.isVisible = true
                favoriteItem?.isVisible = fileSystemViewModel.selectedItems.value.any { it is PdfFileItem }
                moveItem?.isVisible = true

                moveHereItem?.isVisible = false
                moveBackItem?.isVisible = false

                mode?.title = "$selectedCount" + getString(R.string.selected)


                fileSystemViewModel.currentFolder.value?.id ?: FileSystemDatasource.ROOT_FOLDER_ID

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

        // --- INIZIO: GESTIONE WINDOW INSETS per il banner superiore, RecyclerView e FAB ---
        // Questo listener è fondamentale per adattare il layout alle barre di sistema (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            // Ottieni gli insets per le barre di sistema (status bar, navigation bar)
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Ottieni gli insets specifici per la barra di navigazione
            val navigationBarsInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // 1. Applica l'inset superiore al padding del topBannerCardView (per la status bar)
            binding.bannerContentLayout.setPadding(
                binding.bannerContentLayout.paddingLeft,
                systemBarsInsets.top,
                binding.bannerContentLayout.paddingRight,
                binding.bannerContentLayout.paddingBottom
            )

            // 2. Determina il padding inferiore per la pdfRecyclerView in base all'orientamento
            val orientation = resources.configuration.orientation
            val bottomPaddingForRecyclerView = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                0 // Nessun padding inferiore in modalità orizzontale, la lista può andare sotto la barra
            } else {
                systemBarsInsets.bottom // Applica il padding inferiore in modalità verticale (per non coprire il contenuto)
            }

            // Applica il padding inferiore alla pdfRecyclerView
            // (gli insets left/right di systemBarsInsets tengono conto di notch, display cutouts ecc.)
            binding.pdfRecyclerView.setPadding(
                systemBarsInsets.left, // Padding sinistro per eventuali insets del sistema (es. notch/gesture bar)
                binding.pdfRecyclerView.paddingTop,
                systemBarsInsets.right, // Padding destro per eventuali insets del sistema
                bottomPaddingForRecyclerView // Padding inferiore condizionale
            )

            // 3. Gestione del Floating Action Button (FAB) in base agli insets della barra di navigazione
            val fabLayoutParams = binding.floatingActionButton.layoutParams as? ConstraintLayout.LayoutParams
            if (fabLayoutParams != null) {
                // Margini di default per il FAB, se non modificati in XML
                val defaultMarginEnd = 16.dpToPx(requireContext())
                val defaultMarginBottom = 16.dpToPx(requireContext())

                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // In landscape, la barra di navigazione è solitamente a destra o sinistra.
                    // Aggiungiamo l'inset destro (se la barra è a destra) al marginEnd del FAB.
                    fabLayoutParams.marginEnd = defaultMarginEnd + navigationBarsInsets.right
                    // Il bottom margin può rimanere quello di default o essere adattato se la barra è anche in basso.
                    fabLayoutParams.bottomMargin = defaultMarginBottom
                } else { // Portrait
                    // In portrait, la barra di navigazione è in basso.
                    // Aggiungiamo l'inset inferiore al marginBottom del FAB.
                    fabLayoutParams.marginEnd = defaultMarginEnd
                    fabLayoutParams.bottomMargin = defaultMarginBottom + navigationBarsInsets.bottom
                }
                binding.floatingActionButton.layoutParams = fabLayoutParams // Applica i LayoutParams modificati
            }

            insets // Restituisci gli insets per permettere ad altre viste di gestirli
        }
    }

    private fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

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
                        Log.d("FoldersFragment", "PDF cliccato: ${item.displayName}. Notifico l'Activity tramite callback.")
                        // Utilizza il callback per notificare l'Activity
                        pdfFileClickListener?.onPdfFileClicked(pdfUri)

                    }
                    is FolderItem -> {
                        fileSystemViewModel.enterFolder(item)
                    }
                }
            },
            onItemLongClick = { item ->
                fileSystemViewModel.toggleSelection(item)
                true
            },
            onItemDoubleClick = { pdfFile ->
                val pdfUri = pdfFile.uriString.toUri()
                pdfFileClickListener?.onPdfFileClickedForceActivity(pdfUri)
            },
            onSelectionToggle = { item ->
                fileSystemViewModel.toggleSelection(item)
            },
            onFavoriteToggle = { pdfFile ->
                fileSystemViewModel.toggleFavorite(pdfFile)
            }
        )

        binding.pdfRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = fileSystemAdapter
        }
    }

    private fun setupListeners() {

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
        // IMPORTANTE: Resetta il listener per evitare memory leaks quando il fragment è scollegato
        pdfFileClickListener = null
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