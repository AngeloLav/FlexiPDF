package it.lavorodigruppo.flexipdf.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import it.lavorodigruppo.flexipdf.items.toJson
import it.lavorodigruppo.flexipdf.items.toPdfFileList

class PdfDatasource(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("pdf_prefs", Context.MODE_PRIVATE)

    private val PDF_LIST_KEY = "pdf_list_key"

    fun loadPdfFiles(): List<PdfFileItem> {
        val json = sharedPreferences.getString(PDF_LIST_KEY, null)
        return json?.toPdfFileList() ?: emptyList()
    }

    fun savePdfFiles(pdfFiles: List<PdfFileItem>) {
        sharedPreferences.edit {
            putString(PDF_LIST_KEY, pdfFiles.toJson())
        }
    }
}