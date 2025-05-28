package it.lavorodigruppo.flexipdf.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import it.lavorodigruppo.flexipdf.data.PdfDatasource
import it.lavorodigruppo.flexipdf.items.PdfFileItem

class PdfListViewModel(application: Application) : AndroidViewModel(application) {

    private val datasource = PdfDatasource(application)
    private val _pdfFiles = MutableLiveData<List<PdfFileItem>>()
    val pdfFiles: LiveData<List<PdfFileItem>> = _pdfFiles

    init {
        _pdfFiles.value = datasource.loadPdfFiles()
    }

    fun addPdfFile(pdfFile: PdfFileItem) {
        val currentList = _pdfFiles.value?.toMutableList() ?: mutableListOf()
        if (currentList.none { it.uriString == pdfFile.uriString }) {
            currentList.add(0, pdfFile)
            _pdfFiles.value = currentList
            datasource.savePdfFiles(currentList)
        }
    }

    //Still have to implement this feature into the recyclerView
    fun removePdfFile(pdfFile: PdfFileItem) {
        val currentList = _pdfFiles.value?.toMutableList() ?: mutableListOf()
        if (currentList.remove(pdfFile)) {
            _pdfFiles.value = currentList
            datasource.savePdfFiles(currentList)
        }
    }
}