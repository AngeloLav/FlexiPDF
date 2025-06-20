/**
 * @file MainActivity.kt
 *
 * @overview
 * Questa activity è il punto di ingresso principale dell'applicazione FlexiPDF.
 * È responsabile di:
 * - Gestire l'inizializzazione del layout tramite ViewBinding per un accesso efficiente ai componenti UI.
 * - Coordinare la navigazione tra i diversi fragment (Home, Folders, Shared, Settings) in base al componente di navigazione attivo
 * (BottomNavigationView, NavigationRailView, o un Custom Side Menu).
 * - Gestire l'importazione di file PDF tramite un ActivityResultLauncher.
 * - Coordinare l'apertura dei file PDF, differenziando tra la visualizzazione in un pannello secondario (per layout tablet in orizzontale)
 * o l'apertura in una nuova Activity dedicata (per smartphone o tablet in verticale).
 * - Integrare i ViewModel (`FileSystemViewModel`) per una corretta gestione dei dati UI e `PdfManager` per le operazioni sui PDF.
 * - Implementare una logica di debounce per prevenire click rapidi e multipli sui componenti di navigazione.
 */

package it.lavorodigruppo.flexipdf.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.ContextCompat

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView

import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.databinding.ActivityMainBinding
import it.lavorodigruppo.flexipdf.databinding.LayoutCustomSideMenuBinding

import it.lavorodigruppo.flexipdf.fragments.FoldersFragment
import it.lavorodigruppo.flexipdf.fragments.HomeFragment
import it.lavorodigruppo.flexipdf.fragments.OnPdfFileClickListener
import it.lavorodigruppo.flexipdf.fragments.SettingsFragment
import it.lavorodigruppo.flexipdf.fragments.SharedFragment

import it.lavorodigruppo.flexipdf.fragments.OnPdfPickerListener
import it.lavorodigruppo.flexipdf.fragments.PdfViewerFragment
import it.lavorodigruppo.flexipdf.utils.PdfManager
import it.lavorodigruppo.flexipdf.viewmodels.FileSystemViewModel
import it.lavorodigruppo.flexipdf.viewmodels.FileSystemViewModel.FileSystemViewModelFactory

class MainActivity : AppCompatActivity(), OnPdfPickerListener, OnPdfFileClickListener {

    /**
     * L'oggetto ViewBinding per l'Activity principale. Permette un accesso sicuro e diretto a tutte le viste definite in `activity_main.xml`.
     */
    private lateinit var binding: ActivityMainBinding

    /**
     * Un'istanza di `PdfManager`, una classe helper responsabile della gestione delle operazioni legate ai file PDF,
     * come l'apertura del selettore di file.
     */
    private lateinit var pdfManager: PdfManager

    /**
     * Un'istanza di `FileSystemViewModel`, un ViewModel che gestisce la logica e i dati relativi ai file e alle cartelle,
     * inclusa l'importazione di PDF, persistendo attraverso i cambiamenti di configurazione dell'Activity.
     */
    private lateinit var fileSystemViewModel: FileSystemViewModel

    /**
     * Timestamp dell'ultimo click valido su un elemento di navigazione, utilizzato per implementare la logica di debounce.
     */
    private var lastNavigationClickTime: Long = 0

    /**
     * Il tempo minimo (in millisecondi) che deve trascorrere tra due click consecutivi sugli elementi di navigazione
     * per evitare doppi click o click accidentali troppo rapidi.
     */
    private val NAVIGATION_DEBOUNCE_TIME = 100L

