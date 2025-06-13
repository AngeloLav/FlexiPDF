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
// it.lavorodigruppo.flexipdf.activities/PDFViewerActivity.kt
package it.lavorodigruppo.flexipdf.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.lavorodigruppo.flexipdf.R
import androidx.activity.enableEdgeToEdge

import android.content.Intent // Importa Intent per grantUriPermission
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.listener.OnErrorListener // <--- IMPORT CORRETTO

class PDFViewerActivity : AppCompatActivity(), OnErrorListener { // <--- INTERFACCIA CORRETTA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pdfviewer)
        enableEdgeToEdge()

        val pdfView = findViewById<PDFView>(R.id.pdfView)

        val pdfUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("pdf_uri", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("pdf_uri")
        }

        if (pdfUri != null) {
            Log.d("PDFViewerActivity", "Tentativo di caricare PDF da URI: $pdfUri")

            try {
                // WORKAROUND: Concedi un permesso temporaneo all'Activity corrente
                // Questo dovrebbe garantire che l'Activity possa leggere l'URI,
                // anche se il permesso persistente non è stato correttamente salvato.
                grantUriPermission(packageName, pdfUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d("PDFViewerActivity", "Permesso URI temporaneo concesso per: $pdfUri")

                pdfView.fromUri(pdfUri)
                    .scrollHandle(DefaultScrollHandle(this))
                    .enableSwipe(true)
                    .enableDoubletap(true)
                    .onError(this) // <--- IMPOSTA IL LISTENER DI ERRORE QUI
                    .load()
            } catch (e: Exception) {
                Log.e("PDFViewerActivity", "Errore durante la concessione del permesso o caricamento del PDF: ${e.message}", e)
                Toast.makeText(this, getString(R.string.pdf_load)+"${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }

        } else {
            Log.e("PDFViewerActivity", "Errore: URI PDF nullo nell'Intent.")
            Toast.makeText(this, getString(R.string.empty_pdf), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // <--- METODO onError CORRETTO ---
    override fun onError(e: Throwable?) {
        Log.e("PDFViewerActivity", "Errore dalla libreria PDF: ${e?.message}", e)
        Toast.makeText(this, getString(R.string.pdf_load)+"${e?.message}", Toast.LENGTH_LONG).show()
        // Puoi decidere di chiudere l'Activity qui o mostrare un messaggio più dettagliato
        // finish()
    }
}