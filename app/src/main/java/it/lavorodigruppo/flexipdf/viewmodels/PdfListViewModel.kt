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
 *
 */


package it.lavorodigruppo.flexipdf.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import it.lavorodigruppo.flexipdf.data.PdfDatasource
import it.lavorodigruppo.flexipdf.items.PdfFileItem

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
     *      @see
     *      it.lavorodigruppo.flexipdf/items/PdfFileItem.kt
     *      androidx.lifecycle.MutableLiveData
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
        viewModelScope.launch {
            _pdfFiles.value = datasource.loadPdfFiles()
        }
    }

    /**
     * Aggiunge un nuovo PdfFileItem alla lista dei PDF.
     * Se il PDF non è già presente (controllato tramite uriString), viene aggiunto all'inizio della lista.
     * Dopo l'aggiunta, la lista viene aggiornata e salvata tramite il datasource.
     *
     * @param pdfFile L'oggetto PdfFileItem da aggiungere alla lista.
     */
    fun addPdfFile(pdfFile: PdfFileItem) { // <-- MODIFICATO: Reso asincrono
        CoroutineScope(Dispatchers.IO).launch {
            val currentList = _pdfFiles.value?.toMutableList() ?: mutableListOf()

            // Controlla se un PDF con lo stesso URI è già presente nella lista.
            if (currentList.none { it.uriString == pdfFile.uriString }) {
                currentList.add(0, pdfFile.copy(isSelected = false)) // Assicurati che il nuovo file non sia selezionato
                datasource.savePdfFiles(currentList) // Salva la lista aggiornata
                withContext(Dispatchers.Main) {
                    _pdfFiles.value = currentList // Aggiorna il LiveData sul thread principale
                }
            }
        }
    }

    /**
     * Restituisce una lista di tutti i PdfFileItem attualmente selezionati.
     * @return Una lista di PdfFileItem selezionati.
     */
    fun getSelectedPdfFiles(): List<PdfFileItem> {
        return _pdfFiles.value?.filter { it.isSelected } ?: emptyList()
    }

    /**
     * Rimuove una lista specifica di PdfFileItem dalla lista principale.
     * Dopo la rimozione, gli elementi rimanenti vengono ri-assegnati al LiveData.
     * @param filesToRemove La lista di PdfFileItem da rimuovere.
     */
    fun removePdfFiles(filesToRemove: List<PdfFileItem>) {
        val currentList = _pdfFiles.value.orEmpty().toMutableList()
        // Rimuove tutti gli elementi presenti in 'filesToRemove' dalla 'currentList'.
        // Usiamo toSet() per una rimozione più efficiente se le liste sono grandi.
        currentList.removeAll(filesToRemove.toSet())
        _pdfFiles.value = currentList
    }

    /**
     * Toggla lo stato di selezione (isSelected) di un PdfFileItem.
     * Questo metodo crea una nuova lista con l'item aggiornato e la emette tramite LiveData.
     * Non salva lo stato di selezione in persistenza, in quanto è uno stato transitorio per la UI.
     *
     * @param pdfFile L'oggetto PdfFileItem di cui togglare la selezione.
     */
    fun togglePdfSelection(pdfFile: PdfFileItem) {
        val currentList = _pdfFiles.value?.toMutableList() ?: mutableListOf()
        val updatedList = currentList.map { item ->
            if (item.uriString == pdfFile.uriString) {
                // Crea una nuova istanza dell'oggetto con lo stato isSelected invertito
                item.copy(isSelected = !item.isSelected)
            } else {
                item // Restituisce l'elemento invariato
            }
        }
        _pdfFiles.value = updatedList // Aggiorna il LiveData, che notificherà l'Adapter
    }

    /**
     * Deseleziona tutti i PdfFileItem nella lista.
     * Utile quando si esce dalla modalità di selezione.
     * Emette una nuova lista tramite LiveData.
     */
    fun clearAllSelections() {
        val currentList = _pdfFiles.value.orEmpty()
        val updatedList = currentList.map { it.copy(isSelected = false) } // Crea una nuova lista con tutti isSelected a false
        _pdfFiles.value = updatedList // Aggiorna il LiveData, che notificherà l'Adapter
    }

    /**
     * Aggiunge più file PDF alla lista del ViewModel partendo dai loro URI e nomi visualizzati.
     * Estrae la dimensione del file per ciascun URI.
     * @param pdfUris A list di URI che puntano ai file PDF.
     * @param displayNames A list di nomi visualizzati corrispondenti ai pdfUris.
     * @param context Application context needed to resolve content URIs for file size.
     */
    fun addPdfFilesFromUris(pdfUris: List<Uri>, displayNames: List<String>, context: Context) {
        val currentList = _pdfFiles.value.orEmpty().toMutableList()
        val newPdfItems = mutableListOf<PdfFileItem>()

        for (i in pdfUris.indices) {
            val uri = pdfUris[i]
            val displayName = displayNames[i]

            val newPdfItem = PdfFileItem(
                uriString = uri.toString(),
                displayName = displayName,
                isSelected = false
            )

            if (!currentList.any { it.uriString == newPdfItem.uriString }) {
                newPdfItems.add(newPdfItem)
            }
        }

        if (newPdfItems.isNotEmpty()) {
            currentList.addAll(newPdfItems)
            _pdfFiles.value = currentList.sortedBy { it.displayName.lowercase() }
        }
    }

}