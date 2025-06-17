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
    private var enableInternalSwipe: Boolean = true // <--- NUOVO: Variabile per controllare lo swipe interno

    private var pdfView: PDFView? = null
    private var pdfLoadCallback: PdfLoadCallback? = null

    companion object {
        private const val ARG_PDF_URI = "pdf_uri"
        private const val ARG_INITIAL_PAGE = "initial_page"
        private const val ARG_ENABLE_INTERNAL_SWIPE = "enable_internal_swipe" // <--- NUOVO: Chiave per lo swipe

        @JvmStatic
        fun newInstance(pdfUri: Uri, initialPage: Int = 0, enableInternalSwipe: Boolean = true): PdfViewerFragment { // <--- MODIFICATO: Aggiunto enableInternalSwipe
            return PdfViewerFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PDF_URI, pdfUri)
                    putInt(ARG_INITIAL_PAGE, initialPage)
                    putBoolean(ARG_ENABLE_INTERNAL_SWIPE, enableInternalSwipe) // <--- NUOVO: Passa il valore
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
            enableInternalSwipe = it.getBoolean(ARG_ENABLE_INTERNAL_SWIPE, true) // <--- NUOVO: Recupera il valore
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

        pdfView = view.findViewById(R.id.pdfViewFragment)

        pdfUri?.let { uri ->
            Log.d("PdfViewerFragment", "Tentativo di caricare PDF da URI: $uri, pagina: $initialPage, swipe interno: $enableInternalSwipe")
            try {
                pdfView?.fromUri(uri)
                    ?.defaultPage(initialPage)
                    // Il DefaultScrollHandle è sempre utile, ma il suo comportamento è influenzato da enableSwipe
                    ?.scrollHandle(DefaultScrollHandle(requireContext()))
                    ?.enableSwipe(enableInternalSwipe) // <--- MODIFICATO: Usa la nuova variabile per abilitare/disabilitare lo swipe
                    ?.enableDoubletap(true) // Il doubletap lo manteniamo abilitato
                    ?.onError(this)
                    ?.onLoad(this)
                    ?.load()
            } catch (e: Exception) {
                Log.e("PdfViewerFragment", "Errore durante il caricamento del PDF: ${e.message}", e)
                Toast.makeText(context, "Errore nel caricamento del PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } ?: Log.e("PdfViewerFragment", "Errore: URI PDF nullo nel Fragment durante onViewCreated.")
    }

    fun setPage(pageNumber: Int) {
        if (pdfView != null && pageNumber != pdfView!!.currentPage) {
            pdfView?.jumpTo(pageNumber)
            this.initialPage = pageNumber
            Log.d("PdfViewerFragment", "Jumped to page: $pageNumber")
        } else {
            Log.w("PdfViewerFragment", "Impossibile impostare la pagina: pdfView non inizializzata o stessa pagina ($pageNumber).")
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