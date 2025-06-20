/**
 * @file PDFViewerActivity.kt
 *
 * @brief Activity dedicata alla visualizzazione di documenti PDF.
 *
 * @overview
 * Questa Activity è responsabile di caricare e mostrare un file PDF, il cui URI (Uniform Resource Identifier)
 * viene passato tramite un Intent dalla Activity chiamante. Utilizza la libreria com.github.barteksc.pdfviewer.PDFView
 * per un rendering efficiente e interattivo dei PDF.
 *
 */
package it.lavorodigruppo.flexipdf.activities

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.databinding.ActivityPdfViewerFoldableBinding
import it.lavorodigruppo.flexipdf.fragments.PdfViewerFragment
import it.lavorodigruppo.flexipdf.interfaces.PdfLoadCallback
import kotlinx.coroutines.launch
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor


class PDFViewerActivity : AppCompatActivity(), PdfLoadCallback {

    private var totalPdfPages: Int = 0
    private var pdfUri: Uri? = null
    private var currentPage: Int = 0 // La pagina corrente (0-indexed). Per foldable, è la pagina di sinistra.
    // IMPORTE: Inizializziamo a TRUE per forzare la prima esecuzione di updateLayout(false)
    private var currentLayoutIsFoldable: Boolean = true // Variabile per tenere traccia dello stato del layout


    private var binding: ActivityPdfViewerFoldableBinding? = null // Usiamo il binding per il layout foldable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Imposto il layout foldable come layout principale UNA SOLA VOLTA in onCreate
        binding = ActivityPdfViewerFoldableBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        this.pdfUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("pdf_uri", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("pdf_uri")
        } ?: run {
            Log.e("PDFViewerActivity", "Nessun PDF URI fornito.")
            Toast.makeText(this, "Nessun PDF specificato.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // <--- CRUCIALE: GESTIONE DEI PERMESSI URI PER LA LETTURA DEL FILE --->
        pdfUri?.let {
            try {
                // Tentativo di ottenere un permesso persistente (migliore per l'accesso futuro)
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d("PDFViewerActivity", "Permesso URI persistente concesso per: $it")
            } catch (e: SecurityException) {
                // Se il permesso persistente fallisce (es. non supportato dal Content Provider),
                // tenta di ottenere un permesso temporaneo.
                Log.e("PDFViewerActivity", "Impossibile ottenere permesso URI persistente: ${e.message}. Tentativo di permesso temporaneo.")
                try {
                    grantUriPermission(packageName, it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    Log.d("PDFViewerActivity", "Permesso URI temporaneo concesso per: $it")
                } catch (e2: Exception) {
                    // Se anche il permesso temporaneo fallisce, non possiamo caricare il PDF.
                    Log.e("PDFViewerActivity", "Impossibile ottenere permesso URI temporaneo: ${e2.message}", e2)
                    Toast.makeText(this, "Errore permessi file: ${e2.message}", Toast.LENGTH_LONG).show()
                    finish()
                    return // Termina l'Activity se i permessi non sono disponibili
                }
            } catch (e: Exception) {
                // Catch per altri tipi di eccezioni durante la gestione dei permessi URI
                Log.e("PDFViewerActivity", "Errore generico nell'ottenere permessi URI: ${e.message}", e)
                Toast.makeText(this, "Errore permessi file: ${e.message}", Toast.LENGTH_LONG).show()

                finish()
                return // Termina l'Activity
            }
        }
        // <--- FINE GESTIONE PERMESSI URI --->


        // NUOVO: Funzione per ottenere il conteggio totale delle pagine usando PdfRenderer
        fun getAbsolutePdfPageCount(uri: Uri): Int {
            var pageCount = 0
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                pfd = contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    renderer = PdfRenderer(pfd)
                    pageCount = renderer.pageCount
                    Log.d("PDFViewerActivity", "PdfRenderer per URI: $uri ha ${pageCount} pagine.")
                }
            } catch (e: Exception) {
                Log.e("PDFViewerActivity", "Errore nell'ottenere il conteggio pagine PDF da URI: ${e.message}", e)
            } finally {
                renderer?.close()
                pfd?.close()
            }
            return pageCount
        }

        // --- INIZIO MODIFICA CRUCIALE: Ottieni il conteggio totale delle pagine all'avvio ---
        // Questa è la fonte autorevole del numero di pagine totali del documento.
        pdfUri?.let { uri ->
            totalPdfPages = getAbsolutePdfPageCount(uri)
            Log.d("PDFViewerActivity", "Conteggio pagine PDF totale (da PdfRenderer): $totalPdfPages")
        } ?: run {
            Log.e("PDFViewerActivity", "Impossibile ottenere il conteggio totale delle pagine: URI PDF nullo.")
            totalPdfPages = 0 // Assicura che sia 0 se l'URI è nullo
        }
        // --- FINE MODIFICA CRUCIALE ---


        // Ripristina lo stato dell'Activity se è stata ricreata (es. dopo rotazione o configurazione)
        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt("currentPage", 0)
            totalPdfPages = savedInstanceState.getInt("totalPdfPages", 0)
            // Se lo stato è ripristinato, currentLayoutIsFoldable deve riflettere quello dello stato salvato,
            // non quello iniziale (true).
            currentLayoutIsFoldable = savedInstanceState.getBoolean("currentLayoutIsFoldable", false)
            Log.d("PDFViewerActivity", "Stato ripristinato: Pagina=${currentPage}, Totale=${totalPdfPages}, LayoutFoldable=${currentLayoutIsFoldable}")
        } else {
            Log.d("PDFViewerActivity", "Nessuno stato salvato. Avvio pagina: 0.")
        }

