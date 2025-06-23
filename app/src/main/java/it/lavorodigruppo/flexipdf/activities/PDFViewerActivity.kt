/**
 * @file PDFViewerActivity.kt
 *
 * @brief Activity dedicata alla visualizzazione di documenti PDF.
 *
 * @overview
 * Questa Activity è responsabile di caricare e mostrare un file PDF, il cui URI (Uniform Resource Identifier)
 * viene passato tramite un Intent dalla Activity chiamante. Utilizza un approccio basato su Fragment
 * (`PdfViewerFragment`) per il rendering dei PDF, supportando diverse configurazioni di layout:
 * visualizzazione a pagina singola (per smartphone o tablet in orientamento portrait)
 * e visualizzazione a doppia pagina (per dispositivi pieghevoli in modalità "tabletop" o "book mode").
 * Gestisce i permessi per l'accesso ai file, la navigazione tra le pagine e l'adattamento dell'interfaccia
 * in base alle posture dei dispositivi pieghevoli tramite Jetpack WindowManager.
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
import it.lavorodigruppo.flexipdf.provider.FlexiPDFWidgetProvider


class PDFViewerActivity : AppCompatActivity(), PdfLoadCallback {

    private var totalPdfPages: Int = 0
    private var pdfUri: Uri? = null
    private var currentPage: Int = 0
    private var currentLayoutIsFoldable: Boolean = true

    private var binding: ActivityPdfViewerFoldableBinding? = null

    /**
     * Metodo chiamato alla creazione dell'Activity.
     * Inizializza il ViewBinding, recupera l'URI del PDF dall'Intent, gestisce i permessi di lettura del file,
     * ottiene il conteggio totale delle pagine del PDF e ripristina lo stato se l'Activity viene ricreata.
     * Configura i listener per i pulsanti di navigazione e avvia l'osservazione dei cambiamenti di layout
     * per i dispositivi pieghevoli tramite `WindowInfoTracker`.
     * @param savedInstanceState L'oggetto Bundle contenente lo stato precedentemente salvato dell'Activity, se presente.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPdfViewerFoldableBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        this.pdfUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("pdf_uri", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("pdf_uri")
        } ?: run {
            Log.e("PDFViewerActivity", "Nessun PDF URI fornito.")
            Toast.makeText(this, "No PDF specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        pdfUri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d("PDFViewerActivity", "Permesso URI persistente concesso per: $it")
            } catch (e: SecurityException) {
                Log.e("PDFViewerActivity", "Impossibile ottenere permesso URI persistente: ${e.message}. Tentativo di permesso temporaneo.")
                try {
                    grantUriPermission(packageName, it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    Log.d("PDFViewerActivity", "Permesso URI temporaneo concesso per: $it")
                } catch (e2: Exception) {
                    Log.e("PDFViewerActivity", "Impossibile ottenere permesso URI temporaneo: ${e2.message}", e2)
                    Toast.makeText(this, "Error with file permissions: ${e2.message}", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
            } catch (e: Exception) {
                Log.e("PDFViewerActivity", "Errore generico nell'ottenere permessi URI: ${e.message}", e)
                Toast.makeText(this, "Error with file permissions: ${e.message}", Toast.LENGTH_LONG).show()

                finish()
                return
            }
        }

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

        pdfUri?.let { uri ->
            totalPdfPages = getAbsolutePdfPageCount(uri)
            Log.d("PDFViewerActivity", "Conteggio pagine PDF totale (da PdfRenderer): $totalPdfPages")
        } ?: run {
            Log.e("PDFViewerActivity", "Impossibile ottenere il conteggio totale delle pagine: URI PDF nullo.")
            totalPdfPages = 0
        }

        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt("currentPage", 0)
            totalPdfPages = savedInstanceState.getInt("totalPdfPages", 0)
            currentLayoutIsFoldable = savedInstanceState.getBoolean("currentLayoutIsFoldable", false)
            Log.d("PDFViewerActivity", "Stato ripristinato: Pagina=${currentPage}, Totale=${totalPdfPages}, LayoutFoldable=${currentLayoutIsFoldable}")
        } else {
            Log.d("PDFViewerActivity", "Nessuno stato salvato. Avvio pagina: 0.")
        }

        binding?.btnPrev?.setOnClickListener {
            Log.d("PDFViewerActivity", "Pulsante 'Indietro' cliccato. Chiamo navigatePages(-2).")
            navigatePages(-2)
        }
        binding?.btnNext?.setOnClickListener {
            Log.d("PDFViewerActivity", "Pulsante 'Avanti' cliccato. Chiamo navigatePages(2).")
            navigatePages(2)
        }

        lifecycleScope.launch {
            WindowInfoTracker.getOrCreate(this@PDFViewerActivity)
                .windowLayoutInfo(this@PDFViewerActivity)
                .collect { newLayoutInfo ->
                    val foldingFeature = newLayoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()

                    val isInFoldableSemiOpenPortraitMode = foldingFeature != null &&
                            foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                            foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL

                    Log.d("PDFViewerActivity", "WindowLayoutInfo aggiornato. IsFoldableSemiOpenPortraitMode = $isInFoldableSemiOpenPortraitMode, Current Orientation = ${resources.configuration.orientation}")

                    val shouldBeFoldable = isInFoldableSemiOpenPortraitMode && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

                    updateLayout(shouldBeFoldable)
                }
        }

        if (savedInstanceState == null) {
            updateLayout(false)
        } else {
            updateLayout(currentLayoutIsFoldable)
            FlexiPDFWidgetProvider.notifyFileStatusChanged(applicationContext, "currentFileName.pdf")
        }
        updateNavigationButtonStates()
    }

    /**
     * Salva lo stato corrente dell'Activity prima che venga distrutta a causa di un cambiamento di configurazione
     * (es. rotazione dello schermo) o per altre ragioni di sistema.
     * Salva la pagina corrente, il numero totale di pagine e lo stato del layout (foldable/single-pane).
     * @param outState L'oggetto Bundle in cui salvare lo stato dell'Activity.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentPage", currentPage)
        outState.putInt("totalPdfPages", totalPdfPages)
        outState.putBoolean("currentLayoutIsFoldable", currentLayoutIsFoldable)
        Log.d("PDFViewerActivity", "Stato salvato: Pagina=${currentPage}, Totale=${totalPdfPages}, LayoutFoldable=${currentLayoutIsFoldable}")
    }

    /**
     * Callback chiamato quando il `PdfViewerFragment` ha terminato di caricare il PDF.
     * Questo metodo viene usato per aggiornare lo stato di abilitazione dei pulsanti di navigazione
     * una volta che il documento è pronto per la visualizzazione.
     * @param nbPagesFromFragment Il numero di pagine che il fragment ha caricato (non sempre il totale del PDF).
     */
    override fun onPdfFragmentLoaded(nbPagesFromFragment: Int) {
        updateNavigationButtonStates()
    }

    /**
     * Funzione centrale per aggiornare il layout dell'Activity (caricando o nascondendo i Fragment visualizzatori di PDF)
     * in base alla modalità di visualizzazione desiderata (a pagina singola o a due pagine per i foldable).
     * Rimuove prima i frammenti esistenti per prevenire sovrapposizioni e memory leaks.
     * @param shouldBeFoldable `true` se la modalità a due pagine (foldable) deve essere attivata, `false` per la modalità a pagina singola.
     */
    private fun updateLayout(shouldBeFoldable: Boolean) {
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
            currentLayoutIsFoldable = true

            binding?.pdfFragmentLeftContainer?.let {
                val params = it.layoutParams as LinearLayout.LayoutParams
                params.weight = 1f
                it.layoutParams = params
                it.visibility = View.VISIBLE
            }
            binding?.pdfFragmentRightContainer?.let {
                val params = it.layoutParams as LinearLayout.LayoutParams
                params.weight = 1f
                it.layoutParams = params
                it.visibility = View.VISIBLE
            }
            binding?.pdfFragmentsContainerHorizontal?.requestLayout()


            binding?.navigationCard?.visibility = View.VISIBLE


            setupFoldablePdfFragments()
        } else {
            Log.d("PDFViewerActivity", "Torno alla modalità single-pane (un singolo frammento).")
            currentLayoutIsFoldable = false

            binding?.pdfFragmentLeftContainer?.let {
                val params = it.layoutParams as LinearLayout.LayoutParams
                params.weight = 2f
                it.layoutParams = params
                it.visibility = View.VISIBLE
            }
            binding?.pdfFragmentRightContainer?.let {
                val params = it.layoutParams as LinearLayout.LayoutParams
                params.weight = 0f
                it.layoutParams = params
                it.visibility = View.GONE
            }
            binding?.pdfFragmentsContainerHorizontal?.requestLayout()


            binding?.navigationCard?.visibility = View.GONE


            setupSinglePdfFragment()
        }
        updateNavigationButtonStates()
        Log.d("PDFViewerActivity", "updateLayout chiamato con shouldBeFoldable: $shouldBeFoldable. Nuovo stato: $currentLayoutIsFoldable")
    }

    /**
     * Configura il visualizzatore PDF per la modalità a pagina singola.
     * Carica un singolo `PdfViewerFragment` nel contenitore di sinistra, il quale occuperà tutta la larghezza disponibile.
     * In questa modalità, lo scorrimento interno del PDF è abilitato all'interno del fragment.
     */
    private fun setupSinglePdfFragment() {
        Log.d("PDFViewerActivity", "Caricamento Single-Pane PdfViewerFragment. Pagina iniziale: $currentPage. URI: $pdfUri")
        pdfUri?.let { uri ->
            val singleFragment = PdfViewerFragment.newInstance(uri, currentPage, true, null)
            supportFragmentManager.beginTransaction()
                .replace(R.id.pdf_fragment_left_container, singleFragment, "PDF_SINGLE_TAG")
                .commitNowAllowingStateLoss()

            binding?.pdfFragmentLeftContainer?.setOnClickListener {
                Log.d("PDFViewerActivity", "Cliccato pannello singolo. Navigo avanti di 1 pagina.")
                navigatePages(1)
            }
        } ?: Log.e("PDFViewerActivity", "URI PDF nullo durante setupSinglePdfFragment.")
    }

    /**
     * Configura il visualizzatore PDF per la modalità a due pagine (foldable).
     * Carica due `PdfViewerFragment`: uno nel contenitore di sinistra e uno in quello di destra.
     * Assicura che la pagina iniziale del pannello sinistro sia un numero pari per una corretta visualizzazione a "libro".
     * Il fragment destro viene caricato solo se esiste una pagina successiva valida nel documento.
     * In questa modalità, lo scorrimento interno del PDF è disabilitato per entrambi i fragment.
     */
    private fun setupFoldablePdfFragments() {
        Log.d("PDFViewerActivity", "Caricamento Foldable PdfViewerFragments.")

        if (currentPage % 2 != 0) {
            currentPage -= 1
            if (currentPage < 0) currentPage = 0
        }
        val pageLeft = currentPage
        val pageRight = currentPage + 1

        Log.d("PDFViewerActivity", "Configurazione visualizzazione Foldable. Pagina sinistra: $pageLeft, Pagina destra: $pageRight")

        pdfUri?.let { uri ->
            val leftFragment = PdfViewerFragment.newInstance(uri, 0, false, intArrayOf(pageLeft))

            supportFragmentManager.beginTransaction()
                .replace(R.id.pdf_fragment_left_container, leftFragment, "PDF_LEFT_TAG")
                .commitNowAllowingStateLoss()

            if (pageRight < totalPdfPages) {
                val rightFragment = PdfViewerFragment.newInstance(uri, 0, false, intArrayOf(pageRight))
                binding?.pdfFragmentRightContainer?.visibility = View.VISIBLE
                supportFragmentManager.beginTransaction()
                    .replace(R.id.pdf_fragment_right_container, rightFragment, "PDF_RIGHT_TAG")
                    .commitNowAllowingStateLoss()
                Log.d("PDFViewerActivity", "Caricato fragment destro per pagina: $pageRight")
            } else {
                binding?.pdfFragmentRightContainer?.visibility = View.INVISIBLE
                supportFragmentManager.findFragmentByTag("PDF_RIGHT_TAG")?.let {
                    supportFragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
                }
                Log.d("PDFViewerActivity", "Nessuna pagina destra da visualizzare. Contenitore destro impostato a INVISIBLE.")
            }

            binding?.pdfFragmentLeftContainer?.setOnClickListener {
                Log.d("PDFViewerActivity", "Cliccato pannello sinistro. Navigo indietro (-2).")
                navigatePages(-2)
            }
            binding?.pdfFragmentRightContainer?.setOnClickListener {
                Log.d("PDFViewerActivity", "Cliccato pannello destro. Navigo avanti (+2).")
                navigatePages(2)
            }
        } ?: Log.e("PDFViewerActivity", "URI PDF nullo durante setupFoldablePdfFragments.")
    }

    /**
     * Naviga le pagine del documento PDF, aggiornando la `currentPage` e ricaricando i fragment di visualizzazione.
     * La logica di navigazione si adatta alla modalità corrente (single-pane o foldable a due pagine).
     * Gestisce i limiti del documento (prima e ultima pagina) e fornisce feedback all'utente tramite Toast.
     * @param offset L'offset di pagine da applicare. Ad esempio, `1` per avanzare di una pagina in modalità singola,
     * `2` per avanzare di una coppia di pagine in modalità foldable, `-2` per tornare indietro di una coppia.
     */
    private fun navigatePages(offset: Int) {
        Log.d("PDFViewerActivity", "Chiamata navigatePages con offset: $offset (Current: $currentPage, Total: $totalPdfPages, Foldable: $currentLayoutIsFoldable)")
        if (totalPdfPages == 0) {
            Log.w("PDFViewerActivity", "Pagine totali non determinate per la navigazione. TotalPagine: $totalPdfPages")
            Toast.makeText(this, "PDF non ancora caricato o non disponibile per la navigazione.", Toast.LENGTH_SHORT).show()
            return
        }

        val isCurrentlyFoldable = currentLayoutIsFoldable
        var proposedNewLeftPage = currentPage + offset

        if (isCurrentlyFoldable) {
            val minPossibleLeftPage = 0
            val maxPossibleLeftPage = if (totalPdfPages % 2 != 0) {
                totalPdfPages - 1
            } else {
                totalPdfPages - 2
            }.coerceAtLeast(0)

            if (offset < 0) {
                if (proposedNewLeftPage < minPossibleLeftPage) {
                    Log.d("PDFViewerActivity", "Tentato di navigare prima della prima pagina. Current: $currentPage, Proposed: $proposedNewLeftPage.")
                    return
                }
                if (proposedNewLeftPage % 2 != 0) {
                    proposedNewLeftPage -= 1
                }
            } else if (offset > 0) {
                if (proposedNewLeftPage > maxPossibleLeftPage) {
                    Log.d("PDFViewerActivity", "Tentato di navigare oltre l'ultima pagina. Current: $currentPage, Proposed: $proposedNewLeftPage, Max Possible Left: $maxPossibleLeftPage.")
                    return
                }
                if (proposedNewLeftPage % 2 != 0 && proposedNewLeftPage != totalPdfPages - 1) {
                    proposedNewLeftPage += 1
                }
            }

            if (proposedNewLeftPage == currentPage) {
                Log.d("PDFViewerActivity", "Tentato di navigare alla stessa pagina effettiva ($proposedNewLeftPage). Nessuna azione.")
                return
            }

            currentPage = proposedNewLeftPage
            setupFoldablePdfFragments()
            Log.d("PDFViewerActivity", "Navigazione foldable a pagine: Sinistra=${currentPage}, Destra=${currentPage + 1}")

        } else {
            var newPage = currentPage + offset

            if (newPage < 0) {
                newPage = 0
            } else if (newPage >= totalPdfPages) {
                newPage = totalPdfPages - 1
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
     * Aggiorna lo stato di abilitazione/disabilitazione dei pulsanti di navigazione "Indietro" e "Avanti".
     * I pulsanti sono abilitati/disabilitati in base alla pagina corrente e al numero totale di pagine,
     * tenendo conto della modalità di visualizzazione (single-pane o foldable).
     * In modalità single-pane, i pulsanti sono sempre disabilitati/nascosti.
     */
    private fun updateNavigationButtonStates() {
        if (currentLayoutIsFoldable) {
            binding?.btnPrev?.isEnabled = currentPage > 0
            val nextPairStartPage = currentPage + 2
            binding?.btnNext?.isEnabled = nextPairStartPage <= totalPdfPages
            if (totalPdfPages > 0 && currentPage == totalPdfPages -1 && totalPdfPages % 2 != 0) {
                binding?.btnNext?.isEnabled = false
            }

        } else {
            binding?.btnPrev?.isEnabled = false
            binding?.btnNext?.isEnabled = false
        }
        Log.d("PDFViewerActivity", "Bottoni navigazione aggiornati: Indietro=${binding?.btnPrev?.isEnabled}, Avanti=${binding?.btnNext?.isEnabled} (Current: $currentPage, Total: $totalPdfPages, Foldable: $currentLayoutIsFoldable)")
    }

    /**
     * Metodo chiamato quando l'Activity sta per essere distrutta.
     * Rilascia il riferimento al ViewBinding per prevenire memory leaks.
     */
    override fun onDestroy() {
        super.onDestroy()
        binding = null // Rilascia il riferimento al binding per prevenire memory leaks
        FlexiPDFWidgetProvider.notifyFileStatusChanged(applicationContext, null)
    }
}