// it.lavorodigruppo.flexipdf.fragments/OnPdfPickerListener.kt
package it.lavorodigruppo.flexipdf.fragments

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