        // --- INIZIO MODIFICA: Aggiungi i click listener per i bottoni ---
        binding?.btnPrev?.setOnClickListener {
            Log.d("PDFViewerActivity", "Pulsante 'Indietro' cliccato. Chiamo navigatePages(-2).")
            navigatePages(-2)
        }
        binding?.btnNext?.setOnClickListener {
            Log.d("PDFViewerActivity", "Pulsante 'Avanti' cliccato. Chiamo navigatePages(2).")
            navigatePages(2)
        }
        // --- FINE MODIFICA ---

        // Inizia a osservare i cambiamenti di layout del dispositivo foldable in un Coroutine Scope
        lifecycleScope.launch {
            WindowInfoTracker.getOrCreate(this@PDFViewerActivity)
                .windowLayoutInfo(this@PDFViewerActivity)
                .collect { newLayoutInfo ->
                    // Cerca una FoldingFeature (cerniera o piega) nel layout
                    val foldingFeature = newLayoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()

                    // Determina se il dispositivo è in modalità "semi-aperta" con cerniera verticale (modalità a libro)
                    val isInFoldableSemiOpenPortraitMode = foldingFeature != null &&
                            foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                            foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL

                    Log.d("PDFViewerActivity", "WindowLayoutInfo aggiornato. IsFoldableSemiOpenPortraitMode = $isInFoldableSemiOpenPortraitMode, Current Orientation = ${resources.configuration.orientation}")

                    // La logica per decidere se il layout dovrebbe essere "foldable" (due pagine)
                    // richiede che sia in modalità semi-aperta E l'orientamento generale sia portrait
                    val shouldBeFoldable = isInFoldableSemiOpenPortraitMode && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

                    // Chiama la funzione per aggiornare il layout dell'Activity in base a shouldBeFoldable
                    updateLayout(shouldBeFoldable)
                }
        }

