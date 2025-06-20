package it.lavorodigruppo.flexipdf.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnErrorListener
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.interfaces.PdfLoadCallback

/**
 * `PdfViewerFragment` è un Fragment che incapsula la visualizzazione di un documento PDF
 * utilizzando la libreria `AndroidPdfViewer` di Barteksc.
 * Questo fragment è configurabile per visualizzare un intero PDF o pagine specifiche,
 * supporta lo scorrimento interno e gestisce gli errori di caricamento e il completamento del caricamento.
 * Implementa `OnErrorListener` e `OnLoadCompleteListener` per interagire con la libreria PDF.
 */
class PdfViewerFragment : Fragment(), OnErrorListener, OnLoadCompleteListener {

    private var pdfUri: Uri? = null
    var initialPage: Int = 0
    private var enableInternalSwipe: Boolean = true
    private var pagesToLoad: IntArray? = null

    private var pdfView: PDFView? = null
    private var pdfLoadCallback: PdfLoadCallback? = null

    companion object {
        private const val ARG_PDF_URI = "pdf_uri"
        private const val ARG_INITIAL_PAGE = "initial_page"
        private const val ARG_ENABLE_INTERNAL_SWIPE = "enable_internal_swipe"
        private const val ARG_PAGES_TO_LOAD = "pages_to_load"

        /**
         * Crea una nuova istanza di `PdfViewerFragment` con i parametri specificati.
         * Questo è il metodo raccomandato per istanziare il fragment, passando gli argomenti
         * tramite un `Bundle` per la ricostruzione sicura del fragment.
         *
         * @param pdfUri L'URI del documento PDF da visualizzare.
         * @param initialPage L'indice della pagina da cui iniziare la visualizzazione (default 0).
         * @param enableInternalSwipe Indica se lo scorrimento interno (swipe) del PDF è abilitato (default true).
         * @param pagesToLoad Un array opzionale di indici di pagine da caricare. Se fornito, verranno visualizzate
         * solo queste pagine e lo swipe interno sarà disabilitato. (default null)
         * @return Una nuova istanza di `PdfViewerFragment`.
         */
        @JvmStatic
        fun newInstance(
            pdfUri: Uri,
            initialPage: Int = 0,
            enableInternalSwipe: Boolean = true,
            pagesToLoad: IntArray? = null
        ): PdfViewerFragment {
            return PdfViewerFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PDF_URI, pdfUri)
                    putInt(ARG_INITIAL_PAGE, initialPage)
                    putBoolean(ARG_ENABLE_INTERNAL_SWIPE, enableInternalSwipe)
                    putIntArray(ARG_PAGES_TO_LOAD, pagesToLoad)
                }
            }
        }
    }

    /**
     * Chiamato quando il Fragment viene attaccato al suo contesto (Activity).
     * In questo metodo, si verifica se il contesto implementa l'interfaccia `PdfLoadCallback`
     * e, in caso affermativo, si memorizza un riferimento per la comunicazione.
     *
     * @param context Il contesto (Activity) a cui il Fragment è associato.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PdfLoadCallback) {
            pdfLoadCallback = context
        } else {
            Log.e("PdfViewerFragment", "$context deve implementare PdfLoadCallback")
        }
    }

    /**
     * Chiamato quando il Fragment viene creato.
     * Recupera gli argomenti passati al Fragment (URI del PDF, pagina iniziale, abilitazione swipe, pagine specifiche)
     * dal `Bundle`. Se gli argomenti essenziali sono mancanti, viene loggato un errore e mostrato un Toast.
     *
     * @param savedInstanceState Se non nullo, questo Fragment viene ricostruito da uno stato precedentemente salvato.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pdfUri = it.getParcelable(ARG_PDF_URI)
            initialPage = it.getInt(ARG_INITIAL_PAGE, 0)
            enableInternalSwipe = it.getBoolean(ARG_ENABLE_INTERNAL_SWIPE, true)
            pagesToLoad = it.getIntArray(ARG_PAGES_TO_LOAD)
        } ?: run {
            Log.e("PdfViewerFragment", "Argomenti mancanti per PdfViewerFragment.")
            Toast.makeText(context, "Error: Pdf data missing", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Chiamato per creare e restituire la gerarchia di viste associata al Fragment.
     * Gonfia il layout `fragment_pdf_viewer.xml` e lo restituisce.
     *
     * @param inflater L'oggetto `LayoutInflater` che può essere usato per gonfiare qualsiasi vista nel contesto corrente.
     * @param container Se non nullo, questo è il `ViewGroup` padre a cui la UI del Fragment dovrebbe essere allegata.
     * @param savedInstanceState Se non nullo, questo Fragment viene ricostruito da uno stato precedentemente salvato.
     * @return La vista radice (View) del Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pdf_viewer, container, false)
    }

    /**
     * Chiamato subito dopo che `onCreateView` ha restituito la sua vista.
     * In questo metodo, si inizializza l'oggetto `PDFView` presente nel layout e si configura
     * il caricamento del PDF utilizzando l'URI e i parametri forniti.
     * Gestisce la logica per caricare l'intero documento o solo pagine specifiche.
     *
     * @param view La vista radice del Fragment restituita da `onCreateView`.
     * @param savedInstanceState Se non nullo, questo Fragment viene ricostruito da uno stato precedentemente salvato.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pdfView = view.findViewById(R.id.pdfViewFragment)

        pdfUri?.let { uri ->
            Log.d("PdfViewerFragment", "Tentativo di caricare PDF da URI: $uri, pagina: $initialPage, swipe interno: $enableInternalSwipe, pagesToLoad=${pagesToLoad?.joinToString()}")
            try {
                val configurator = pdfView?.fromUri(uri)
                    ?.defaultPage(initialPage)
                    ?.scrollHandle(DefaultScrollHandle(requireContext()))
                    ?.enableDoubletap(true)
                    ?.onError(this)
                    ?.onLoad(this)

                pagesToLoad?.let { pagesArray ->
                    if (pagesArray.isNotEmpty()) {
                        Log.d("PdfViewerFragment", "Caricando pagine specifiche con .pages(): ${pagesArray.joinToString()}")
                        configurator?.pages(*pagesArray)
                        configurator?.enableSwipe(false)
                        configurator?.swipeHorizontal(false)
                    } else {
                        Log.d("PdfViewerFragment", "Caricamento completo con swipe interno: $enableInternalSwipe")
                        configurator?.enableSwipe(enableInternalSwipe)
                        configurator?.swipeHorizontal(false)
                    }
                } ?: run {
                    Log.d("PdfViewerFragment", "Caricamento completo con swipe interno: $enableInternalSwipe (pagesToLoad è null)")
                    configurator?.enableSwipe(enableInternalSwipe)
                    configurator?.swipeHorizontal(false)
                }

                configurator?.load()
            } catch (e: Exception) {
                Log.e("PdfViewerFragment", "Errore durante il caricamento del PDF: ${e.message}", e)
                Toast.makeText(context, "Error in PDF's loading: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } ?: Log.e("PdfViewerFragment", "Errore: URI PDF nullo nel Fragment durante onViewCreated.")
    }

    /**
     * Imposta la pagina corrente del visualizzatore PDF.
     * Se il fragment è stato configurato per visualizzare pagine specifiche (tramite `pagesToLoad`),
     * la chiamata a questo metodo ricaricherà il visualizzatore mostrando solo la pagina desiderata.
     * Altrimenti, se si tratta di un PDF completo, salterà direttamente alla pagina specificata.
     *
     * @param pageNumber L'indice della pagina a cui saltare (0-based).
     */
    fun setPage(pageNumber: Int) {
        if (pdfView != null) {
            if (pagesToLoad != null && pagesToLoad!!.isNotEmpty()) {
                pdfUri?.let { uri ->
                    Log.d("PdfViewerFragment", "setPage: Ricaricando pagina specifica: $pageNumber")
                    pdfView?.fromUri(uri)
                        ?.pages(pageNumber)
                        ?.defaultPage(0)
                        ?.scrollHandle(null)
                        ?.enableSwipe(false)
                        ?.enableDoubletap(true)
                        ?.onError(this)
                        ?.onLoad(this)
                        ?.load()
                    this.initialPage = pageNumber
                }
            } else if (pageNumber != pdfView!!.currentPage) {
                pdfView?.jumpTo(pageNumber)
                this.initialPage = pageNumber
                Log.d("PdfViewerFragment", "Jumped to page: $pageNumber (scorrevole)")
            } else {
                Log.w("PdfViewerFragment", "Impossibile impostare la pagina: stessa pagina ($pageNumber).")
            }
        } else {
            Log.w("PdfViewerFragment", "Impossibile impostare la pagina: pdfView non inizializzata.")
        }
    }

    /**
     * Callback chiamato dalla libreria `AndroidPdfViewer` quando si verifica un errore durante il caricamento o la visualizzazione del PDF.
     * Logga l'errore e mostra un `Toast` all'utente con il messaggio dell'errore.
     * @param e L'oggetto `Throwable` che rappresenta l'errore.
     */
    override fun onError(e: Throwable?) {
        Log.e("PdfViewerFragment", "Errore dalla libreria PDF: ${e?.message}", e)
        Toast.makeText(context, "Error during PDF's loading: ${e?.localizedMessage}", Toast.LENGTH_LONG).show()
    }

    /**
     * Callback chiamato dalla libreria `AndroidPdfViewer` quando il caricamento del PDF è completato con successo.
     * Logga il numero totale di pagine caricate e notifica l'Activity tramite il `pdfLoadCallback`.
     * @param nbPages Il numero totale di pagine nel documento PDF caricato.
     */
    override fun loadComplete(nbPages: Int) {
        Log.d("PdfViewerFragment", "PDF caricato nel fragment. Pagine totali: $nbPages")
        pdfLoadCallback?.onPdfFragmentLoaded(nbPages)
    }

    /**
     * Chiamato quando la vista del Fragment sta per essere distrutta.
     * Esegue la pulizia delle risorse: imposta `pdfView` e `pdfLoadCallback` a `null`
     * per prevenire memory leak.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        pdfView = null
        pdfLoadCallback = null
    }
}