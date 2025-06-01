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

import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import it.lavorodigruppo.flexipdf.databinding.FragmentFoldersBinding
import android.widget.PopupWindow
import android.widget.Toast
import it.lavorodigruppo.flexipdf.databinding.CustomPopupMenuBinding
import android.animation.ObjectAnimator
import android.content.Context
import android.view.animation.OvershootInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import it.lavorodigruppo.flexipdf.adapters.OnPdfFileClickListener
import it.lavorodigruppo.flexipdf.adapters.PdfFileAdapter
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import it.lavorodigruppo.flexipdf.viewmodels.PdfListViewModel
import it.lavorodigruppo.flexipdf.activities.PDFViewerActivity
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri


/**
 * Interfaccia di callback per notificare all'Activity ospitante che è necessario
 * avviare il selettore di file PDF.
 * Questo pattern garantisce una comunicazione pulita tra Fragment e Activity.
 */
interface OnPdfPickerListener {
    /**
     * Chiamato quando è necessario avviare il selettore di file PDF.
     */
    fun launchPdfPicker()
}

/**
 * FoldersFragment è il Fragment principale che visualizza la lista dei file PDF.
 * Implementa OnPdfFileClickListener per gestire i click sui singoli elementi della lista.
 *
 * Gestisce anche l'interazione con un Floating Action Button (FAB) per mostrare un popup
 * con opzioni aggiuntive come l'importazione di PDF o la creazione di cartelle.
 */
class FoldersFragment : Fragment(), OnPdfFileClickListener {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    // Listener per la comunicazione con l'Activity per l'apertura del PDF picker.
    private var listener: OnPdfPickerListener? = null

    // ViewModel e Adapter per la gestione della lista dei PDF.
    private lateinit var pdfListViewModel: PdfListViewModel
    private lateinit var pdfFileAdapter: PdfFileAdapter

