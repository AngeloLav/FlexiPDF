// app/src/main/java/it.lavorodigruppo.flexipdf.fragments/PdfViewerFragment.kt
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

class PdfViewerFragment : Fragment(), OnErrorListener, OnLoadCompleteListener {

    private var pdfUri: Uri? = null
    var initialPage: Int = 0
    private var enableInternalSwipe: Boolean = true
    private var pagesToLoad: IntArray? = null // <--- AGGIUNTA QUI: Parametro per pages() --->

    private var pdfView: PDFView? = null
    private var pdfLoadCallback: PdfLoadCallback? = null

    companion object {
        private const val ARG_PDF_URI = "pdf_uri"
        private const val ARG_INITIAL_PAGE = "initial_page"
        private const val ARG_ENABLE_INTERNAL_SWIPE = "enable_internal_swipe"
        private const val ARG_PAGES_TO_LOAD = "pages_to_load" // <--- AGGIUNTA QUI: Chiave argomento --->

        @JvmStatic
        fun newInstance(
            pdfUri: Uri,
            initialPage: Int = 0,
            enableInternalSwipe: Boolean = true,
            pagesToLoad: IntArray? = null // <--- AGGIUNTA QUI: Nuovo parametro con default null --->
        ): PdfViewerFragment {
            return PdfViewerFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PDF_URI, pdfUri)
                    putInt(ARG_INITIAL_PAGE, initialPage)
                    putBoolean(ARG_ENABLE_INTERNAL_SWIPE, enableInternalSwipe)
                    putIntArray(ARG_PAGES_TO_LOAD, pagesToLoad) // <--- AGGIUNTA QUI: Passa il valore
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PdfLoadCallback) {
            pdfLoadCallback = context
        } else {
            Log.e("PdfViewerFragment", "$context deve implementare PdfLoadCallback")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pdfUri = it.getParcelable(ARG_PDF_URI)
            initialPage = it.getInt(ARG_INITIAL_PAGE, 0)
            enableInternalSwipe = it.getBoolean(ARG_ENABLE_INTERNAL_SWIPE, true)
            pagesToLoad = it.getIntArray(ARG_PAGES_TO_LOAD) // <--- AGGIUNTA QUI: Recupera il valore
        } ?: run {
            Log.e("PdfViewerFragment", "Argomenti mancanti per PdfViewerFragment.")
            Toast.makeText(context, "Errore: dati PDF mancanti.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pdf_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pdfView = view.findViewById(R.id.pdfViewFragment) // Mantenuto come richiesto

        pdfUri?.let { uri ->
            Log.d("PdfViewerFragment", "Tentativo di caricare PDF da URI: $uri, pagina: $initialPage, swipe interno: $enableInternalSwipe, pagesToLoad=${pagesToLoad?.joinToString()}")
            try {
                val configurator = pdfView?.fromUri(uri)
                    ?.defaultPage(initialPage)
                    ?.scrollHandle(DefaultScrollHandle(requireContext()))
                    ?.enableDoubletap(true)
                    ?.onError(this)
                    ?.onLoad(this)

                // <--- LOGICA PER .pages() AGGIUNTA QUI --->
                pagesToLoad?.let { pagesArray ->
                    if (pagesArray.isNotEmpty()) {
                        Log.d("PdfViewerFragment", "Caricando pagine specifiche con .pages(): ${pagesArray.joinToString()}")
                        configurator?.pages(*pagesArray) // Usa il metodo .pages()
                        configurator?.enableSwipe(false) // Disabilita swipe per pagine specifiche
                        configurator?.swipeHorizontal(false) // Forzalo
                        // Nota: Barteksc non ha FitPolicy.BOTH o FitToWidth(pageIndex)
                        // Quindi, il fit dovrà essere gestito a livello di layout o activity.
                    } else {
                        // Comportamento esistente per caricamento completo
                        Log.d("PdfViewerFragment", "Caricamento completo con swipe interno: $enableInternalSwipe")
                        configurator?.enableSwipe(enableInternalSwipe)
                        configurator?.swipeHorizontal(false) // Verticale di default
                    }
                } ?: run {
                    // pagesToLoad è nullo, comportamento esistente per caricamento completo
                    Log.d("PdfViewerFragment", "Caricamento completo con swipe interno: $enableInternalSwipe (pagesToLoad è null)")
                    configurator?.enableSwipe(enableInternalSwipe)
                    configurator?.swipeHorizontal(false) // Verticale di default
                }
                // <--- FINE LOGICA PER .pages() AGGIUNTA --->

                configurator?.load()
            } catch (e: Exception) {
                Log.e("PdfViewerFragment", "Errore durante il caricamento del PDF: ${e.message}", e)
                Toast.makeText(context, "Errore nel caricamento del PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } ?: Log.e("PdfViewerFragment", "Errore: URI PDF nullo nel Fragment durante onViewCreated.")
    }

    fun setPage(pageNumber: Int) {
        if (pdfView != null) {
            // <--- MODIFICA QUI PER setPage e pagesToLoad --->
            if (pagesToLoad != null && pagesToLoad!!.isNotEmpty()) {
                // Se stiamo visualizzando pagine specifiche, ricarica solo quella pagina.
                // Barteksc fromUri().pages() ricarica tutto il viewer.
                pdfUri?.let { uri ->
                    Log.d("PdfViewerFragment", "setPage: Ricaricando pagina specifica: $pageNumber")
                    pdfView?.fromUri(uri)
                        ?.pages(pageNumber) // Ricarica solo la nuova pagina desiderata
                        ?.defaultPage(0) // DefaultPage è 0 per un array di una sola pagina
                        ?.scrollHandle(null) // Nessun scroll handle
                        ?.enableSwipe(false) // Swipe disabilitato
                        ?.enableDoubletap(true)
                        ?.onError(this)
                        ?.onLoad(this)
                        ?.load()
                    this.initialPage = pageNumber
                }
            } else if (pageNumber != pdfView!!.currentPage) {
                // Comportamento standard per scorrimento continuo
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

    override fun onError(e: Throwable?) {
        Log.e("PdfViewerFragment", "Errore dalla libreria PDF: ${e?.message}", e)
        Toast.makeText(context, "Errore durante il caricamento del PDF: ${e?.localizedMessage}", Toast.LENGTH_LONG).show()
    }

    override fun loadComplete(nbPages: Int) {
        Log.d("PdfViewerFragment", "PDF caricato nel fragment. Pagine totali: $nbPages")
        pdfLoadCallback?.onPdfFragmentLoaded(nbPages)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pdfView = null
        pdfLoadCallback = null
    }
}