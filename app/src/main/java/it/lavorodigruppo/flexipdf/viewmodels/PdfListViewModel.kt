/**
 * @file PdfListViewModel.kt
 *
 * @overview
 *
 * PdfListViewModel è un viewModel che gestisce i dati della lista dei file PDF nell'applicazione.
 *
 * Estendendo AndroidViewModel, questo ViewModel ha accesso al contesto dell'applicazione
 * tramite il costruttore. AndroidViewModel è una sottoclasse di ViewModel che ha una differenza fondamentale:
 * il suo costruttore accetta un oggetto Application. L'oggetto Application rappresenta l'intero ciclo di vita
 * dell'applicazione. A differenza dell'Activity o del Fragment Context, l'Application Context dura per tutta
 * la vita dell'applicazione e non viene distrutto e ricreato durante i cambiamenti di configurazione.
 *
 * Il suo scopo è:
 * - Mantenere lo stato della lista dei PDF (pdfFiles)
 * - Esporre la lista dei PDF alla UI tramite LiveData, permettendo un aggiornamento reattivo
 * quando i dati cambiano.
 * - Centralizzare la logica di business relativa alla gestione della lista dei PDF
 * (aggiunta, rimozione, caricamento/salvataggio), separandola dalla logica dell'interfaccia utente.
 *
 * @author Angelo
 *
 * @date 31/05/25
 *
 */


package it.lavorodigruppo.flexipdf.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import it.lavorodigruppo.flexipdf.data.PdfDatasource
import it.lavorodigruppo.flexipdf.items.PdfFileItem

class PdfListViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Datasource utilizzato per caricare e salvare la lista dei file PDF in memoria persistente.
     * Inizializzato con il contesto dell'applicazione.
     *      @see
     *      it.lavorodigruppo.flexipdf/data/PdfDatasource.kt
     */
    private val datasource = PdfDatasource(application)

    /**
     * MutableLiveData privato che contiene la lista corrente di PdfFileItem.
     * L'underscore _ è una convenzione di naming per suggerire che _pdfFiles è la sorgente modificabile
     * (mutabile) che verrà poi esposta all'esterno come una proprietà immutabile.
     *
     * 'Mutable' significa che il suo valore può essere modificato internamente al ViewModel.
     * Questo è il contenitore modificabile dei dati che verrà esposto tramite pdfFiles
     *
     * MutableLiveData è una classe della libreria `androidx.lifecycle`. È una sottoclasse di LiveData,
     * ossia un contenitore di dati osservabile e consapevole del ciclo di vita.
     * - **Osservabile:** Le componenti dell'UI (come Activity o Fragment) possono "osservare" un oggetto LiveData.
     * Quando il valore al suo interno cambia, gli osservatori vengono automaticamente notificati e aggiornati.
     * - **Consapevole del ciclo di vita:** LiveData notifica gli osservatori solo se sono in uno stato "attivo"
     * del loro ciclo di vita (es. `STARTED` o `RESUMED`). Questo previene memory leaks e crash,
     * poiché non invia aggiornamenti a componenti che non sono più attivi o sono stati distrutti.
     *
     *      @see
     *      it.lavorodigruppo.flexipdf/items/PdfFileItem.kt
     *      @see
     *      androidx.lifecycle.MutableLiveData
     *      @see
     *      androidx.lifecycle.LiveData
     */
    private val _pdfFiles = MutableLiveData<List<PdfFileItem>>()

    /**
     * LiveData pubblico e immutabile che espone la lista dei PdfFileItem alla UI.
     */
    val pdfFiles: LiveData<List<PdfFileItem>> = _pdfFiles

    /**
     * Blocco di inizializzazione che viene eseguito non appena l'istanza del ViewModel viene creata.
     * In questo blocco, carichiamo la lista iniziale dei PDF dal datasource
     * e assegniamo il valore a _pdfFiles, rendendolo disponibile agli osservatori.
     */
    init {
        _pdfFiles.value = datasource.loadPdfFiles()
    }

    /**
     * Aggiunge un nuovo PdfFileItem alla lista dei PDF.
     * Se il PDF non è già presente (controllato tramite uriString), viene aggiunto all'inizio della lista.
     * Dopo l'aggiunta, la lista viene aggiornata e salvata tramite il datasource.
     *
     * @param pdfFile L'oggetto PdfFileItem da aggiungere alla lista.
     *      @see
     *      it.lavorodigruppo.flexipdf/items/PdfFileItem.kt
     */
    fun addPdfFile(pdfFile: PdfFileItem) {

        val currentList = _pdfFiles.value?.toMutableList() ?: mutableListOf()

        // Controlla se un PDF con lo stesso URI è già presente nella lista.
        if (currentList.none { it.uriString == pdfFile.uriString }) {
            currentList.add(0, pdfFile)
            _pdfFiles.value = currentList
            datasource.savePdfFiles(currentList)
        }
    }

    /**
     * Rimuove un PdfFileItem dalla lista dei PDF.
     * Se il PDF viene trovato e rimosso, la lista viene aggiornata e salvata.
     *
     * @param pdfFile L'oggetto PdfFileItem da rimuovere dalla lista.
     *      @see
     *      it.lavorodigruppo.flexipdf/items/PdfFileItem.kt
     */

    // Still have to implement this feature into the RecyclerView
    fun removePdfFile(pdfFile: PdfFileItem) {

        val currentList = _pdfFiles.value?.toMutableList() ?: mutableListOf()

        //Non lancia eccezioni se l'elemento non viene trovato
        if (currentList.remove(pdfFile)) {
            _pdfFiles.value = currentList
            datasource.savePdfFiles(currentList)
        }
    }
}