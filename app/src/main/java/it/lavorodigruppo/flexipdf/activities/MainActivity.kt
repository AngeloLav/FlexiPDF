/**
 * @file MainActivity.kt
 *
 * @overview
 *
 * Questa activity è il punto di ingresso dell'applicazione: ho utilizzato il viewBinding per potermi muovere attraverso
 * i diversi fragment dell'applicazione. All'inizio dichiaro delle variabili tramite lateinit (significa che le variabili
 * non saranno inizializzate immediatamente ma solo durante il loro primo utilizzo).
 *
 * binding: dichiarando questo oggetto della classe ActivityMainBinding (che viene generata automaticamente da AS quando nel
 * gradle:app aggiungi "buildFeatures { viewBinding true }") posso facilmente accedere a tutti i componenti dichiarati nel file
 * activity_main.xml. Attraverso binding = ActivityMainBinding.inflate(layoutInflater) e il metodo .inflate(), attacco al mio oggetto binding
 * il layout gonfiato e attraverso setContentView(binding.root) lo imposto come il layout della activity.
 * Attraverso il viewBinding secondo me è più facile impostare il layout rispetto all'uso di findViewById.
 * Utilizzo poi l'oggetto binding nel metodo onCreate per selezionare l'elemento bottomNavigationView: su questo uso il metodo
 * setOnItemSelectedListener per far in modo attraverso un when che quando clicco su un'icona del menù in base all'id dell'icona
 * selezionata si attivi la funzione replaceFragment che mi permette di selezionare il fragment corrispondente. Il true alla fine del metodo
 * è necessario perchè setOnItemSelectedListener ha bisogno di restituire un valore booleano; se fosse false vorrebbe dire che
 * l'evento non sarebbe stato gestito correttamente e non aggiornerebbe visivamente il bottomNav con l'item cliccato.
 *
 * pdfListViewModel: Questa variabile contiene un'istanza di PdfListViewModel: un ViewModel è una
 * classe progettata per memorizzare e gestire i dati relativi all'interfaccia utente in un modo che sopravviva ai cambiamenti
 * di configurazione dell'Activity (ad esempio, quando l'utente ruota lo schermo). pdfListViewModel gestisce la lista dei file PDF,
 * assicurandosi che non vada persa se l'Activity viene ricreata. L'inizializzazione avviene in 'onCreate'
 * tramite 'ViewModelProvider(this)[PdfListViewModel::class.java]', che garantisce il recupero o la creazione corretta del ViewModel. (this) fa in
 * modo che la MainActivity possieda il ciclo vita dei viewModel. [PdfListViewModel::class.java] mi permette di determinare il tipo
 * di viewModel che mi interessa; in questo caso è un PdfListViewModel.
 *      @see
 *      it.lavorodigruppo.flexipdf/viewmodels/PdfListViewModel.kt
 *
 * pdfManager: Questa variabile contiene un'istanza della classe personalizzata PdfManager: è responsabile di  operazioni
 * specifiche legate ai PDF. L'ho creata inizialmente per dividere la logica di alcune funzioni per i pdf dal fragment folders.
 * Durante la sua inizializzazione in onCreate, gli viene passato un callback (una funzione lambda: '{ uri: Uri, displayName: String -> ... }').
 * Un 'callback' è una funzione che verrà eseguita in un secondo momento, quando una specifica operazione è completata.
 * In questo caso, il 'PdfManager' invocherà questa funzione lambda quando l'utente avrà selezionato un PDF,
 * passando l'URI (Uniform Resource Identifier) del file e il suo nome. La lambda poi si occuperà di:
 * 1. Creare un nuovo oggetto PdfFileItem con le informazioni del PDF.
 * 2. Aggiungere questo nuovo PdfFileItem al pdfListViewModel, in modo che i dati della lista PDF vengano aggiornati.
 * 3. Mostrare un messaggio Toast all'utente per confermare l'avvenuta importazione del PDF.
 *      @see
 *      it.lavorodigruppo.flexipdf/utils/PdfManager
 *
 * replaceFragment: metodo che mi permette di cambiare fragment (come parametro riceve un'istanza del fragment da sostituire).
 * Crea prima un oggetto della classe FragmentManager, ereditata da AppCompatActivity. Attraverso questo oggetto con il metodo beginTransaction()
 * inizializzo l'oggetto e rendo possibile iniziare le operazioni sui fragment, con il metodo replace() seleziono e rimuovo qualunque fragment
 * attualmente presente nella mia activity_main.xml (R.id.frame_layout è l'id del FrameLayout in activity_main.xml) e lo sostituisco con il nuovo
 * fragment passato come parametro. commit() è un metodo che mi permette di finalizzare questi cambiamenti.
 *
 *
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.ContextCompat

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView // Import corretto per onItemSelectedListener
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

    private lateinit var binding: ActivityMainBinding
    private lateinit var pdfManager: PdfManager
    private lateinit var fileSystemViewModel: FileSystemViewModel

    private var lastNavigationClickTime: Long = 0
    private val NAVIGATION_DEBOUNCE_TIME = 100L // Tempo in millisecondi per il debounce

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

    override fun launchPdfPicker() {
        pickPdfLauncher.launch(arrayOf("application/pdf"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toast.makeText(this, getString(R.string.changed_language),
            Toast.LENGTH_SHORT).show()

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

        // Determina quale componente di navigazione è presente nel layout attuale
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

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Log.d("MainActivity", "onPostCreate chiamato.")

        // Esegui la sostituzione iniziale del fragment e l'aggiornamento della UI qui
        // Questa parte è cruciale per la gestione delle rotazioni e per evitare la ricreazione/sovrapposizione.
        if (savedInstanceState == null) {
            // SOLO se l'Activity viene creata per la prima volta (non ricreata da rotazione/process kill)
            // Seleziona il fragment Home all'avvio iniziale
            handleNavigationItemSelected(R.id.home) // Chiama handleNavigationItemSelected per il primo avvio
            Log.d("MainActivity", "onPostCreate: savedInstanceState è null. Caricato HomeFragment iniziale.")
        } else {
            // Se l'Activity è stata ricreata (es. cambio configurazione/rotazione)
            val currentFragmentInFrameLayout = supportFragmentManager.findFragmentById(R.id.frame_layout)
            currentFragmentInFrameLayout?.let {
                updateNavigationSelectionForFragment(it)
                Log.d("MainActivity", "onPostCreate: savedInstanceState NON è null. Aggiornata UI di navigazione per ${it.javaClass.simpleName}.")

                val pdfViewerContainer = findViewById<View>(R.id.fragment_pdf_viewer_container)

                // Determina se il FoldersFragment è il fragment attivo nel pannello principale
                val isFoldersFragmentActive = it is FoldersFragment
                // Determina se c'era un PdfViewerFragment nel contenitore secondario prima della ricreazione
                val pdfViewerFragmentRestored = supportFragmentManager.findFragmentById(R.id.fragment_pdf_viewer_container) != null

                if (pdfViewerContainer != null) { // Solo se siamo in un layout con il contenitore PDF (e quindi tablet landscape)
                    if (isFoldersFragmentActive && pdfViewerFragmentRestored) {
                        // Se il FoldersFragment è attivo E un PDFViewerFragment è stato ripristinato,
                        // allora rende il contenitore del PDF visibile.
                        pdfViewerContainer.visibility = View.VISIBLE
                        Log.d("MainActivity", "onPostCreate: FoldersFragment attivo e PdfViewerFragment ripristinato. Reso visibile contenitore PDF.")
                    } else {
                        // In tutti gli altri casi (es. HomeFragment attivo, o nessun PDF era aperto),
                        // assicurati che il contenitore del PDF sia GONE.
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

    private fun setupNavigationRail(navRail: NavigationRailView) {
        navRail.setOnItemSelectedListener { item ->
            handleNavigationItemSelected(item.itemId)
            true
        }
    }

    private fun setupBottomNavigationView(bottomNav: BottomNavigationView) {
        bottomNav.setOnItemSelectedListener { item ->
            handleNavigationItemSelected(item.itemId)
            true
        }
    }

    // Metodo per configurare il contenuto visivo del Custom Side Menu
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

            // Setup titolo e sottotitolo nell'header del custom menu
            customSideMenuBinding.homeTitleTextView.text = getString(R.string.app_name)
            // customSideMenuBinding.customSideMenuAppSubtitle.text = getString(R.string.app_subtitle_placeholder) // Rimuovi o decommenta se hai questa stringa

        } catch (e: Exception) {
            Log.e("MainActivity", "Errore durante la configurazione visiva del menu laterale custom: ${e.message}", e)
        }
    }

    // Gestisce solo l'aspetto visivo (icone, testo, colori default)
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

            // Colori iniziali (non selezionati)
            icon?.setColorFilter(ContextCompat.getColor(this, R.color.white))
            text?.setTextColor(ContextCompat.getColor(this, R.color.white))

        } catch (e: Exception) {
            Log.e("MainActivity", "Errore in setupCustomMenuItemContentVisuals (iconResId: ${resources.getResourceEntryName(iconResId)}, textResId: ${resources.getResourceEntryName(textResId)}): ${e.message}", e)
        }
    }

    // Questo metodo ora è un helper per cambiare l'aspetto del custom menu item quando selezionato/deselezionato
    private fun setCustomMenuItemSelected(itemCardView: CardView?, isSelected: Boolean) {
        if (itemCardView == null) {
            Log.e("MainActivity", "Tentativo di selezionare/deselezionare un item card view nullo.")
            return
        }
        try {
            // Definisci i colori per lo stato selezionato e non selezionato
            val selectedTextColor = ContextCompat.getColor(this, R.color.white) // Colore per il testo selezionato
            val selectedIconTint = ContextCompat.getColor(this, R.color.white) // Colore per l'icona selezionata
            // Colore per lo sfondo selezionato (es. light blue)

            val defaultTextColor = ContextCompat.getColor(this, R.color.white) // Colore per il testo non selezionato
            val defaultIconTint = ContextCompat.getColor(this, R.color.white) // Colore per l'icona non selezionata
            val defaultBackgroundColor = ContextCompat.getColor(this, android.R.color.transparent) // Sfondo trasparente per non selezionato

            // Applica il colore di sfondo alla CardView


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

    // Metodo unificato per gestire la selezione degli elementi di navigazione
    private fun handleNavigationItemSelected(itemId: Int) {

        //DEBOUNCING
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNavigationClickTime < NAVIGATION_DEBOUNCE_TIME) {
            Log.d("MainActivity", "Click di navigazione ignorato (debounce): troppo veloce. Item ID: $itemId")
            return // Ignora il click
        }
        lastNavigationClickTime = currentTime // Aggiorna il tempo dell'ultimo click valido

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
            // MODIFICA QUI: Istanzia FoldersFragment senza argomenti
            R.id.folders -> FoldersFragment()
            R.id.shared -> SharedFragment()
            R.id.settings -> SettingsFragment().apply {
                // Assicurati di impostare il listener per il cambio tema qui, se necessario
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

    override fun onPdfFileClicked(pdfUri: Uri) {
        Log.d("MainActivity", "Ricevuto click PDF da FoldersFragment per URI: $pdfUri")

        val pdfViewerContainer = findViewById<View>(R.id.fragment_pdf_viewer_container)
        val isLandscapeTabletLayout = pdfViewerContainer != null

        if (isLandscapeTabletLayout) {
            Log.d("MainActivity", "Layout tablet landscape con contenitore PDF. Carico PdfViewerFragment.")
            val pdfViewerFragment = PdfViewerFragment.newInstance(pdfUri)

            // --- GESTIONE PERMESSI URI (come da ultima versione) ---
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
            // --- FINE GESTIONE PERMESSI URI ---

            // Rende il contenitore VISIBILE e poi esegue la transazione
            pdfViewerContainer.visibility = View.VISIBLE

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_pdf_viewer_container, pdfViewerFragment)
                .commitAllowingStateLoss()
        } else {
            // Altrimenti (smartphone o tablet portrait), apri l'Activity dedicata al viewer PDF
            Log.d("MainActivity", "Layout smartphone o tablet portrait. Avvio PDFViewerActivity.")
            val intent = Intent(this, PDFViewerActivity::class.java).apply {
                putExtra("pdf_uri", pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }
    }

    override fun onPdfFileClickedForceActivity(pdfUri: Uri) {
        Log.d("MainActivity", "Ricevuto DOPPIO CLIC PDF da FoldersFragment per URI: $pdfUri. Avvio forzato PDFViewerActivity.")
        val intent = Intent(this, PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun replaceFragment(fragment: Fragment) {
        if (isFinishing || isDestroyed) {
            Log.w("MainActivity", "Ignorata sostituzione Fragment, Activity in fase di chiusura. Fragment: ${fragment.javaClass.simpleName}")
            return
        }

        try {
            // Semplificazione: usa sempre replace, che gestisce la rimozione del precedente
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commitAllowingStateLoss() // commitAllowStateLoss è preferibile a commitNowAllowStateLoss per evitare blocchi dell'UI se non strettamente necessario, e gestisce meglio gli stati di Activity
            Log.d("MainActivity", "Fragment sostituito con: ${fragment.javaClass.simpleName}")
        } catch (e: IllegalStateException) {
            Log.e("MainActivity", "Errore durante la sostituzione del fragment: ${e.message}", e)
        }
    }


    // Gestisce l'aggiornamento della selezione visiva dei componenti di navigazione
    private fun updateNavigationSelection(itemId: Int) {
        Log.d("MainActivity", "Inizio updateNavigationSelection per ID: $itemId")

        // Usa View.post qui. È probabile che fosse necessario per il timing dell'UI.
        binding.root.post {
            // 1. Gestione del Custom Side Menu
            binding.customSideMenu?.let { customSideMenuBinding ->
                Log.d("MainActivity", "Resetting Custom Side Menu selection.")
                // Deseleziona tutti gli elementi del custom menu
                setCustomMenuItemSelected(customSideMenuBinding.customMenuItemHome.root, false)
                setCustomMenuItemSelected(customSideMenuBinding.customMenuItemFolders.root, false)
                setCustomMenuItemSelected(customSideMenuBinding.customMenuItemShared.root, false)
                setCustomMenuItemSelected(customSideMenuBinding.customMenuItemSettings.root, false)

                // Seleziona l'item corretto nel custom menu
                when (itemId) {
                    R.id.home -> setCustomMenuItemSelected(customSideMenuBinding.customMenuItemHome.root, true)
                    R.id.folders -> setCustomMenuItemSelected(customSideMenuBinding.customMenuItemFolders.root, true)
                    R.id.shared -> setCustomMenuItemSelected(customSideMenuBinding.customMenuItemShared.root, true)
                    R.id.settings -> setCustomMenuItemSelected(customSideMenuBinding.customMenuItemSettings.root, true)
                    else -> Log.w("MainActivity", "ID ($itemId) non gestito per selezione Custom Side Menu.")
                }
            }

            // 2. Gestione di NavigationRailView
            binding.navigationRail?.apply {
                if (isAttachedToWindow) {
                    val item = menu.findItem(itemId)
                    if (item != null && item.itemId != selectedItemId) { // Controlla anche se non è già selezionato
                        selectedItemId = itemId
                        Log.d("MainActivity", "NavigationRailView selezionata con ID: $itemId")
                    } else if (item == null) {
                        Log.w("MainActivity", "ID ($itemId) non trovato nel menu NavigationRail. Impossibile selezionare.")
                        // Se l'ID non è nel menu, deseleziona tutto per evitare stati falsi
                        if (selectedItemId != -1) {
                            selectedItemId = -1 // Deseleziona
                            Log.d("MainActivity", "NavigationRailView deselezionata.")
                        }
                    }
                }
            }

            // 3. Gestione di BottomNavigationView
            binding.bottomNavigationView?.apply {
                if (isAttachedToWindow) {
                    val item = menu.findItem(itemId)
                    if (item != null && item.itemId != selectedItemId) { // Controlla anche se non è già selezionato
                        selectedItemId = itemId
                        Log.d("MainActivity", "BottomNavigationView selezionata con ID: $itemId")
                    } else if (item == null) {
                        Log.w("MainActivity", "ID ($itemId) non trovato nel menu BottomNavigationView. Impossibile selezionare.")
                        // Se l'ID non è nel menu, deseleziona tutto per evitare stati falsi
                        if (selectedItemId != -1) {
                            selectedItemId = -1 // Deseleziona
                            Log.d("MainActivity", "BottomNavigationView deselezionata.")
                        }
                    }
                }
            }
        } // Fine binding.root.post
        Log.d("MainActivity", "Fine updateNavigationSelection per ID: $itemId")
    }

    // Helper per ottenere l'ID del menu corrispondente a un fragment
    private fun getMenuItemIdForFragment(fragment: Fragment): Int {
        return when (fragment) {
            is HomeFragment -> R.id.home
            is FoldersFragment -> R.id.folders
            is SharedFragment -> R.id.shared
            is SettingsFragment -> R.id.settings
            else -> {
                Log.w("MainActivity", "Fragment non riconosciuto per la sincronizzazione della selezione: ${fragment.javaClass.simpleName}. Defaulting to Home.")
                R.id.home // Fallback per sicurezza
            }
        }
    }

    // Questoo metodo è chiamato solo in onPostCreate per inizializzare/ripristinare lo stato della UI
    private fun updateNavigationSelectionForFragment(fragment: Fragment) {
        val itemId = getMenuItemIdForFragment(fragment)
        updateNavigationSelection(itemId)
    }
}



