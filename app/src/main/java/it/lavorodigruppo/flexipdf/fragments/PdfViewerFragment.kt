// app/src/main/java/it.lavorodigruppo.flexipdf.fragments/PdfViewerFragment.kt
package it.lavorodigruppo.flexipdf.fragments

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
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import it.lavorodigruppo.flexipdf.R // Assicurati che l'import sia corretto

class PdfViewerFragment : Fragment(), OnErrorListener { // Implementa OnErrorListener

    private var pdfUri: Uri? = null // Variabile per contenere l'URI del PDF
    private lateinit var pdfView: PDFView // Riferimento alla vista PDF

    companion object {
        // Chiave per passare l'URI del PDF tra il Fragment e l'Activity
        private const val ARG_PDF_URI = "pdf_uri"

        /**
         * Crea una nuova istanza di PdfViewerFragment con l'URI del PDF specificato.
         * Questo è il modo preferito per passare argomenti ai Fragment.
         *
         * @param pdfUri L'URI del PDF da visualizzare.
         * @return Una nuova istanza di PdfViewerFragment.
         */
        @JvmStatic
        fun newInstance(pdfUri: Uri): PdfViewerFragment {
            return PdfViewerFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PDF_URI, pdfUri)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recupera l'URI del PDF dagli argomenti
        arguments?.let {
            pdfUri = it.getParcelable(ARG_PDF_URI)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflata il layout per questo fragment
        return inflater.inflate(R.layout.fragment_pdf_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pdfView = view.findViewById(R.id.pdfViewFragment) // Inizializza PDFView con l'ID del layout del fragment

        if (pdfUri != null) {
            Log.d("PdfViewerFragment", "Tentativo di caricare PDF da URI: $pdfUri")

            try {
                // Carica il PDF dall'URI
                pdfView.fromUri(pdfUri)
                    .scrollHandle(DefaultScrollHandle(requireContext())) // Usa requireContext() per il contesto
                    .enableSwipe(true)
                    .enableDoubletap(true)
                    .onError(this) // Imposta il listener di errore del fragment
                    .load()
            } catch (e: Exception) {
                Log.e("PdfViewerFragment", "Errore durante il caricamento del PDF: ${e.message}", e)
                Toast.makeText(context, "Errore nel caricamento del PDF: ${e.message}", Toast.LENGTH_LONG).show()
                // Non chiamiamo finish() qui perché siamo in un fragment.
                // Potresti voler mostrare un messaggio di errore e nascondere la vista PDF.
            }
        } else {
            Log.e("PdfViewerFragment", "Errore: URI PDF nullo nel Fragment.")
            Toast.makeText(context, "Errore: nessun PDF da visualizzare nel fragment.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Callback per gli errori della libreria PDF.
     */
    override fun onError(e: Throwable?) {
        Log.e("PdfViewerFragment", "Errore dalla libreria PDF: ${e?.message}", e)
        Toast.makeText(context, "Errore durante il caricamento del PDF: ${e?.message}", Toast.LENGTH_LONG).show()
    }
}