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

/**
 * `FoldersFragment` è il frammento principale che visualizza la lista di file PDF e cartelle
 * all'interno dell'applicazione. Gestisce l'interazione dell'utente con gli elementi del file system,
 * la modalità di selezione contestuale, la navigazione tra cartelle, la ricerca,
 * e l'integrazione con il ViewModel per la gestione dei dati.
 */
class FoldersFragment(
) : Fragment() {

    /**
     * Listener per la gestione dei click sui file PDF, comunicando con l'Activity ospitante.
     */
    private var pdfFileClickListener: OnPdfFileClickListener? = null

    /**
     * Override del metodo `onAttach` del ciclo di vita del Fragment.
     * Questo metodo viene chiamato quando il Fragment viene associato al suo contesto (Activity).
     * Qui si verifica se il contesto implementa l'interfaccia `OnPdfFileClickListener`
     * e, in caso affermativo, si assegna il riferimento al listener per consentire la comunicazione.
     *
     * @param context Il contesto (Activity) a cui il Fragment è associato.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPdfFileClickListener) {
            pdfFileClickListener = context
        } else {
            Log.e("FoldersFragment", "$context must implement OnPdfFileClickListener")
        }
    }

    /**
     * L'istanza del binding View per il layout del fragment.
     * Utilizzata per accedere alle viste nel layout in modo sicuro e senza boilerplate.
     */
    private var _binding: FragmentFoldersBinding? = null

    /**
     * Proprietà di convenienza per accedere all'istanza del binding.
     * Assicura che il binding non sia nullo quando viene utilizzato.
     */
    private val binding get() = _binding!!

    /**
     * L'istanza del ViewModel associato a questo Fragment, utilizzata per gestire la logica dei dati
     * e interagire con il modello del file system.
     */
    private lateinit var fileSystemViewModel: FileSystemViewModel

    /**
     * L'istanza dell'adapter per la `RecyclerView` che visualizza gli elementi del file system.
     */
    private lateinit var fileSystemAdapter: FileSystemAdapter

    /**
     * L'istanza di `ActionMode` utilizzata per la barra delle azioni contestuale (Contextual Action Bar - CAB).
     * Questa barra appare quando gli elementi vengono selezionati per azioni multiple (es. elimina, sposta).
     */
    private var actionMode: ActionMode? = null

    /**
     * Implementazione dell'interfaccia `ActionMode.Callback` per gestire il ciclo di vita e
     * le interazioni con la Contextual Action Bar (CAB). Definisce come viene creata la CAB,
     * come vengono preparate le sue voci di menu in base allo stato, e come vengono gestiti i click sulle voci.
     */
    private val actionModeCallback = object : ActionMode.Callback {
        /**
         * Chiamato quando la Contextual Action Bar (CAB) viene creata.
         * Gonfia il menu XML (`contextual_action_bar_menu.xml`) per la CAB.
         * @param mode L'istanza di `ActionMode` per questa CAB.
         * @param menu Il `Menu` della CAB in cui gonfiare le voci.
         * @return `true` se la CAB deve essere mostrata, `false` altrimenti.
         */
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater: MenuInflater = mode!!.menuInflater
            inflater.inflate(R.menu.contextual_action_bar_menu, menu)
            return true
        }

        /**
         * Chiamato ogni volta che la Contextual Action Bar (CAB) deve essere aggiornata.
         * Questo include la prima volta che viene mostrata e ogni volta che `invalidate()` viene chiamato.
         * Questo metodo viene utilizzato per mostrare/nascondere le voci di menu e aggiornare il titolo
         * in base allo stato corrente (modalità di selezione standard o modalità di spostamento).
         * @param mode L'istanza di `ActionMode` per questa CAB.
         * @param menu Il `Menu` della CAB da preparare.
         * @return `true` se il menu è stato modificato e deve essere ridisegnato, `false` altrimenti.
         */
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val isMoving = fileSystemViewModel.isMovingItems.value
            val selectedCount = fileSystemViewModel.selectedItems.value.size
            val itemsToMoveCount = fileSystemViewModel.itemsToMove.value.size

            val deleteItem = menu?.findItem(R.id.action_delete_selected)
            val favoriteItem = menu?.findItem(R.id.action_favorite_selected)
            val moveItem = menu?.findItem(R.id.action_move)
            val moveHereItem = menu?.findItem(R.id.action_move_here)
            val moveBackItem = menu?.findItem(R.id.action_move_back)

            if (isMoving) {
                deleteItem?.isVisible = false
                favoriteItem?.isVisible = false
                moveItem?.isVisible = false

                moveHereItem?.isVisible = true
                moveBackItem?.isVisible = fileSystemViewModel.currentFolder.value != null

                mode?.title = resources.getQuantityString(R.plurals.move_items_message, itemsToMoveCount, itemsToMoveCount)
            } else {
                deleteItem?.isVisible = true
                favoriteItem?.isVisible = fileSystemViewModel.selectedItems.value.any { it is PdfFileItem }
                moveItem?.isVisible = true

                moveHereItem?.isVisible = false
                moveBackItem?.isVisible = false

                mode?.title = "$selectedCount" + getString(R.string.selected)
            }
            return true
        }

        /**
         * Chiamato quando l'utente clicca su una voce di menu nella Contextual Action Bar (CAB).
         * Gestisce le azioni corrispondenti all'ID della voce cliccata (es. eliminazione, aggiunta ai preferiti, spostamento).
         * @param mode L'istanza di `ActionMode` per questa CAB.
         * @param item La `MenuItem` che è stata cliccata.
         * @return `true` se l'evento è stato gestito, `false` altrimenti.
         */
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
                    fileSystemViewModel.initiateMove()
                    mode?.invalidate()
                    Snackbar.make(binding.root, getString(R.string.moving_items), Snackbar.LENGTH_LONG).show()
                    true
                }
                R.id.action_move_here -> {
                    fileSystemViewModel.moveItemsToCurrentFolder()
                    Snackbar.make(binding.root, getString(R.string.successful_moving_operation), Snackbar.LENGTH_SHORT).show()
                    mode?.finish()
                    true
                }

                R.id.action_move_back -> {
                    fileSystemViewModel.goBack()
                    mode?.invalidate()
                    true
                }
                else -> false
            }
        }

        /**
         * Chiamato quando la Contextual Action Bar (CAB) viene chiusa o distrutta.
         * Questo accade quando l'utente esce dalla modalità contestuale.
         * Esegue la pulizia necessaria, come reimpostare lo stato di `actionMode` a `null`,
         * cancellare tutte le selezioni e annullare qualsiasi operazione di spostamento in corso.
         * @param mode L'istanza di `ActionMode` che sta per essere distrutta.
         */
        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            fileSystemViewModel.clearAllSelections()
            fileSystemViewModel.cancelMoveOperation()
        }
    }

    /**
     * Override del metodo `onCreateView` del ciclo di vita del Fragment.
     * Questo metodo è responsabile di gonfiare il layout del Fragment e restituire la sua vista radice.
     * Utilizza View Binding per creare un'istanza del binding per il layout `fragment_folders.xml`.
     * @param inflater L'oggetto `LayoutInflater` che può essere usato per gonfiare qualsiasi vista nel contesto corrente.
     * @param container Se non nullo, questo è il ViewGroup padre a cui la UI del Fragment dovrebbe essere allegata.
     * @param savedInstanceState Se non nullo, questo Fragment viene ricostruito da uno stato precedentemente salvato.
     * @return La vista radice (View) del Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Override del metodo `onViewCreated` del ciclo di vita del Fragment.
     * Questo metodo viene chiamato subito dopo che `onCreateView` ha restituito la sua vista.
     * Qui vengono inizializzati il `FileSystemViewModel`, configurata la `RecyclerView`,
     * impostati i listener per gli elementi UI e avviata l'osservazione dei dati dal ViewModel.
     * Include anche la logica per la gestione degli `WindowInsets` per adattare il layout alle barre di sistema.
     * @param view La vista radice del Fragment restituita da `onCreateView`.
     * @param savedInstanceState Se non nullo, questo Fragment viene ricostruito da uno stato precedentemente salvato.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileSystemViewModel = ViewModelProvider(requireActivity(),
            FileSystemViewModel.FileSystemViewModelFactory(requireActivity().application)
        )[FileSystemViewModel::class.java]

        fileSystemViewModel.goToRoot()

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBarsInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.bannerContentLayout.setPadding(
                binding.bannerContentLayout.paddingLeft,
                systemBarsInsets.top,
                binding.bannerContentLayout.paddingRight,
                binding.bannerContentLayout.paddingBottom
            )

            val orientation = resources.configuration.orientation
            val bottomPaddingForRecyclerView = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                0
            } else {
                systemBarsInsets.bottom
            }

            binding.pdfRecyclerView.setPadding(
                systemBarsInsets.left,
                binding.pdfRecyclerView.paddingTop,
                systemBarsInsets.right,
                bottomPaddingForRecyclerView
            )

            val fabLayoutParams = binding.floatingActionButton.layoutParams as? ConstraintLayout.LayoutParams
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

    /**
     * Funzione di estensione per `Int` che converte un valore da DP (Density-independent Pixels) a PX (Pixels).
     * Utile per impostare dimensioni in pixel in modo programmatico basandosi su valori DP.
     * @receiver Il valore in DP da convertire.
     * @param context Il contesto utilizzato per accedere ai `displayMetrics` del dispositivo.
     * @return Il valore convertito in pixel.
     */
    private fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    /**
     * Configura la `RecyclerView` per visualizzare gli elementi del file system.
     * Inizializza il `FileSystemAdapter` con tutti i listener di interazione necessari (click, long click,
     * doppio click, toggle di selezione, toggle preferiti) e lo associa alla `RecyclerView`.
     * Include una logica specifica per gestire i click sugli elementi in base alla modalità di spostamento attiva.
     */
    private fun setupRecyclerView() {
        fileSystemAdapter = FileSystemAdapter(
            onItemClick = onItemClick@{ item ->
                if (fileSystemViewModel.isMovingItems.value) {
                    if (item is PdfFileItem) {
                        fileSystemViewModel.showUserMessage("Click on a folder to navigate or move here to confirm")
                        Log.d("FoldersFragment", "Tentativo di aprire PDF in modalità spostamento, impedito: ${item.displayName}")
                        return@onItemClick
                    } else if (item is FolderItem) {
                        fileSystemViewModel.enterFolder(item)
                        return@onItemClick
                    }
                }

                when (item) {
                    is PdfFileItem -> {
                        val pdfUri = item.uriString.toUri()
                        Log.d("FoldersFragment", "PDF cliccato: ${item.displayName}. Notifico l'Activity tramite callback.")
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

    /**
     * Configura i listener per gli elementi dell'interfaccia utente nel Fragment.
     * Include il listener per il `FloatingActionButton` (FAB) per mostrare un popup,
     * i listener per la `SearchView` per la gestione della ricerca del testo,
     * e il listener per il pulsante "indietro" personalizzato.
     */
    private fun setupListeners() {

        binding.floatingActionButton.setOnClickListener {
            rotateFabForward()
            showPopupMenu()
        }

        val searchEditText = binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.let {
            it.background = null
            it.setTextColor(requireContext().getColor(android.R.color.white))
            it.setHintTextColor(requireContext().getColor(android.R.color.darker_gray))
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

    /**
     * Osserva i `Flow` esposti dal `FileSystemViewModel` per aggiornare l'interfaccia utente
     * in risposta a cambiamenti nei dati.
     * - `filteredAndDisplayedItems`: aggiorna la lista mostrata nella `RecyclerView`.
     * - `currentFolder`: aggiorna il titolo della barra superiore e la visibilità del pulsante "indietro".
     * - `isSelectionModeActive`: gestisce l'attivazione/disattivazione della `Contextual Action Bar` (CAB)
     * e la modalità di selezione dell'adapter.
     * - `selectedItems`: aggiorna il titolo della CAB con il conteggio degli elementi selezionati.
     */
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
                    actionMode?.invalidate()
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
                    actionMode?.invalidate()
                } else {
                    if (!fileSystemViewModel.isMovingItems.value) {
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

    /**
     * Mostra un `AlertDialog` che permette all'utente di inserire un nome per una nuova cartella.
     * Se il nome inserito non è vuoto, viene richiesta al `fileSystemViewModel` la creazione della nuova cartella.
     * Mostra un Toast se il nome è vuoto.
     */
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

    /**
     * Mostra un `AlertDialog` di conferma prima di procedere con l'eliminazione degli elementi selezionati.
     * Il messaggio del dialog varia a seconda che sia stato selezionato un solo elemento o più elementi.
     * Se l'utente conferma, chiama il `fileSystemViewModel` per eliminare gli elementi e chiude la CAB.
     */
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

    /**
     * Override del metodo `onDestroyView` del ciclo di vita del Fragment.
     * Chiamato quando la vista del Fragment sta per essere distrutta.
     * Esegue la pulizia delle risorse: chiude la `Contextual Action Bar` (se attiva),
     * resetta l'istanza del binding a `null` per prevenire memory leak,
     * e resetta il riferimento al `pdfFileClickListener`.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        actionMode?.finish()
        actionMode = null
        _binding = null
        pdfFileClickListener = null
    }

    /**
     * Mostra un `PopupWindow` personalizzato che funge da menu per il `FloatingActionButton`.
     * Questo popup contiene opzioni come "Importa PDF" e "Crea Cartella".
     * Il posizionamento del popup è calcolato per apparire sopra il FAB.
     * Definisce i listener per le opzioni del popup e per la sua chiusura, ripristinando l'animazione del FAB.
     */
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
            showNewFolderDialog()
            popupWindow.dismiss()
        }

        popupWindow.setOnDismissListener {
            rotateFabBackward()
        }
    }

    /**
     * Avvia un'animazione di rotazione per il `FloatingActionButton` (FAB) in avanti (da 0 a 90 gradi).
     * Utilizzato quando il FAB viene cliccato per aprire il menu popup.
     */
    private fun rotateFabForward() {
        ObjectAnimator.ofFloat(binding.floatingActionButton, "rotation", 0f, 90f).apply {
            duration = 500
            interpolator = android.view.animation.OvershootInterpolator()
            start()
        }
    }

    /**
     * Avvia un'animazione di rotazione per il `FloatingActionButton` (FAB) all'indietro (da 90 a 0 gradi).
     * Utilizzato quando il menu popup del FAB viene chiuso.
     */
    private fun rotateFabBackward() {
        ObjectAnimator.ofFloat(binding.floatingActionButton, "rotation", 90f, 0f).apply {
            duration = 500
            interpolator = android.view.animation.OvershootInterpolator()
            start()
        }
    }
}