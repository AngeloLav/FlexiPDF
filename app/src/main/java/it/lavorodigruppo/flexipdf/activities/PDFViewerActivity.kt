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

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.lavorodigruppo.flexipdf.R
import androidx.activity.enableEdgeToEdge

import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle

class PDFViewerActivity : AppCompatActivity() {

    /**
     * Metodo chiamato alla creazione dell'Activity.
     * Inizializza il layout, abilita la visualizzazione edge-to-edge e carica il PDF
     * recuperato dall'Intent.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Imposta il layout dell'Activity dal file XML 'activity_pdfviewer.xml'.
        setContentView(R.layout.activity_pdfviewer)

        // Abilita la visualizzazione edge-to-edge, estendendo il contenuto sotto
        // le barre di sistema (stato e navigazione) per un'esperienza a schermo intero.
        enableEdgeToEdge()

        // Trova il componente PDFView nel layout tramite il suo ID.
        // Nota: Qui si usa findViewById() perché questa Activity non utilizza View Binding,
        // a differenza di MainActivity.
        val pdfView = findViewById<PDFView>(R.id.pdfView)

        // Recupera l'URI del PDF dall'Intent che ha avviato questa Activity.
        // Il metodo getParcelableExtra("pdf_uri") è usato per estrarre un oggetto Parcelable
        // (come Uri) associato alla chiave "pdf_uri".
        // Il metodo è deprecato, ma funziona con questa libreria
        val pdfUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Per Android 13 (API 33) e superiori
            intent.getParcelableExtra("pdf_uri", Uri::class.java)
        } else {
            // Per versioni di Android inferiori a 13 (API 32 e precedenti)
            // Qui è necessario sopprimere il warning di deprecazione se si vuole supportare
            // versioni più vecchie e non si vuole che il compilatore si lamenti.
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("pdf_uri")
        }

        if (pdfUri != null) {
            // Carica il PDF nel componente PDFView utilizzando l'URI fornito. Si possono aggiungere anche altre
            // feature alla visualizzazione del PDF. Visionare la documentazione della libreria com.github.barteksc.pdfviewer
            pdfView.fromUri(pdfUri)
                .scrollHandle(DefaultScrollHandle(this))
                .enableSwipe(true)
                .enableDoubletap(true)
                .load()
        } else {
            // Messaggio Toast per informare l'utente che non è stato possibile caricare il PDF.
            Toast.makeText(this, "Errore: nessun PDF da visualizzare.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}