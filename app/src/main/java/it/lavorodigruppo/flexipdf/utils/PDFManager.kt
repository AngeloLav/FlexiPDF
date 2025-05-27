package it.lavorodigruppo.flexipdf.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import it.lavorodigruppo.flexipdf.activities.PDFViewerActivity

class PdfManager(private val activity: AppCompatActivity) {

    private val PDF_URI_KEY = "saved_pdf_uri"
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

    private val pickPdfFile = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                Log.d("PDF_PICKER", "PDF selected: $it")

                savePdfUri(it)

                // TEMPORARY, just for testing
                openPdfViewerActivity(it)

            } ?: run {
                Snackbar.make(activity.findViewById(android.R.id.content), "No PDF selected", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(activity.findViewById(android.R.id.content), "PDF selection interrupted", Snackbar.LENGTH_SHORT).show()
        }
    }

    fun launchPdfPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        pickPdfFile.launch(intent)
    }

    private fun savePdfUri(uri: Uri) {
        try {
            val contentResolver = activity.applicationContext.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            contentResolver.takePersistableUriPermission(uri, takeFlags)

            sharedPreferences.edit { putString(PDF_URI_KEY, uri.toString()) }
            Snackbar.make(activity.findViewById(android.R.id.content), "PDF imported!", Snackbar.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("PDF_PICKER", "Error in saving the permission for URI: ${e.message}")
            Snackbar.make(activity.findViewById(android.R.id.content), "Error in saving the PDF", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun openPdfViewerActivity(pdfUri: Uri) {
        val intent = Intent(activity, PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", pdfUri)
        }
        activity.startActivity(intent)
    }
}