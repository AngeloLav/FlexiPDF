/**
 * @file SharedFragment.kt
 *
 * @brief Fragment che rappresenta la schermata dei file condivisi.
 *
 * Questo Fragment è un segnaposto per la futura implementazione della funzionalità
 * di gestione dei file PDF condivisi. Attualmente, carica solo il layout di base,
 * ma include una logica mock-up per la gestione di cartelle e file PDF "cloud" locali,
 * la selezione multipla, la ricerca e le azioni contestuali.
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
import android.provider.OpenableColumns
import kotlinx.coroutines.FlowPreview

/**
 * `SharedFragment` è un Fragment che simula la gestione di file e cartelle in un ambiente "cloud" condiviso.
 * Permette di visualizzare una gerarchia di cartelle e file PDF mock-up, gestire la selezione multipla,
 * la ricerca, l'importazione di PDF e la creazione di nuove cartelle.
 * Interagisce con l'Activity ospitante tramite `OnPdfFileClickListener` per l'apertura dei PDF.
 */
class SharedFragment : Fragment() {

    /**
     * Listener per la gestione dei click sui file PDF, comunicando con l'Activity ospitante.
     */
    private var pdfFileClickListener: OnPdfFileClickListener? = null

    /**
     * Launcher per l'apertura del selettore di file di sistema, utilizzato per importare PDF.
     * Gestisce il risultato della selezione del PDF e lo aggiunge alla cartella cloud corrente (mock).
     */
    private val pdfPickerLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri>? ->
            uris?.let { selectedUris ->
                if (selectedUris.isNotEmpty()) {
                    selectedUris.forEach { uri ->
                        addPdfToCurrentCloudFolder(uri)
                    }
                    Toast.makeText(context, "${selectedUris.size} PDF importati nella cartella cloud corrente.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Nessun PDF selezionato.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(context, "Nessun PDF selezionato.", Toast.LENGTH_SHORT).show()
            }
        }

    /**
     * Chiamato quando il Fragment viene attaccato al suo contesto (Activity).
     * Verifica se il contesto implementa `OnPdfFileClickListener` e lo imposta come listener.
     * @param context Il contesto (Activity) a cui il Fragment è associato.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPdfFileClickListener) {
            pdfFileClickListener = context
        } else {
            Log.e("SharedFragment", "$context deve implementare OnPdfFileClickListener.")
        }
    }

    /**
     * L'istanza del binding View per il layout del fragment.
     * Viene utilizzata per accedere alle viste nel layout in modo sicuro.
     */
    private var _binding: FragmentSharedBinding? = null

    /**
     * Proprietà di convenienza per accedere all'istanza del binding, assicurando che non sia nullo.
     */
    private val binding get() = _binding!!

    /**
     * L'adapter per la RecyclerView che visualizza i file e le cartelle nel sistema di file mock.
     */
    private lateinit var fileSystemAdapter: FileSystemAdapter

    /**
     * `MutableStateFlow` che tiene traccia della cartella cloud attualmente visualizzata.
     * Un valore `null` indica che si è nella directory radice delle cartelle cloud.
     */
    private var _currentCloudFolder = MutableStateFlow<FolderItem?>(null)

    /**
     * `StateFlow` di sola lettura per osservare la cartella cloud corrente.
     */
    private val currentCloudFolder: StateFlow<FolderItem?> = _currentCloudFolder.asStateFlow()

    /**
     * `MutableStateFlow` che indica se la modalità di selezione multipla è attiva.
     */
    private var _isSelectionModeActive = MutableStateFlow(false)

    /**
     * `StateFlow` di sola lettura per osservare lo stato della modalità di selezione.
     */
    private val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    /**
     * `MutableStateFlow` che contiene il set di elementi attualmente selezionati in modalità selezione.
     */
    private val _selectedItems = MutableStateFlow<MutableSet<FileSystemItem>>(mutableSetOf())

    /**
     * `StateFlow` di sola lettura per osservare gli elementi selezionati.
     */
    private val selectedItems: StateFlow<Set<FileSystemItem>> = _selectedItems.asStateFlow()

    /**
     * `MutableStateFlow` che contiene la stringa di query di ricerca corrente.
     */
    private val _searchQuery = MutableStateFlow("")

    /**
     * `StateFlow` di sola lettura per osservare la query di ricerca.
     */
    private val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * L'istanza di `ActionMode` attiva quando la modalità di selezione contestuale è abilitata.
     */
    private var actionMode: ActionMode? = null

    /**
     * Callback per la `ActionMode` (barra delle azioni contestuale) che gestisce la creazione,
     * la preparazione e gli eventi di click degli elementi del menu della CAB.
     */
    private val actionModeCallback = object : ActionMode.Callback {
        /**
         * Chiamato quando la `ActionMode` viene creata. Gonfia il menu contestuale.
         * @param mode L'oggetto `ActionMode` che viene creato.
         * @param menu Il `Menu` da popolare.
         * @return `true` se la `ActionMode` deve essere visualizzata, `false` altrimenti.
         */
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater: MenuInflater = mode!!.menuInflater
            inflater.inflate(R.menu.contextual_action_bar_menu, menu)

            menu?.findItem(R.id.action_move)?.isVisible = false
            menu?.findItem(R.id.action_move_here)?.isVisible = false
            menu?.findItem(R.id.action_move_back)?.isVisible = false
            return true
        }

        /**
         * Chiamato ogni volta che il menu della `ActionMode` deve essere aggiornato.
         * Aggiorna il titolo della `ActionMode` con il numero di elementi selezionati
         * e la visibilità delle voci di menu (es. "Preferito") in base al tipo di elementi selezionati.
         * @param mode L'oggetto `ActionMode` corrente.
         * @param menu Il `Menu` da aggiornare.
         * @return `true` se il menu è stato modificato, `false` altrimenti.
         */
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val selectedCount = _selectedItems.value.size
            mode?.title = "$selectedCount" + getString(R.string.selected)

            val deleteItem = menu?.findItem(R.id.action_delete_selected)
            val favoriteItem = menu?.findItem(R.id.action_favorite_selected)

            deleteItem?.isVisible = true
            favoriteItem?.isVisible = _selectedItems.value.any { it is PdfFileItem }

            return true
        }

