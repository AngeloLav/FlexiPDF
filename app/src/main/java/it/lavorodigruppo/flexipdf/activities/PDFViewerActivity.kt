package it.lavorodigruppo.flexipdf.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import it.lavorodigruppo.flexipdf.R

class PDFViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdfviewer)


        val pdfView = findViewById<PDFView>(R.id.pdfView)
        pdfView.fromAsset("PresAILAVORO.pdf")
            .load()


    }
}