    /**
     * Launcher per l'apertura del selettore di file di sistema, configurato per permettere la selezione di più documenti PDF.
     * Il risultato (lista di URI) viene passato al `fileSystemViewModel` per l'importazione.
     */
    private val pickPdfLauncher = registerForActivityResult(
        object : androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments() {
            override fun createIntent(context: Context, input: Array<String>): Intent {
                return super.createIntent(context, input).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/pdf"
                }
            }
        }
    ) { uris: List<Uri>? ->
        uris?.let {
            fileSystemViewModel.importPdfs(it)
        }
    }

    /**
     * Metodo di callback chiamato per avviare il selettore di file PDF.
     * Implementa l'interfaccia `OnPdfPickerListener`.
     */
    override fun launchPdfPicker() {
        pickPdfLauncher.launch(arrayOf("application/pdf"))
    }

    /**
     * Metodo chiamato alla creazione dell'Activity.
     * Inizializza il ViewBinding, imposta il layout, inizializza il `FileSystemViewModel` e il `PdfManager`.
     * Configura dinamicamente i listener per i componenti di navigazione (Custom Side Menu, NavigationRailView, BottomNavigationView)
     * in base a quale di essi è presente nel layout corrente (gestendo layout qualificati per diverse configurazioni).
     * @param savedInstanceState L'oggetto Bundle contenente lo stato precedentemente salvato dell'Activity, se presente.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            Log.e("MainActivity", "Errore critico durante l'inflazione del layout: ${e.message}", e)
            return
        }

        fileSystemViewModel = ViewModelProvider(this, FileSystemViewModel.FileSystemViewModelFactory(application))[FileSystemViewModel::class.java]
        pdfManager = PdfManager(this) { uris: List<Uri> ->
            fileSystemViewModel.importPdfs(uris)
        }

        when {
            binding.customSideMenu != null -> {
                Log.d("MainActivity", "Custom Side Menu rilevato in onCreate. Configuro listener.")
                try {
                    val customSideMenuBinding = binding.customSideMenu!!
                    customSideMenuBinding.customMenuItemHome.root.setOnClickListener { handleNavigationItemSelected(R.id.home) }
                    customSideMenuBinding.customMenuItemFolders.root.setOnClickListener { handleNavigationItemSelected(R.id.folders) }
                    customSideMenuBinding.customMenuItemShared.root.setOnClickListener { handleNavigationItemSelected(R.id.shared) }
                    customSideMenuBinding.customMenuItemSettings.root.setOnClickListener { handleNavigationItemSelected(R.id.settings) }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Errore configurando listener custom side menu in onCreate: ${e.message}", e)
                }
            }
            binding.navigationRail != null -> {
                Log.d("MainActivity", "NavigationRailView rilevata in onCreate. Configuro listener.")
                setupNavigationRail(binding.navigationRail!!)
            }
            binding.bottomNavigationView != null -> {
                Log.d("MainActivity", "BottomNavigationView rilevata in onCreate. Configuro listener.")
                setupBottomNavigationView(binding.bottomNavigationView!!)
            }
            else -> {
                Log.w("MainActivity", "Nessun componente di navigazione principale trovato nel layout in onCreate. Controlla i tuoi XML qualificati.")
            }
        }
    }

    /**
     * Metodo chiamato dopo che `onCreate` è terminato e tutte le viste sono state ripristinate.
     * Utilizzato per eseguire la sostituzione iniziale del fragment (se l'Activity viene creata per la prima volta)
     * o per ripristinare e aggiornare la selezione della UI di navigazione in caso di ricreazione dell'Activity (es. rotazione).
     * Gestisce anche la visibilità del contenitore del viewer PDF nei layout tablet landscape.
     * @param savedInstanceState L'oggetto Bundle contenente lo stato precedentemente salvato dell'Activity, se presente.
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Log.d("MainActivity", "onPostCreate chiamato.")

        if (savedInstanceState == null) {
            handleNavigationItemSelected(R.id.home)
            Log.d("MainActivity", "onPostCreate: savedInstanceState è null. Caricato HomeFragment iniziale.")
        } else {
            val currentFragmentInFrameLayout = supportFragmentManager.findFragmentById(R.id.frame_layout)
            currentFragmentInFrameLayout?.let {
                updateNavigationSelectionForFragment(it)
                Log.d("MainActivity", "onPostCreate: savedInstanceState NON è null. Aggiornata UI di navigazione per ${it.javaClass.simpleName}.")

                val pdfViewerContainer = findViewById<View>(R.id.fragment_pdf_viewer_container)

                val isFoldersFragmentActive = it is FoldersFragment
                val pdfViewerFragmentRestored = supportFragmentManager.findFragmentById(R.id.fragment_pdf_viewer_container) != null

                if (pdfViewerContainer != null) {
                    if (isFoldersFragmentActive && pdfViewerFragmentRestored) {
                        pdfViewerContainer.visibility = View.VISIBLE
                        Log.d("MainActivity", "onPostCreate: FoldersFragment attivo e PdfViewerFragment ripristinato. Reso visibile contenitore PDF.")
                    } else {
                        pdfViewerContainer.visibility = View.GONE
                        Log.d("MainActivity", "onPostCreate: Contenitore PDF presente ma non dovrebbe essere visibile. Assicurato che sia GONE.")
                    }
                }
            }
        }

        binding.customSideMenu?.let {
            setupCustomSideMenuContent(it)
        }
    }

    /**
     * Configura il listener per la selezione degli elementi nella `NavigationRailView`.
     * Quando un elemento viene selezionato, chiama `handleNavigationItemSelected` per gestire la navigazione.
     * @param navRail L'istanza di `NavigationRailView` da configurare.
     */
    private fun setupNavigationRail(navRail: NavigationRailView) {
        navRail.setOnItemSelectedListener { item ->
            handleNavigationItemSelected(item.itemId)
            true
        }
    }

    /**
     * Configura il listener per la selezione degli elementi nella `BottomNavigationView`.
     * Quando un elemento viene selezionato, chiama `handleNavigationItemSelected` per gestire la navigazione.
     * @param bottomNav L'istanza di `BottomNavigationView` da configurare.
     */
    private fun setupBottomNavigationView(bottomNav: BottomNavigationView) {
        bottomNav.setOnItemSelectedListener { item ->
            handleNavigationItemSelected(item.itemId)
            true
        }
    }

    /**
     * Configura il contenuto visivo degli elementi all'interno del Custom Side Menu.
     * Imposta le icone e i testi per ciascuna voce del menu (Home, Folders, Shared, Settings)
     * e configura anche il titolo dell'header del menu.
     * @param customSideMenuBinding L'oggetto ViewBinding per il layout del custom side menu.
     */
    private fun setupCustomSideMenuContent(customSideMenuBinding: LayoutCustomSideMenuBinding) {
        try {
            val itemHome = customSideMenuBinding.customMenuItemHome.root
            val itemFolders = customSideMenuBinding.customMenuItemFolders.root
            val itemShared = customSideMenuBinding.customMenuItemShared.root
            val itemSettings = customSideMenuBinding.customMenuItemSettings.root

            setupCustomMenuItemContentVisuals(itemHome, R.drawable.home_24dp_000000_fill0_wght400_grad0_opsz24, R.string.home)
            setupCustomMenuItemContentVisuals(itemFolders, R.drawable.folder_open_24dp_000000_fill0_wght400_grad0_opsz24, R.string.folders)
            setupCustomMenuItemContentVisuals(itemShared, R.drawable.group_24dp_000000_fill0_wght400_grad0_opsz24, R.string.shared)
            setupCustomMenuItemContentVisuals(itemSettings, R.drawable.settings_24dp_000000_fill0_wght400_grad0_opsz24, R.string.settings)

            customSideMenuBinding.homeTitleTextView.text = getString(R.string.app_name)

        } catch (e: Exception) {
            Log.e("MainActivity", "Errore durante la configurazione visiva del menu laterale custom: ${e.message}", e)
        }
    }

    /**
     * Imposta l'aspetto visivo (icona, testo e colori di default) di un singolo elemento del Custom Side Menu.
     * @param itemCardView La `CardView` che rappresenta l'elemento del menu.
     * @param iconResId L'ID della risorsa drawable per l'icona dell'elemento.
     * @param textResId L'ID della risorsa stringa per il testo dell'elemento.
     */
    private fun setupCustomMenuItemContentVisuals(itemCardView: CardView, iconResId: Int, textResId: Int) {
        try {
            val linearLayout = itemCardView.findViewById<LinearLayout>(R.id.custom_menu_item_linear_layout)
            if (linearLayout == null) {
                Log.e("MainActivity", "ERRORE: LinearLayout custom_menu_item_linear_layout non trovato nella CardView per ${resources.getResourceEntryName(textResId)}")
                return
            }
            val icon = linearLayout.findViewById<ImageView>(R.id.custom_menu_item_icon)
            val text = linearLayout.findViewById<TextView>(R.id.custom_menu_item_text)

            icon?.setImageResource(iconResId) ?: Log.e("MainActivity", "ERRORE: ImageView custom_menu_item_icon non trovata per ${resources.getResourceEntryName(textResId)}")
            text?.setText(textResId) ?: Log.e("MainActivity", "ERRORE: TextView custom_menu_item_text non trovata per ${resources.getResourceEntryName(textResId)}")

            icon?.setColorFilter(ContextCompat.getColor(this, R.color.white))
            text?.setTextColor(ContextCompat.getColor(this, R.color.white))

        } catch (e: Exception) {
            Log.e("MainActivity", "Errore in setupCustomMenuItemContentVisuals (iconResId: ${resources.getResourceEntryName(iconResId)}, textResId: ${resources.getResourceEntryName(textResId)}): ${e.message}", e)
        }
    }

    /**
     * Aggiorna lo stato visivo (selezionato/deselezionato) di un elemento `CardView` nel Custom Side Menu.
     * Cambia il colore dell'icona e del testo per riflettere lo stato di selezione.
     * @param itemCardView L'elemento `CardView` da aggiornare.
     * @param isSelected `true` se l'elemento deve essere selezionato, `false` altrimenti.
     */
    private fun setCustomMenuItemSelected(itemCardView: CardView?, isSelected: Boolean) {
        if (itemCardView == null) {
            Log.e("MainActivity", "Tentativo di selezionare/deselezionare un item card view nullo.")
            return
        }
        try {
            val selectedTextColor = ContextCompat.getColor(this, R.color.white)
            val selectedIconTint = ContextCompat.getColor(this, R.color.white)

            val defaultTextColor = ContextCompat.getColor(this, R.color.white)
            val defaultIconTint = ContextCompat.getColor(this, R.color.white)

            val linearLayout = itemCardView.findViewById<LinearLayout>(R.id.custom_menu_item_linear_layout)
            if (linearLayout != null) {
                val icon = linearLayout.findViewById<ImageView>(R.id.custom_menu_item_icon)
                val text = linearLayout.findViewById<TextView>(R.id.custom_menu_item_text)

                icon?.setColorFilter(if (isSelected) selectedIconTint else defaultIconTint)
                text?.setTextColor(if (isSelected) selectedTextColor else defaultTextColor)
            } else {
                Log.e("MainActivity", "LinearLayout custom_menu_item_linear_layout non trovato per CardView in setCustomMenuItemSelected.")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Errore impostando item custom selezionato: ${e.message}", e)
        }
    }


    /**
     * Metodo unificato per gestire la selezione di un elemento di navigazione.
     * Applica una logica di debounce per prevenire doppi click.
     * Determina quale fragment caricare e se un viewer PDF deve essere rimosso/reso invisibile in layout tablet.
     * Sostituisce il fragment corrente con il `targetFragment` e aggiorna la selezione visiva della UI di navigazione.
     * @param itemId L'ID dell'elemento di navigazione selezionato (es. R.id.home, R.id.folders).
     */
    private fun handleNavigationItemSelected(itemId: Int) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNavigationClickTime < NAVIGATION_DEBOUNCE_TIME) {
            Log.d("MainActivity", "Click di navigazione ignorato (debounce): troppo veloce. Item ID: $itemId")
            return
        }
        lastNavigationClickTime = currentTime

        if (isFinishing || isDestroyed) {
            Log.w("MainActivity", "Ignorata selezione di navigazione, Activity in fase di chiusura. Item ID: $itemId")
            return
        }

        val pdfViewerContainer = findViewById<View>(R.id.fragment_pdf_viewer_container)
        val isLandscapeTabletLayout = pdfViewerContainer != null

        val isGoingToFoldersFragment = (itemId == R.id.folders)

        if (isLandscapeTabletLayout && !isGoingToFoldersFragment) {
            val pdfViewerFragment = supportFragmentManager.findFragmentById(R.id.fragment_pdf_viewer_container)
            if (pdfViewerFragment != null) {
                Log.d("MainActivity", "Navigazione verso una sezione diversa da 'Folders'. Rimuovo il PdfViewerFragment.")
                supportFragmentManager.beginTransaction()
                    .remove(pdfViewerFragment)
                    .commitAllowingStateLoss()
                pdfViewerContainer.visibility = View.GONE
            }
        }

        val targetFragment = when (itemId) {
            R.id.home -> HomeFragment()
            R.id.folders -> FoldersFragment()
            R.id.shared -> SharedFragment()
            R.id.settings -> SettingsFragment().apply {
            }
            else -> {
                Log.w("MainActivity", "ID elemento di navigazione non riconosciuto: $itemId. Nessun fragment da sostituire.")
                return
            }
        }

        val currentFragment = supportFragmentManager.findFragmentById(R.id.frame_layout)
        if (currentFragment != null && currentFragment::class == targetFragment::class) {
            Log.d("MainActivity", "Tentativo di sostituire con lo stesso tipo di Fragment: ${targetFragment.javaClass.simpleName}. Ignorata sostituzione del fragment.")
            updateNavigationSelection(itemId)
            return
        }

        replaceFragment(targetFragment)
        updateNavigationSelection(itemId)
    }

    /**
     * Callback chiamato quando un file PDF viene cliccato nel `FoldersFragment`.
     * Se il layout è tablet in modalità orizzontale, carica il `PdfViewerFragment` nel contenitore secondario.
     * Altrimenti, avvia una nuova `PDFViewerActivity` per visualizzare il PDF.
     * Gestisce anche i permessi URI per l'accesso persistente al file.
     * @param pdfUri L'URI del file PDF cliccato.
     */
    override fun onPdfFileClicked(pdfUri: Uri) {
        Log.d("MainActivity", "Ricevuto click PDF da FoldersFragment per URI: $pdfUri")

        val pdfViewerContainer = findViewById<View>(R.id.fragment_pdf_viewer_container)
        val isLandscapeTabletLayout = pdfViewerContainer != null

        if (isLandscapeTabletLayout) {
            Log.d("MainActivity", "Layout tablet landscape con contenitore PDF. Carico PdfViewerFragment.")
            val pdfViewerFragment = PdfViewerFragment.newInstance(pdfUri)

            try {
                contentResolver.takePersistableUriPermission(pdfUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d("MainActivity", "Permesso URI persistente preso per: $pdfUri")
            } catch (e: SecurityException) {
                Log.e("MainActivity", "Fallito tentativo di prendere permesso URI persistente per $pdfUri: ${e.message}. Tentativo di grant temporaneo.", e)
                grantUriPermission(packageName, pdfUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d("MainActivity", "Permesso URI temporaneo concesso per: $pdfUri")
            } catch (e: Exception) {
                Log.e("MainActivity", "Errore inatteso nel prendere permesso URI: ${e.message}", e)
                return
            }

            pdfViewerContainer.visibility = View.VISIBLE

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_pdf_viewer_container, pdfViewerFragment)
                .commitAllowingStateLoss()
        } else {
            Log.d("MainActivity", "Layout smartphone o tablet portrait. Avvio PDFViewerActivity.")
            val intent = Intent(this, PDFViewerActivity::class.java).apply {
                putExtra("pdf_uri", pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }
    }

    /**
     * Callback chiamato quando un file PDF viene cliccato con un'azione che forza l'apertura in una nuova Activity
     * (es. doppio click). Avvia sempre una `PDFViewerActivity` separata per visualizzare il PDF.
     * @param pdfUri L'URI del file PDF cliccato.
     */
    override fun onPdfFileClickedForceActivity(pdfUri: Uri) {
        Log.d("MainActivity", "Ricevuto DOPPIO CLIC PDF da FoldersFragment per URI: $pdfUri. Avvio forzato PDFViewerActivity.")
        val intent = Intent(this, PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    /**
     * Sostituisce il fragment attualmente visualizzato nel `frame_layout` con un nuovo fragment.
     * Utilizza una transazione del `FragmentManager` per eseguire la sostituzione in modo sicuro e gestire lo stato.
     * @param fragment Il nuovo fragment da visualizzare.
     */
    private fun replaceFragment(fragment: Fragment) {
        if (isFinishing || isDestroyed) {
            Log.w("MainActivity", "Ignorata sostituzione Fragment, Activity in fase di chiusura. Fragment: ${fragment.javaClass.simpleName}")
            return
        }

        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commitAllowingStateLoss()
            Log.d("MainActivity", "Fragment sostituito con: ${fragment.javaClass.simpleName}")
        } catch (e: IllegalStateException) {
            Log.e("MainActivity", "Errore durante la sostituzione del fragment: ${e.message}", e)
        }
    }


    /**
     * Aggiorna lo stato di selezione visiva dei componenti di navigazione (Custom Side Menu, NavigationRailView, BottomNavigationView)
     * per riflettere l'elemento attualmente selezionato. Questo assicura che la UI di navigazione sia sincronizzata con il fragment visualizzato.
     * @param itemId L'ID dell'elemento del menu che deve essere impostato come selezionato.
     */
    private fun updateNavigationSelection(itemId: Int) {
        Log.d("MainActivity", "Inizio updateNavigationSelection per ID: $itemId")

        binding.root.post {
            binding.customSideMenu?.let { customSideMenuBinding ->
                Log.d("MainActivity", "Resetting Custom Side Menu selection.")
                setCustomMenuItemSelected(customSideMenuBinding.customMenuItemHome.root, false)
                setCustomMenuItemSelected(customSideMenuBinding.customMenuItemFolders.root, false)
                setCustomMenuItemSelected(customSideMenuBinding.customMenuItemShared.root, false)
                setCustomMenuItemSelected(customSideMenuBinding.customMenuItemSettings.root, false)

                when (itemId) {
                    R.id.home -> setCustomMenuItemSelected(customSideMenuBinding.customMenuItemHome.root, true)
                    R.id.folders -> setCustomMenuItemSelected(customSideMenuBinding.customMenuItemFolders.root, true)
                    R.id.shared -> setCustomMenuItemSelected(customSideMenuBinding.customMenuItemShared.root, true)
                    R.id.settings -> setCustomMenuItemSelected(customSideMenuBinding.customMenuItemSettings.root, true)
                    else -> Log.w("MainActivity", "ID ($itemId) non gestito per selezione Custom Side Menu.")
                }
            }

            binding.navigationRail?.apply {
                if (isAttachedToWindow) {
                    val item = menu.findItem(itemId)
                    if (item != null && item.itemId != selectedItemId) {
                        selectedItemId = itemId
                        Log.d("MainActivity", "NavigationRailView selezionata con ID: $itemId")
                    } else if (item == null) {
                        Log.w("MainActivity", "ID ($itemId) non trovato nel menu NavigationRail. Impossibile selezionare.")
                        if (selectedItemId != -1) {
                            selectedItemId = -1
                            Log.d("MainActivity", "NavigationRailView deselezionata.")
                        }
                    }
                }
            }

            binding.bottomNavigationView?.apply {
                if (isAttachedToWindow) {
                    val item = menu.findItem(itemId)
                    if (item != null && item.itemId != selectedItemId) {
                        selectedItemId = itemId
                        Log.d("MainActivity", "BottomNavigationView selezionata con ID: $itemId")
                    } else if (item == null) {
                        Log.w("MainActivity", "ID ($itemId) non trovato nel menu BottomNavigationView. Impossibile selezionare.")
                        if (selectedItemId != -1) {
                            selectedItemId = -1
                            Log.d("MainActivity", "BottomNavigationView deselezionata.")
                        }
                    }
                }
            }
        }
        Log.d("MainActivity", "Fine updateNavigationSelection per ID: $itemId")
    }

    /**
     * Metodo helper che restituisce l'ID della risorsa del menu corrispondente a un dato tipo di Fragment.
     * Utilizzato per sincronizzare la selezione dei componenti di navigazione con il fragment attualmente visualizzato.
     * @param fragment L'istanza del Fragment di cui si vuole conoscere l'ID del menu corrispondente.
     * @return L'ID della risorsa del menu (es. R.id.home) o R.id.home come fallback.
     */
    private fun getMenuItemIdForFragment(fragment: Fragment): Int {
        return when (fragment) {
            is HomeFragment -> R.id.home
            is FoldersFragment -> R.id.folders
            is SharedFragment -> R.id.shared
            is SettingsFragment -> R.id.settings
            else -> {
                Log.w("MainActivity", "Fragment non riconosciuto per la sincronizzazione della selezione: ${fragment.javaClass.simpleName}. Defaulting to Home.")
                R.id.home
            }
        }
    }

    /**
     * Aggiorna lo stato di selezione dei componenti di navigazione in base al fragment attualmente visualizzato.
     * Questo metodo viene chiamato tipicamente durante il ripristino dello stato dell'Activity (es. dopo una rotazione)
     * per assicurare che la UI di navigazione rifletta correttamente il fragment attivo.
     * @param fragment Il fragment attualmente visualizzato per cui aggiornare la selezione.
     */
    private fun updateNavigationSelectionForFragment(fragment: Fragment) {
        val itemId = getMenuItemIdForFragment(fragment)
        updateNavigationSelection(itemId)
    }
}