    /**
     * @param inflater L'LayoutInflater per gonfiare il layout.
     * @param container Il ViewGroup genitore a cui la UI del Fragment dovrebbe essere attaccata.
     * @param savedInstanceState Se non nullo, questo Fragment sta venendo ricreato da un precedente stato salvato.
     * @return La View radice del layout del Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Gonfia il layout 'fragment_folders.xml' utilizzando View Binding.
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Metodo chiamato subito dopo che onCreateView ha restituito la View,
     * e la gerarchia delle View del Fragment è stata completamente creata.
     * Questo è il luogo ideale per inizializzare le View, impostare i listener,
     * e osservare i LiveData dal ViewModel.
     *
     * @param view La View radice del Fragment (uguale a `binding.root`).
     * @param savedInstanceState Lo stato salvato del Fragment.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ottiene un'istanza del PdfListViewModel.
        // `requireActivity()` garantisce che il ViewModel sia con scope all'Activity,
        // permettendo la condivisione tra più Fragment nella stessa Activity.
        pdfListViewModel = ViewModelProvider(requireActivity())[PdfListViewModel::class.java]

        // Inizializza l'adapter per la RecyclerView, passando 'this' (il Fragment) come listener per i click.
        pdfFileAdapter = PdfFileAdapter(this)

        // Configura la RecyclerView:
        binding.pdfRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pdfFileAdapter
            setHasFixedSize(true) // Ottimizzazione per performance se la dimensione degli item non cambia.
        }

        // Osserva il LiveData 'pdfFiles' dal ViewModel.
        // Ogni volta che la lista di PDF cambia nel ViewModel, l'adapter viene aggiornato
        // con la nuova lista tramite submitList(), e la RecyclerView si ridisegna.
        pdfListViewModel.pdfFiles.observe(viewLifecycleOwner) { pdfFiles ->
            pdfFileAdapter.submitList(pdfFiles)
        }

        // --- Gestione degli WindowInsets ---
        val bannerContentLayout = binding.bannerContentLayout

        ViewCompat.setOnApplyWindowInsetsListener(bannerContentLayout) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                v.paddingLeft,
                systemBarsInsets.top,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }
        // --- Fine della gestione degli WindowInsets ---

        // Imposta il click listener per il Floating Action Button (FAB).
        binding.floatingActionButton.setOnClickListener {
            rotateFabForward()
            showPopupMenu()
        }
    }

    /**
     * Metodo chiamato quando la View del Fragment sta per essere distrutta.
     * È cruciale nullificare l'oggetto View Binding qui per prevenire memory leak,
     * poiché la View non è più valida e il binding conterrebbe riferimenti a essa.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Altre funzioni ---

    /**
     * Implementazione del metodo onPdfFileClick dell'interfaccia OnPdfFileClickListener.
     * Chiamato quando un elemento PDF nella lista viene cliccato.
     * Avvia il PDFViewerActivity per visualizzare il PDF selezionato, passando
     * l'URI e il nome visualizzato del file tramite un Intent.
     *
     * @param pdfFile L'oggetto [PdfFileItem] del PDF cliccato.
     */
    override fun onPdfFileClick(pdfFile: PdfFileItem) {

        val intent = Intent(requireContext(), PDFViewerActivity::class.java).apply {
            // Passa l'URI del PDF come Stringa convertita in Uri.
            putExtra("pdf_uri", pdfFile.uriString.toUri())
            // Passa il nome visualizzato del PDF.
            putExtra("pdf_display_name", pdfFile.displayName)
        }
        startActivity(intent)
        // Mostra un Toast di conferma all'utente.
        Toast.makeText(context, "Opening ${pdfFile.displayName}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Mostra un popup menu personalizzato centrato sopra il Floating Action Button (FAB).
     * Il popup offre opzioni come l'importazione di PDF o la creazione di cartelle.
     * Gestisce il posizionamento del popup per apparire correttamente rispetto al FAB.
     */
    private fun showPopupMenu() {
        // Gonfia il layout del popup menu utilizzando View Binding.
        val popupBinding = CustomPopupMenuBinding.inflate(LayoutInflater.from(requireContext()))
        // La proprietà .root di questo oggetto popupBinding
        // ti restituisce direttamente la View genitore più esterna (quella che racchiude tutte le altre)
        // del layout custom_popup_menu.xml
        val popupView = popupBinding.root

        // Misura la view del popup per calcolare le sue dimensioni prima di mostrarla.
        // Questo è necessario per posizionare correttamente il popup rispetto al FAB.
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        // Crea un'istanza di PopupWindow.
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT, // Larghezza del popup
            ViewGroup.LayoutParams.WRAP_CONTENT, // Altezza del popup
            true // Rendere il popup focusable per poterlo chiudere cliccando fuori.
        )

        // Ottiene la posizione (X, Y) del FAB sullo schermo.
        val location = IntArray(2)
        binding.floatingActionButton.getLocationOnScreen(location)

        val fabX = location[0] // Coordinata X del FAB
        val fabY = location[1] // Coordinata Y del FAB
        val popupWidth = popupView.measuredWidth // Larghezza misurata del popup
        val fabWidth = binding.floatingActionButton.width // Larghezza del FAB

        // Calcola l'offset X per posizionare il popup leggermente a sinistra del centro del FAB.
        val xOffset = fabX - popupWidth + fabWidth/3
        // Calcola l'offset Y per posizionare il popup sopra il FAB.
        val yOffset = fabY - popupView.measuredHeight

        // Aggiunge un margine extra per separare il popup dal FAB.
        val margin = (15 * resources.displayMetrics.density).toInt()
        val finalYOffset = yOffset - margin

        // Mostra il popup in una posizione specifica sullo schermo.
        popupWindow.showAtLocation(
            binding.root,
            Gravity.NO_GRAVITY,
            xOffset,
            finalYOffset
        )

        // --- Listeners per le opzioni del popup ---

        // Listener per l'opzione "Importa PDF".
        popupBinding.optionImportPdf.setOnClickListener {
            // Notifica all'Activity tramite il listener di avviare il selettore PDF.
            listener?.launchPdfPicker()
            popupWindow.dismiss()
        }

        // Listener per l'opzione "Crea nuova cartella".
        popupBinding.optionCreateFolder.setOnClickListener {
            Toast.makeText(context, "Create new folder clicked!", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }
    }

    // --- Gestione del PDF Picker ---

    /**
     *
     * Questi metodi assicurano che il listener verso l'Activity sia gestito correttamente
     * durante il ciclo di vita del Fragment.
     *
     * Questa sezione del codice gestisce la comunicazione tra il FoldersFragment e l'Activity
     * che lo ospita MainActivity, per richiedere l'avvio del selettore
     * di file PDF. Questo pattern di comunicazione è una best practice per garantire
     * disaccoppiamento, sicurezza del tipo e corretta gestione del ciclo di vita.
     *
     * Definizione dell'Interfaccia: OnPdfPickerListener (all'inizio di questo file)
     * Questa interfaccia agisce come un contratto. Dichiarando `fun launchPdfPicker()`,
     * impone che qualsiasi classe che voglia ricevere richieste
     * da questo Fragment per avviare il PDF picker, debba implementare tale metodo.
     * Questo assicura che il Fragment non abbia conoscenze dirette sulla classe specifica
     * dell'Activity, promuovendo il disaccoppiamento.
     *
     * FoldersFragment richiede un listener
     * Il Fragment ha il compito di reagire all'interazione dell'utente (es. clic sull'opzione
     * "Importa PDF" nel popup del FAB). Tuttavia, l'azione concreta di avviare un selettore
     * di file di sistema e gestire il risultato è un compito che idealmente spetta all'Activity
     * (poiché l'Activity è più adatta a gestire permessi, risultati di altre Activity, ecc.).
     * Perciò, il Fragment "richiede" un listener (l'Activity) che si faccia carico di questa operazione.
     * La variabile `listener: OnPdfPickerListener?` nel Fragment è il riferimento a questo "qualcuno".
     *
     * L'Activity implementa l'interfaccia
     * L'Activity ospitante (ad esempio, la tua `MainActivity`) deve dichiarare esplicitamente
     * che implementa il contratto OnPdfPickerListener
     *
     *
     * Associazione in onAttach(context: Context)
     * Questo metodo del ciclo di vita del Fragment viene chiamato quando il Fragment
     * è stato associato al suo Context (che è l'Activity).
     * - `context` è il riferimento all'Activity ospitante.
     * - La condizione `if (context is OnPdfPickerListener)` verifica dinamicamente
     * se l'Activity che ospita il Fragment implementa il contratto definito.
     * - Se la condizione è vera, `listener = context` assegna il riferimento all'Activity
     * alla variabile `listener` del Fragment. Ora il Fragment ha un modo sicuro e tipizzato
     * per chiamare i metodi definiti nell'interfaccia sull'Activity.
     * - La `throw RuntimeException(...)` agisce come un "fallimento rapido": se, per errore,
     * il Fragment fosse ospitato da un'Activity che non implementa OnPdfPickerListener,
     * un'eccezione chiara avvertirebbe immediatamente dello sbaglio di implementazione.
     *
     * Comunicazione dal Fragment all'Activity
     * Quando l'utente attiva l'azione che richiede l'avvio del PDF picker (es. cliccando
     * sull'opzione "Importa PDF" nel popup), il Fragment chiama `listener?.launchPdfPicker()`.
     * Dato che `listener` punta all'istanza della tua `MainActivity` (o equivalente),
     * questa chiamata si traduce nell'esecuzione del metodo `launchPdfPicker()` definito
     * nell'Activity.
     *
     * Disassociazione in onDetach()]
     * Questo metodo del ciclo di vita viene chiamato quando il Fragment sta per essere
     * disassociato dal suo Context (l'Activity).
     * - `listener = null` rimuove il riferimento all'Activity dalla variabile `listener`.
     * - Questa pulizia è fondamentale per prevenire memory leak. Se il riferimento
     * all'Activity non venisse nullificato, e l'Activity dovesse essere distrutta
     * (es. a seguito di un cambio di configurazione o chiusura dell'app), il Fragment
     * manterrebbe un riferimento a un'istanza "morta" dell'Activity, impedendone
     * la raccolta da parte del garbage collector e sprecando memoria.
     *
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPdfPickerListener) {
            listener = context
        } else {
            // Lancia un'eccezione se l'Activity non implementa il listener richiesto.
            throw RuntimeException("$context must implement OnPdfPickerListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
    // --- Fine della gestione del PDF Picker ---

    /**
     * Esegue un'animazione di rotazione sul Floating Action Button (FAB).
     * Utilizza ObjectAnimator per ruotare il FAB di 90 gradi con un effetto di "overshoot"
     * (rimbalzo) per una migliore sensazione utente.
     */
    private fun rotateFabForward() {
        ObjectAnimator.ofFloat(binding.floatingActionButton, "rotation", 0f, 90f).apply {
            duration = 500 // Durata dell'animazione in millisecondi.
            interpolator = OvershootInterpolator()
            start()
        }
    }

}