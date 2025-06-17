// it.lavorodigruppo.flexipdf.fragments/OnPdfPickerListener.kt
package it.lavorodigruppo.flexipdf.fragments

import android.net.Uri

/**
 * Interfaccia di callback per notificare all'Activity ospitante che è necessario
 * avviare il selettore di file PDF.
 * Questo pattern garantisce una comunicazione pulita tra Fragment e Activity.
 */

interface OnPdfPickerListener {
    /**
     * Chiamato quando è necessario avviare il selettore di file PDF.
     */
    fun launchPdfPicker()
}

interface OnPdfFileClickListener {
    /**
     * Chiamato quando un file PDF viene cliccato.
     * @param pdfUri L'URI del file PDF cliccato.
     */
    fun onPdfFileClicked(pdfUri: Uri)
}

