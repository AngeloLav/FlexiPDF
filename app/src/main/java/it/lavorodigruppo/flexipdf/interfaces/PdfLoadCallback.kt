package it.lavorodigruppo.flexipdf.interfaces

/**
 * Interfaccia di callback utilizzata per notificare l'Activity o il Fragment chiamante
 * quando un frammento di visualizzazione PDF ha completato il caricamento.
 * Questo permette al componente che ha richiesto il caricamento del PDF
 * di essere informato sullo stato e ricevere dati rilevanti, come il numero totale di pagine.
 */
interface PdfLoadCallback {
    /**
     * Chiamato quando il frammento che visualizza il PDF (es. PdfViewerFragment)
     * ha terminato il caricamento del documento e determinato il numero totale delle sue pagine.
     * Questo metodo notifica al listener (tipicamente l'Activity contenitrice)
     * il completamento dell'operazione e il numero di pagine disponibili nel PDF.
     *
     * @param totalPages Il numero totale di pagine del documento PDF appena caricato.
     */
    fun onPdfFragmentLoaded(totalPages: Int)
}