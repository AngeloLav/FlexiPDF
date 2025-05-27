package it.lavorodigruppo.flexipdf.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.lavorodigruppo.flexipdf.R

// PDF imports
import android.net.Uri
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle

class PDFViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pdfviewer)

        val pdfView = findViewById<PDFView>(R.id.pdfView)

        // getParcelableExtra is deprecated but it works
        val pdfUri: Uri? = intent.getParcelableExtra("pdf_uri")

        if (pdfUri != null) {
            Log.d("PDF_VIEWER", "Attempting to load PDF from URI: $pdfUri")

            pdfView.fromUri(pdfUri)
                // Add different configurations here
                .scrollHandle(DefaultScrollHandle(this))
                .enableSwipe(true)
                .enableDoubletap(true)
                .load()
        } else {
            Log.e("PDF_VIEWER", "No PDF URI found in Intent extras. Loading default asset (or showing error).")
        }
    }
}