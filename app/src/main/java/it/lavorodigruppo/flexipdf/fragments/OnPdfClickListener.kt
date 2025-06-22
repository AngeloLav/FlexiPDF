package it.lavorodigruppo.flexipdf.fragments

import android.net.Uri

/**
 * Interfaccia di callback per notificare all'Activity ospitante che è necessario
 * avviare il selettore di file PDF.
 * Questo pattern garantisce una comunicazione pulita tra Fragment e Activity,
 * separando le responsabilità.
 */
interface OnPdfPickerListener {
    /**
     * Metodo chiamato quando il Fragment richiede all'Activity ospitante di aprire il selettore di file PDF.
     * L'Activity che implementa questa interfaccia sarà responsabile di avviare l'Intent appropriato
     * per la selezione dei documenti.
     */
    fun launchPdfPicker()
}

/**
 * Interfaccia di callback per notificare all'Activity ospitante che un file PDF è stato cliccato.
 * Permette ai Fragment di comunicare l'intenzione di aprire un PDF senza dover conoscere
 * i dettagli specifici dell'Activity che gestirà la visualizzazione.
 */
interface OnPdfFileClickListener {
    /**
     * Metodo chiamato quando un file PDF viene cliccato una singola volta.
     * L'Activity implementante deciderà come gestire l'apertura del PDF (es. in un viewer interno, in un pannello, ecc.).
     * @param pdfUri L'URI del file PDF cliccato, che punta alla sua posizione di storage.
     */
    fun onPdfFileClicked(pdfUri: Uri)

    /**
     * Metodo chiamato quando un file PDF viene doppiamente cliccato o quando è richiesta un'azione
     * che forza l'apertura del PDF in una nuova Activity (`PDFViewerActivity`).
     * Questo può essere utilizzato per differenziare il comportamento di apertura (es. sempre in full screen).
     * @param pdfUri L'URI del file PDF doppiamente cliccato o per cui è richiesta l'apertura forzata.
     */
    fun onPdfFileClickedForceActivity(pdfUri: Uri)
}