        // Imposta il layout predefinito (singolo) all'avvio dell'Activity, solo se non è un ripristino di stato.
        // La variabile currentLayoutIsFoldable è inizializzata a true per forzare l'aggiornamento iniziale a false.
        if (savedInstanceState == null) {
            updateLayout(false) // Avvia in modalità single-pane per default
        } else {
            // Se c'è uno stato salvato, ripristina il layout al suo stato originale
            updateLayout(currentLayoutIsFoldable)
        }
        // NUOVO: Chiama updateNavigationButtonStates all'avvio per impostare lo stato iniziale dei bottoni
        updateNavigationButtonStates()
    }

    // Salva lo stato corrente dell'Activity (pagina e totale pagine) prima che venga distrutta
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentPage", currentPage)
        outState.putInt("totalPdfPages", totalPdfPages)
        Log.d("PDFViewerActivity", "Stato salvato: Pagina=${currentPage}, Totale=${totalPdfPages}, LayoutFoldable=${currentLayoutIsFoldable}")
    }

    // MODIFICATO: Questa callback ora serve solo per segnalare il caricamento del fragment
    // e triggerare l'aggiornamento dello stato dei bottoni, non per impostare totalPdfPages.
    override fun onPdfFragmentLoaded(nbPagesFromFragment: Int) {
        // Log.d("PDFViewerActivity", "PDF caricato in fragment: $nbPagesFromFragment pagine (Conteggio totale attività: ${this.totalPdfPages})")
        // Non aggiorniamo totalPdfPages qui, è già gestito da getAbsolutePdfPageCount in onCreate.
        updateNavigationButtonStates()
    }

    /**
     * Funzione centrale per aggiornare il layout dell'Activity (caricando/nascondendo i Fragment)
     * in base alla modalità foldable o single-pane.
     * @param shouldBeFoldable True se la modalità a due pagine (foldable) deve essere attiva.
     */
    private fun updateLayout(shouldBeFoldable: Boolean) {
        // Rimuove tutti i frammenti esistenti prima di caricare i nuovi.
        // Questo è CRUCIALE per prevenire sovrapposizioni, IllegalStateException e memory leaks.
        // commitNowAllowStateLoss() esegue la transazione IMMEDIATAMENTE e permette la perdita di stato.
        supportFragmentManager.findFragmentByTag("PDF_SINGLE_TAG")?.let {
            supportFragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
            Log.d("PDFViewerActivity", "Rimosso PDF_SINGLE_TAG.")
        }
        supportFragmentManager.findFragmentByTag("PDF_LEFT_TAG")?.let {
            supportFragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
            Log.d("PDFViewerActivity", "Rimosso PDF_LEFT_TAG.")
        }
        supportFragmentManager.findFragmentByTag("PDF_RIGHT_TAG")?.let {
            supportFragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
            Log.d("PDFViewerActivity", "Rimosso PDF_RIGHT_TAG.")
        }
        Log.d("PDFViewerActivity", "Puliti i fragment prima di cambiare layout.")


        if (shouldBeFoldable) {
            Log.d("PDFViewerActivity", "Passo alla modalità foldable (due frammenti).")
            currentLayoutIsFoldable = true // Aggiorna lo stato della Activity a foldable

            // Imposta la visibilità e il peso dei contenitori dei fragment per la modalità a due pannelli.
            binding?.pdfFragmentLeftContainer?.let {
                val params = it.layoutParams as LinearLayout.LayoutParams
                params.weight = 1f // Occupa metà larghezza
                it.layoutParams = params
                it.visibility = View.VISIBLE // Assicurati che sia visibile
            }
            binding?.pdfFragmentRightContainer?.let {
                val params = it.layoutParams as LinearLayout.LayoutParams
                params.weight = 1f // Occupa metà larghezza
                it.layoutParams = params
                it.visibility = View.VISIBLE // Assicurati che sia visibile
            }
            // NUOVO: Richiede al LinearLayout padre di ricalcolare il layout
            binding?.pdfFragmentsContainerHorizontal?.requestLayout()


            binding?.navigationCard?.visibility = View.VISIBLE


            setupFoldablePdfFragments() // Carica i due fragment PDF nei rispettivi contenitori
        } else {
            Log.d("PDFViewerActivity", "Torno alla modalità single-pane (un singolo frammento).")
            currentLayoutIsFoldable = false // Aggiorna lo stato della Activity a single-pane

            // Imposta la visibilità e il peso del contenitore sinistro per occupare tutto lo schermo,
            // e nasconde il contenitore destro.
            binding?.pdfFragmentLeftContainer?.let {
                val params = it.layoutParams as LinearLayout.LayoutParams
                params.weight = 2f // Occupa tutta la larghezza disponibile (data weightSum=2)
                it.layoutParams = params
                it.visibility = View.VISIBLE // Assicurati che sia visibile
            }
            // NUOVO: Imposta esplicitamente il peso del contenitore destro a 0f quando è GONE
            binding?.pdfFragmentRightContainer?.let {
                val params = it.layoutParams as LinearLayout.LayoutParams
                params.weight = 0f // Imposta il peso a 0
                it.layoutParams = params
                it.visibility = View.GONE // Nascondi il pannello destro
            }
            // NUOVO: Richiede al LinearLayout padre di ricalcolare il layout
            binding?.pdfFragmentsContainerHorizontal?.requestLayout()


            binding?.navigationCard?.visibility = View.GONE


            setupSinglePdfFragment() // Carica il singolo fragment PDF nel contenitore sinistro
        }
        // NUOVO: Chiama updateNavigationButtonStates dopo ogni cambio di layout
        updateNavigationButtonStates()
        Log.d("PDFViewerActivity", "updateLayout chiamato con shouldBeFoldable: $shouldBeFoldable. Nuovo stato: $currentLayoutIsFoldable")
    }

    /**
     * Carica il singolo PdfViewerFragment nel contenitore di sinistra (che occuperà tutto lo schermo).
     * In questa modalità, lo scorrimento interno della PDFView è abilitato.
     */
    private fun setupSinglePdfFragment() {
        Log.d("PDFViewerActivity", "Caricamento Single-Pane PdfViewerFragment. Pagina iniziale: $currentPage. URI: $pdfUri")
        pdfUri?.let { uri ->
            // Creiamo una nuova istanza del fragment per pulizia e per impostare le opzioni corrette.
            // L'ultimo parametro 'pagesToLoad' è null per indicare che deve caricare l'intero documento scorrevole
            val singleFragment = PdfViewerFragment.newInstance(uri, currentPage, true, null)
            supportFragmentManager.beginTransaction()
                .replace(R.id.pdf_fragment_left_container, singleFragment, "PDF_SINGLE_TAG") // Sostituisce nel contenitore sinistro
                .commitNowAllowingStateLoss() // Esegue la transazione in modo asincrono, permettendo la perdita di stato

            // Aggiungi un click listener al contenitore del fragment per la navigazione (avanza di 1 pagina)
            binding?.pdfFragmentLeftContainer?.setOnClickListener {
                Log.d("PDFViewerActivity", "Cliccato pannello singolo. Navigo avanti di 1 pagina.")
                navigatePages(1)
            }
        } ?: Log.e("PDFViewerActivity", "URI PDF nullo durante setupSinglePdfFragment.")
    }

    /**
     * Carica i due PdfViewerFragment nei contenitori di sinistra e destra.
     * In questa modalità, lo scorrimento interno della PDFView è disabilitato per i fragment.
     */
    private fun setupFoldablePdfFragments() {
        Log.d("PDFViewerActivity", "Caricamento Foldable PdfViewerFragments.")

        // Assicurati che la pagina iniziale del pannello sinistro sia pari, per una visualizzazione a libro corretta
        if (currentPage % 2 != 0) {
            currentPage -= 1
            if (currentPage < 0) currentPage = 0 // Assicura che non sia negativo
        }
        val pageLeft = currentPage
        val pageRight = currentPage + 1 // La pagina destra sarà sempre quella successiva alla sinistra

        Log.d("PDFViewerActivity", "Configurazione visualizzazione Foldable. Pagina sinistra: $pageLeft, Pagina destra: $pageRight")

        pdfUri?.let { uri ->
            // Crea nuove istanze dei fragment, disabilitando lo swipe interno per entrambi
            // Passiamo intArrayOf(pagina) per il parametro pagesToLoad
            val leftFragment = PdfViewerFragment.newInstance(uri, 0, false, intArrayOf(pageLeft))
            val rightFragment = PdfViewerFragment.newInstance(uri, 0, false, intArrayOf(pageRight))

            // Sostituisce i fragment nei rispettivi contenitori
            supportFragmentManager.beginTransaction()
                .replace(R.id.pdf_fragment_left_container, leftFragment, "PDF_LEFT_TAG")
                .replace(R.id.pdf_fragment_right_container, rightFragment, "PDF_RIGHT_TAG")
                .commitNowAllowingStateLoss() // Esegue la transazione in modo asincrono, permettendo la perdita di stato

            Log.d("PDFViewerActivity", "Fragment PDF avviati per modalità foldable. Attendendo callback di caricamento.")

            // Gestisci il fragment destro: caricalo solo se pageRight è all'interno dei limiti del documento
            if (pageRight < totalPdfPages) {
                // Se c'è una pagina destra valida, caricala
                val rightFragment = PdfViewerFragment.newInstance(uri, 0, false, intArrayOf(pageRight))
                binding?.pdfFragmentRightContainer?.visibility = View.VISIBLE // Assicurati che sia visibile
                supportFragmentManager.beginTransaction()
                    .replace(R.id.pdf_fragment_right_container, rightFragment, "PDF_RIGHT_TAG")
                    .commitNowAllowingStateLoss()
                Log.d("PDFViewerActivity", "Caricato fragment destro per pagina: $pageRight")
            } else {
                // Se non c'è una pagina destra valida, nascondi il contenitore e rimuovi eventuali fragment.
                binding?.pdfFragmentRightContainer?.visibility = View.INVISIBLE // Rende il pannello vuoto mantenendo lo spazio
                supportFragmentManager.findFragmentByTag("PDF_RIGHT_TAG")?.let {
                    supportFragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
                }
                Log.d("PDFViewerActivity", "Nessuna pagina destra da visualizzare. Contenitore destro impostato a INVISIBLE.")
            }

            // Aggiungi click listener ai contenitori dei fragment per la navigazione in modalità foldable
            binding?.pdfFragmentLeftContainer?.setOnClickListener {
                Log.d("PDFViewerActivity", "Cliccato pannello sinistro. Navigo indietro (-2).")
                navigatePages(-2) // Naviga indietro di 2 pagine (coppia)
            }
            binding?.pdfFragmentRightContainer?.setOnClickListener {
                Log.d("PDFViewerActivity", "Cliccato pannello destro. Navigo avanti (+2).")
                navigatePages(2) // Naviga avanti di 2 pagine (coppia)
            }
        } ?: Log.e("PDFViewerActivity", "URI PDF nullo durante setupFoldablePdfFragments.")
    }

    /**
     * Naviga le pagine nel visualizzatore PDF.
     * @param offset L'offset di pagine da applicare (es. 1 per avanti in single-pane, 2 per avanti in foldable).
     */
    private fun navigatePages(offset: Int) {
        Log.d("PDFViewerActivity", "Chiamata navigatePages con offset: $offset (Current: $currentPage, Total: $totalPdfPages, Foldable: $currentLayoutIsFoldable)")
        if (totalPdfPages == 0) {
            Log.w("PDFViewerActivity", "Pagine totali non determinate per la navigazione. TotalPagine: $totalPdfPages")
            Toast.makeText(this, "PDF non ancora caricato o non disponibile per la navigazione.", Toast.LENGTH_SHORT).show()
            return
        }

        val isCurrentlyFoldable = currentLayoutIsFoldable
        var proposedNewLeftPage = currentPage + offset // Questa è la pagina che andrebbe a sinistra

        if (isCurrentlyFoldable) {
            // Calcola la pagina iniziale minima (sempre 0)
            val minPossibleLeftPage = 0

            // Calcola la pagina iniziale massima per il pannello sinistro.
            // Se totalPdfPages è dispari (es. 3), l'ultima pagina visualizzabile a sinistra è (totalPdfPages - 1) = 2.
            // Se totalPdfPages è pari (es. 4), l'ultima coppia valida inizia a (totalPdfPages - 2) = 2.
            val maxPossibleLeftPage = if (totalPdfPages % 2 != 0) {
                totalPdfPages - 1
            } else {
                totalPdfPages - 2
            }
                // Assicurati che maxPossibleLeftPage non sia negativo per documenti molto piccoli (0 o 1 pagina)
                .coerceAtLeast(0)


            // Gestione della navigazione "Indietro"
            if (offset < 0) {
                if (proposedNewLeftPage < minPossibleLeftPage) {
                    Toast.makeText(this, "Sei già alla prima coppia di pagine.", Toast.LENGTH_SHORT).show()
                    Log.d("PDFViewerActivity", "Tentato di navigare prima della prima pagina. Current: $currentPage, Proposed: $proposedNewLeftPage.")
                    return // Impedisce la navigazione
                }
                // Assicurati che la pagina sinistra sia pari quando si va indietro, a meno che non sia la pagina 0
                if (proposedNewLeftPage % 2 != 0) {
                    proposedNewLeftPage -= 1 // Arrotonda per difetto alla pagina pari precedente
                }
            }
            // Gestione della navigazione "Avanti"
            else if (offset > 0) {
                if (proposedNewLeftPage > maxPossibleLeftPage) {
                    Toast.makeText(this, "Sei già all'ultima pagina.", Toast.LENGTH_SHORT).show()
                    Log.d("PDFViewerActivity", "Tentato di navigare oltre l'ultima pagina. Current: $currentPage, Proposed: $proposedNewLeftPage, Max Possible Left: $maxPossibleLeftPage.")
                    return // Impedisce la navigazione
                }
                // Assicurati che la pagina sinistra sia pari, a meno che non sia l'ultima pagina dispari del documento
                if (proposedNewLeftPage % 2 != 0 && proposedNewLeftPage != totalPdfPages - 1) {
                    proposedNewLeftPage += 1 // Arrotonda per eccesso alla pagina pari successiva
                }
            }


            // Se la pagina effettiva visualizzata (pannello sinistro) non cambia, non fare nulla
            if (proposedNewLeftPage == currentPage) {
                Log.d("PDFViewerActivity", "Tentato di navigare alla stessa pagina effettiva ($proposedNewLeftPage). Nessuna azione.")
                return
            }

            currentPage = proposedNewLeftPage
            setupFoldablePdfFragments() // Ricarica i frammenti con la nuova `currentPage`
            Log.d("PDFViewerActivity", "Navigazione foldable a pagine: Sinistra=${currentPage}, Destra=${currentPage + 1}")

        } else { // Modalità single-pane (nessun cambiamento qui, già funzionante)
            var newPage = currentPage + offset

            if (newPage < 0) {
                newPage = 0
                Toast.makeText(this, "Sei già alla prima pagina.", Toast.LENGTH_SHORT).show()
            } else if (newPage >= totalPdfPages) {
                newPage = totalPdfPages - 1
                Toast.makeText(this, "Sei già all'ultima pagina.", Toast.LENGTH_SHORT).show()
            }

            if (newPage == currentPage) {
                return
            }

            currentPage = newPage
            val singleFragment = supportFragmentManager.findFragmentByTag("PDF_SINGLE_TAG") as? PdfViewerFragment
            singleFragment?.setPage(newPage)
            Log.d("PDFViewerActivity", "Navigazione singola a pagina: $newPage")
        }
        updateNavigationButtonStates()
    }
    /**
     * Aggiorna lo stato di abilitazione/disabilitazione dei pulsanti di navigazione.
     * I pulsanti sono attivi solo in modalità foldable.
     */
    private fun updateNavigationButtonStates() {
        // I pulsanti sono presenti solo nella navigationCard.
        // Se non siamo in modalità foldable, i pulsanti devono essere disabilitati/nascosti dalla logica updateLayout.
        // Qui aggiorniamo solo lo stato enabled/disabled se la card è visibile.
        if (currentLayoutIsFoldable) {
            binding?.btnPrev?.isEnabled = currentPage > 0
            val nextPairStartPage = currentPage + 2
            // Abilita "Avanti" solo se c'è almeno una pagina successiva alla coppia corrente
            binding?.btnNext?.isEnabled = nextPairStartPage <= totalPdfPages
            // Se totalPages è dispari e currentPage è l'ultima pagina (es. 4 pagine, currentPage=3), non ci sono altre pagine.
            if (totalPdfPages > 0 && currentPage == totalPdfPages -1 && totalPdfPages % 2 != 0) {
                binding?.btnNext?.isEnabled = false
            }

        } else {
            // In modalità single-pane, i pulsanti dovrebbero essere già nascosti.
            // Li disabilitiamo solo per sicurezza logica, ma non avranno effetto visibile.
            binding?.btnPrev?.isEnabled = false
            binding?.btnNext?.isEnabled = false
        }
        Log.d("PDFViewerActivity", "Bottoni navigazione aggiornati: Indietro=${binding?.btnPrev?.isEnabled}, Avanti=${binding?.btnNext?.isEnabled} (Current: $currentPage, Total: $totalPdfPages, Foldable: $currentLayoutIsFoldable)")
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null // Rilascia il riferimento al binding per prevenire memory leaks
    }
}