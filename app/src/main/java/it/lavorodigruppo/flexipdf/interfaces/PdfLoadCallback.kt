package it.lavorodigruppo.flexipdf.interfaces

interface PdfLoadCallback {
    fun onPdfFragmentLoaded(totalPages: Int)
}