package it.lavorodigruppo.flexipdf.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class PdfManager(private val activity: AppCompatActivity, private val onPdfSelected: (Uri, String) -> Unit) {

    private val pickPdfFile = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                Log.d("PDF_PICKER", "PDF selected: $it")

                val displayName = getFileName(it)

                savePdfUri(it)
                onPdfSelected.invoke(it, displayName)
                // Uncomment if you want it to open the PDF viewer after selecting it
                // openPdfViewerActivity(it)
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

        } catch (e: Exception) {
            Log.e("PDF_PICKER", "Error in saving the permission for URI: ${e.message}")
            Snackbar.make(activity.findViewById(android.R.id.content), "Error in saving the PDF", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            activity.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = cursor.getString(displayNameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "Unknown PDF"
    }

}