        /**
         * Chiamato quando un elemento del menu della `ActionMode` viene cliccato.
         * Gestisce le azioni di eliminazione e di toggling dei preferiti per gli elementi selezionati.
         * @param mode L'oggetto `ActionMode` corrente.
         * @param item L'elemento del menu che è stato cliccato.
         * @return `true` se l'evento è stato gestito, `false` altrimenti.
         */
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

        /**
         * Chiamato quando la `ActionMode` sta per essere distrutta.
         * Resetta lo stato dell'actionMode e svuota tutte le selezioni.
         * @param mode L'oggetto `ActionMode` che sta per essere distrutto.
         */
        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            clearAllSelections()
        }
    }

    /**
     * Lista mock-up delle cartelle cloud di primo livello.
     * Rappresenta le principali integrazioni cloud disponibili (es. Google Drive, Dropbox).
     */
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

    /**
     * Mappa che memorizza i contenuti delle cartelle, sia quelle predefinite che quelle create dinamicamente.
     * La chiave è l'ID della cartella genitore, il valore è una lista mutabile di `FileSystemItem`
     * (file e/o sottocartelle).
     */
    private val customCloudFoldersContent = mutableMapOf<String, MutableList<FileSystemItem>>()

    /**
     * Blocco di inizializzazione che popola i contenuti iniziali delle cartelle cloud mock-up.
     * Crea file PDF e sottocartelle di esempio all'interno delle cartelle radice predefinite
     * e delle loro sottocartelle.
     */
    init {
        val googleDriveFolder = cloudFoldersRoot.first { (it as FolderItem).displayName == "Google Drive" } as FolderItem
        val dropboxFolder = cloudFoldersRoot.first { (it as FolderItem).displayName == "Dropbox Sync" } as FolderItem
        val oneDriveFolder = cloudFoldersRoot.first { (it as FolderItem).displayName == "OneDrive Shared" } as FolderItem

        val googleDriveInitialContent = mutableListOf<FileSystemItem>(
            PdfFileItem(
                displayName = "Documento Condiviso 1.pdf",
                id = UUID.randomUUID().toString(),
                uriString = Uri.EMPTY.toString(),
                isSelected = false,
                lastModified = System.currentTimeMillis() - 86400000,
                isFavorite = false,
                parentFolderId = googleDriveFolder.id
            ),
            PdfFileItem(
                displayName = "Report Q3.pdf",
                id = UUID.randomUUID().toString(),
                uriString = Uri.EMPTY.toString(),
                isSelected = false,
                lastModified = System.currentTimeMillis() - 172800000,
                isFavorite = true,
                parentFolderId = googleDriveFolder.id
            )
        )
        val sottocartellaDriveId = UUID.randomUUID().toString()
        googleDriveInitialContent.add(
            FolderItem(
                displayName = "Sottocartella Drive",
                id = sottocartellaDriveId,
                isSelected = false,
                parentFolderId = googleDriveFolder.id
            )
        )
        customCloudFoldersContent[googleDriveFolder.id] = googleDriveInitialContent

        val dropboxInitialContent = mutableListOf<FileSystemItem>(
            PdfFileItem(
                displayName = "Presentazione Client.pdf",
                id = UUID.randomUUID().toString(),
                uriString = Uri.EMPTY.toString(),
                isSelected = false,
                lastModified = System.currentTimeMillis() - 259200000,
                isFavorite = false,
                parentFolderId = dropboxFolder.id
            ),
            PdfFileItem(
                displayName = "Contratti 2024.pdf",
                id = UUID.randomUUID().toString(),
                uriString = Uri.EMPTY.toString(),
                isSelected = false,
                lastModified = System.currentTimeMillis() - 345600000,
                isFavorite = true,
                parentFolderId = dropboxFolder.id
            )
        )
        customCloudFoldersContent[dropboxFolder.id] = dropboxInitialContent

        val oneDriveInitialContent = mutableListOf<FileSystemItem>(
            PdfFileItem(
                displayName = "Manuale Prodotto.pdf",
                id = UUID.randomUUID().toString(),
                uriString = Uri.EMPTY.toString(),
                isSelected = false,
                lastModified = System.currentTimeMillis() - 432000000,
                isFavorite = false,
                parentFolderId = oneDriveFolder.id
            )
        )
        val progettiTeamId = UUID.randomUUID().toString()
        oneDriveInitialContent.add(
            FolderItem(
                displayName = "Progetti Team",
                id = progettiTeamId,
                isSelected = false,
                parentFolderId = oneDriveFolder.id
            )
        )
        customCloudFoldersContent[oneDriveFolder.id] = oneDriveInitialContent

        customCloudFoldersContent[sottocartellaDriveId] = mutableListOf(
            PdfFileItem(
                displayName = "File annidato 1.pdf",
                id = UUID.randomUUID().toString(),
                uriString = Uri.EMPTY.toString(),
                isSelected = false,
                lastModified = System.currentTimeMillis() - 604800000,
                isFavorite = false,
                parentFolderId = sottocartellaDriveId
            )
        )
        customCloudFoldersContent[progettiTeamId] = mutableListOf(
            PdfFileItem(
                displayName = "File annidato 2.pdf",
                id = UUID.randomUUID().toString(),
                uriString = Uri.EMPTY.toString(),
                isSelected = false,
                lastModified = System.currentTimeMillis() - 691200000,
                isFavorite = false,
                parentFolderId = progettiTeamId
            )
        )
    }

    /**
     * Chiamato per creare e restituire la gerarchia di viste associata al Fragment.
     * Gonfia il layout del Fragment utilizzando il View Binding.
     * @param inflater L'oggetto `LayoutInflater` che può essere usato per gonfiare qualsiasi vista nel contesto corrente.
     * @param container Se non nullo, questo è il `ViewGroup` padre a cui la UI del Fragment dovrebbe essere allegata.
     * @param savedInstanceState Se non nullo, questo Fragment viene ricostruito da uno stato precedentemente salvato.
     * @return La vista radice (View) del Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSharedBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Chiamato subito dopo che `onCreateView` ha restituito la sua vista.
     * In questo metodo, vengono configurate la `RecyclerView`, i listener per i pulsanti
     * e vengono avviate le osservazioni degli `StateFlow` per aggiornare la UI.
     * Vengono inoltre applicati gli `WindowInsets` per adattare il layout alle barre di sistema
     * e gestire la posizione del Floating Action Button in base all'orientamento dello schermo.
     * @param view La vista radice del Fragment restituita da `onCreateView`.
     * @param savedInstanceState Se non nullo, questo Fragment viene ricostruito da uno stato precedentemente salvato.
     */
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
                binding.bannerContentLayout.paddingBottom
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

    /**
     * Estensione per convertire un valore in dp (density-independent pixels) in pixel (px).
     * @param context Il contesto utilizzato per accedere ai `DisplayMetrics`.
     * @return Il valore in pixel.
     */
    private fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    /**
     * Configura la `RecyclerView` per la visualizzazione di file e cartelle.
     * Inizializza il `FileSystemAdapter` con i listener di click, long click, toggle selezione
     * e toggle preferito, e imposta il `LayoutManager`.
     */
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

    /**
     * Togglie lo stato di "preferito" di un `PdfFileItem` specifico.
     * Trova il PDF nella lista dati corrente e ne inverte lo stato `isFavorite`.
     * Aggiorna la `RecyclerView` per riflettere la modifica.
     * @param pdfFile L'oggetto `PdfFileItem` di cui cambiare lo stato di preferito.
     */
    private fun toggleFavorite(pdfFile: PdfFileItem) {
        val currentDataSource = getCurrentDataSource()
        val originalPdf = currentDataSource?.find { it.id == pdfFile.id } as? PdfFileItem

        originalPdf?.let {
            it.isFavorite = !it.isFavorite
            updateRecyclerViewContent()
            Toast.makeText(context, "Stato preferito di '${it.displayName}' togglato.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Configura i listener per i vari elementi dell'interfaccia utente,
     * inclusi i pulsanti "Aggiungi", "Indietro" e il Floating Action Button (FAB).
     * Configura anche la `SearchView` per la funzionalità di ricerca.
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupListeners() {
        binding.addButton.setOnClickListener {
            showAddCloudFolderDialog(isCreatingRootCloudFolder = true)
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
            /**
             * Chiamato quando l'utente invia la query di ricerca (es. premendo Invio).
             * Aggiorna il `_searchQuery` e nasconde la tastiera.
             * @param query La stringa di query inviata.
             * @return `true` se la query è stata gestita, `false` altrimenti.
             */
            override fun onQueryTextSubmit(query: String?): Boolean {
                _searchQuery.value = query ?: ""
                binding.searchView.clearFocus()
                return true
            }

            /**
             * Chiamato quando il testo della query di ricerca cambia.
             * Aggiorna il `_searchQuery` con il nuovo testo.
             * @param newText Il nuovo testo della query.
             * @return `true` se il testo è stato gestito, `false` altrimenti.
             */
            override fun onQueryTextChange(newText: String?): Boolean {
                _searchQuery.value = newText ?: ""
                return true
            }
        })
    }

    /**
     * Osserva gli `StateFlow` relativi allo stato dell'interfaccia utente (`currentCloudFolder`,
     * `searchQuery`, `isSelectionModeActive`, `selectedItems`).
     * Ogni volta che questi stati cambiano, vengono attivate le funzioni per aggiornare
     * il titolo, la visibilità dei pulsanti, il contenuto della `RecyclerView` e la `ActionMode` contestuale.
     */
    @OptIn(FlowPreview::class)
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

    /**
     * Gestisce il click su un elemento della lista (`FileSystemItem`).
     * Se la modalità di selezione è attiva, l'elemento viene togglato per la selezione.
     * Altrimenti, se è una cartella, si entra al suo interno; se è un PDF, viene notificato il click.
     * @param item L'elemento (`FolderItem` o `PdfFileItem`) che è stato cliccato.
     */
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

    /**
     * Gestisce il long click su un elemento della lista.
     * Toggla la selezione dell'elemento e attiva la modalità di selezione se non già attiva.
     * @param item L'elemento (`FileSystemItem`) che è stato long cliccato.
     * @return `true` per indicare che l'evento è stato consumato.
     */
    private fun handleItemLongClick(item: FileSystemItem): Boolean {
        toggleSelection(item)
        return true
    }

    /**
     * Gestisce il doppio click su un `PdfFileItem`.
     * Notifica l'Activity ospitante per forzare l'apertura del PDF in una nuova `Activity` (viewer dedicato).
     * @param pdfFile L'oggetto `PdfFileItem` che è stato doppio cliccato.
     */
    private fun handlePdfDoubleClick(pdfFile: PdfFileItem) {
        pdfFileClickListener?.onPdfFileClickedForceActivity(pdfFile.uriString.toUri())
    }

    /**
     * Toggla lo stato di selezione di un `FileSystemItem` (FolderItem o PdfFileItem).
     * Crea una nuova istanza dell'elemento con lo stato `isSelected` modificato,
     * la sostituisce nella lista dati e aggiorna il set `_selectedItems`.
     * Attiva o disattiva la modalità di selezione (`_isSelectionModeActive`) in base al numero di elementi selezionati.
     * @param item L'elemento da togglare.
     */
    private fun toggleSelection(item: FileSystemItem) {
        val currentDataSource = getCurrentDataSource()
        val index = currentDataSource?.indexOfFirst { it.id == item.id }

        if (index != null && index != -1) {
            val originalItem = currentDataSource[index]
            val updatedItem: FileSystemItem

            updatedItem = when (originalItem) {
                is FolderItem -> originalItem.copy(isSelected = !originalItem.isSelected)
                is PdfFileItem -> originalItem.copy(isSelected = !originalItem.isSelected)
                else -> originalItem
            }

            currentDataSource[index] = updatedItem

            if (updatedItem.isSelected) {
                _selectedItems.value = (_selectedItems.value + updatedItem).toMutableSet()
            } else {
                _selectedItems.value = (_selectedItems.value - updatedItem).toMutableSet()
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

    /**
     * Deseleziona tutti gli elementi attualmente selezionati.
     * Resetta lo stato `isSelected` per tutti gli elementi nel data source corrente
     * e svuota il set `_selectedItems`. Disattiva la modalità di selezione.
     */
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

    /**
     * Esce dalla modalità di selezione multipla.
     * Chiama `clearAllSelections()` per deselezionare tutti gli elementi
     * e termina l'azione contestuale (`actionMode`).
     */
    private fun exitSelectionMode() {
        clearAllSelections()
        actionMode?.finish()
    }

    /**
     * Toggla lo stato di "preferito" per tutti i `PdfFileItem` selezionati.
     * Itera sugli elementi selezionati, crea nuove istanze dei PDF con `isFavorite` modificato e `isSelected` a `false`.
     * Dopo l'aggiornamento, deseleziona tutti gli elementi e aggiorna la `RecyclerView`.
     */
    private fun toggleFavoriteSelectedPdfs() {
        val currentDataSource = getCurrentDataSource()
        _selectedItems.value.filterIsInstance<PdfFileItem>().forEach { selectedPdf ->
            val index = currentDataSource?.indexOfFirst { it.id == selectedPdf.id }
            if (index != null && index != -1) {
                val originalPdf = currentDataSource[index] as? PdfFileItem
                originalPdf?.let {
                    val updatedPdf = it.copy(isFavorite = !it.isFavorite, isSelected = false)
                    currentDataSource[index] = updatedPdf // Sostituisce il vecchio item con il nuovo
                }
            }
        }
        _selectedItems.value = mutableSetOf()
        _isSelectionModeActive.value = false
        updateRecyclerViewContent()
    }

    /**
     * Restituisce la lista mutabile di `FileSystemItem` che rappresenta il contenuto
     * della cartella cloud attualmente visualizzata.
     * Se `_currentCloudFolder` è nullo, restituisce le cartelle radice.
     * Altrimenti, restituisce il contenuto della cartella specifica dalla mappa `customCloudFoldersContent`.
     * @return Una `MutableList<FileSystemItem>?` contenente gli elementi della cartella corrente, o null se non trovata.
     */
    private fun getCurrentDataSource(): MutableList<FileSystemItem>? {
        val currentFolder = _currentCloudFolder.value
        return if (currentFolder == null) {
            cloudFoldersRoot
        } else {
            customCloudFoldersContent[currentFolder.id]
        }
    }

    /**
     * Estrae il nome del file da un dato `Uri`.
     * Tenta di ottenere il nome visualizzato tramite `ContentResolver` per URI di tipo "content",
     * altrimenti estrae il nome dal percorso dell'URI.
     * @param uri L'URI del file.
     * @return Il nome del file come stringa, o "Documento senza nome.pdf" se non può essere determinato.
     */
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

    /**
     * Aggiunge un nuovo `PdfFileItem` alla cartella cloud attualmente visualizzata.
     * Crea un nuovo `PdfFileItem` con i dettagli forniti e lo aggiunge alla lista dati della cartella corrente.
     * Aggiorna la `RecyclerView`.
     * @param uri L'URI del PDF da aggiungere.
     */
    private fun addPdfToCurrentCloudFolder(uri: Uri) {
        val newPdf = PdfFileItem(
            displayName = getFileNameFromUri(uri),
            id = UUID.randomUUID().toString(),
            uriString = uri.toString(),
            isSelected = false,
            lastModified = System.currentTimeMillis(),
            isFavorite = false,
            parentFolderId = _currentCloudFolder.value?.id
        )

        val targetList = getCurrentDataSource()
        targetList?.add(newPdf)
        updateRecyclerViewContent()
    }

    /**
     * Aggiorna il testo del titolo nell'interfaccia utente e la visibilità dei pulsanti
     * "Indietro" e "Aggiungi" in base alla cartella cloud attualmente visualizzata.
     * Se si è nella radice, mostra il titolo generico e il pulsante "Aggiungi cartella cloud".
     * Se si è in una sottocartella, mostra il nome della cartella e il pulsante "Indietro".
     * La `SearchView` è sempre visibile.
     */
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

    /**
     * Aggiorna il contenuto della `RecyclerView` applicando il filtro di ricerca corrente.
     * Recupera i dati dalla sorgente corrente, li filtra in base alla `_searchQuery`
     * e li invia all'`adapter` per la visualizzazione.
     */
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

    /**
     * Entra in una cartella specificata, aggiornando lo stato della cartella corrente.
     * Svuota tutte le selezioni e resetta la query di ricerca.
     * @param folder L'oggetto `FolderItem` della cartella in cui entrare.
     */
    private fun enterFolder(folder: FolderItem) {
        _currentCloudFolder.value = folder
        clearAllSelections()
        _searchQuery.value = ""
        binding.searchView.setQuery("", false)
    }

    /**
     * Naviga indietro di una cartella nella gerarchia.
     * Se la cartella corrente non è la radice, imposta la cartella corrente a `null` (radice).
     * Svuota tutte le selezioni e resetta la query di ricerca.
     */
    private fun navigateBack() {
        if (_currentCloudFolder.value != null) {
            _currentCloudFolder.value = null
            clearAllSelections()
            _searchQuery.value = ""
            binding.searchView.setQuery("", false)
        }
    }

    /**
     * Mostra un dialog per aggiungere una nuova cartella cloud (radice) o una sottocartella.
     * Il dialog permette all'utente di inserire il nome della cartella e, opzionalmente, un parametro cloud
     * se si sta creando una cartella cloud di primo livello.
     * @param isCreatingRootCloudFolder Indica se la cartella da creare è una cartella cloud radice (true) o una sottocartella (false).
     */
    @SuppressLint("MissingInflatedId")
    private fun showAddCloudFolderDialog(isCreatingRootCloudFolder: Boolean = false) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (isCreatingRootCloudFolder) getString(R.string.add_cloud) else "Crea Nuova Cartella")

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_cloud_folder, null)
        val folderNameEditText = view.findViewById<EditText>(R.id.folderNameEditText)
        val cloudParamEditText = view.findViewById<EditText>(R.id.cloudParamEditText)
        builder.setView(view)

        if (isCreatingRootCloudFolder) {
            cloudParamEditText.visibility = View.VISIBLE
        } else {
            cloudParamEditText.visibility = View.GONE
        }

        builder.setPositiveButton(getString(R.string.create)) { dialog, _ ->
            val folderName = folderNameEditText.text.toString().trim()
            val cloudParam = if (isCreatingRootCloudFolder) cloudParamEditText.text.toString().trim() else null

            if (folderName.isNotEmpty()) {
                val newFolderId = UUID.randomUUID().toString()
                val newCloudFolder = FolderItem(
                    displayName = folderName,
                    isCloudFolder = isCreatingRootCloudFolder,
                    cloudLinkParam = cloudParam,
                    parentFolderId = if (isCreatingRootCloudFolder) null else _currentCloudFolder.value?.id,
                    id = newFolderId,
                    isSelected = false
                )

                if (isCreatingRootCloudFolder) {
                    cloudFoldersRoot.add(newCloudFolder)
                    customCloudFoldersContent[newFolderId] = mutableListOf()
                } else {
                    val parentFolderContentList = getCurrentDataSource()
                    parentFolderContentList?.add(newCloudFolder)
                    customCloudFoldersContent[newFolderId] = mutableListOf()
                }

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

    /**
     * Mostra un dialog di conferma prima di eliminare gli elementi selezionati.
     * Il messaggio del dialog varia in base al numero di elementi selezionati.
     * Se confermato, gli elementi vengono rimossi dalla lista dati mock-up.
     */
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

                itemsToDelete.forEach { item ->
                    currentDataSource?.removeIf { it.id == item.id }
                    if (item is FolderItem) {
                        customCloudFoldersContent.remove(item.id)
                    }
                }

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

    /**
     * Chiamato quando la vista del Fragment sta per essere distrutta.
     * Esegue la pulizia delle risorse: termina l'`actionMode` se attiva,
     * rilascia il binding e il listener dell'Activity.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        actionMode?.finish()
        actionMode = null
        _binding = null
        pdfFileClickListener = null
    }

    /**
     * Anima il Floating Action Button (FAB) ruotandolo in avanti (da 0 a 90 gradi).
     * Utilizza un `OvershootInterpolator` per un effetto visivo più dinamico.
     */
    private fun rotateFabForward() {
        ObjectAnimator.ofFloat(binding.floatingActionButton, "rotation", 0f, 90f).apply {
            duration = 500
            interpolator = OvershootInterpolator()
            start()
        }
    }

    /**
     * Anima il Floating Action Button (FAB) ruotandolo indietro (da 90 a 0 gradi).
     * Utilizza un `OvershootInterpolator` per un effetto visivo più dinamico.
     */
    private fun rotateFabBackward() {
        ObjectAnimator.ofFloat(binding.floatingActionButton, "rotation", 90f, 0f).apply {
            duration = 500
            interpolator = OvershootInterpolator()
            start()
        }
    }

    /**
     * Mostra un menu popup personalizzato sopra il Floating Action Button.
     * Il menu contiene opzioni per importare un PDF o creare una nuova cartella.
     * Gestisce la posizione del popup rispetto al FAB e le azioni di click sugli elementi del popup.
     */
    @SuppressLint("InflateParams")
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
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
            popupWindow.dismiss()
        }

        optionCreateFolder.setOnClickListener {
            showAddCloudFolderDialog(isCreatingRootCloudFolder = false)
            popupWindow.dismiss()
        }

        popupWindow.setOnDismissListener {
            rotateFabBackward()
        }
    }
}