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
import androidx.lifecycle.map
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

    //Questa è la lista completa di TUTTI i PDF, non filtrata.
    private val _allPdfFiles = MutableLiveData<List<PdfFileItem>>(emptyList())

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
    private val _pdfFilesFiltered = MutableLiveData<List<PdfFileItem>>(emptyList())

    /**
     * LiveData pubblico e immutabile che espone la lista dei PdfFileItem alla UI.
     */
    val pdfFiles: LiveData<List<PdfFileItem>> = _pdfFilesFiltered

    //Stringa di query di ricerca corrente.
    private var currentSearchQuery: String = ""

    /**
     * Blocco di inizializzazione che viene eseguito non appena l'istanza del ViewModel viene creata.
     * In questo blocco, carichiamo la lista iniziale dei PDF dal datasource
     * e assegniamo il valore a _pdfFiles, rendendolo disponibile agli osservatori.
     */
    init {
        viewModelScope.launch {
            Log.d("PdfListViewModel", "INIT: Avvio caricamento PDF.")
            val loadedFiles = datasource.loadPdfFiles()
            Log.d("PdfListViewModel", "INIT: PDF caricati dal datasource, numero: ${loadedFiles.size}")
            _allPdfFiles.value = loadedFiles // Carica tutti i file nella lista completa
            Log.d("PdfListViewModel", "INIT: _allPdfFiles.value aggiornato. Ora applico filtro.")
            applyFilter(currentSearchQuery)
            Log.d("PdfListViewModel", "INIT: Filtro applicato. _pdfFilesFiltered size: ${_pdfFilesFiltered.value?.size}")
        }
    }

    // --- NUOVE LIVE DATA PER HOME FRAGMENT ---

    // LiveData per i PDF più recenti (ordinati per lastModified, dal più recente al meno recente)
    val recentPdfs: LiveData<List<PdfFileItem>> = _allPdfFiles.map { allFiles ->
        // Ordina per lastModified in ordine decrescente (più recente prima)
        // Limita a un certo numero (es. 10), se vuoi, altrimenti rimuovi .take(10)
        allFiles.sortedByDescending { it.lastModified }.take(15)
    }

    // LiveData per i PDF preferiti
    val favoritePdfs: LiveData<List<PdfFileItem>> = _allPdfFiles.map { allFiles ->
        allFiles.filter { it.isFavorite }
    }

    /**
     * Restituisce una lista di tutti i PdfFileItem attualmente selezionati dalla lista COMPLETA.
     * @return Una lista di PdfFileItem selezionati.
     */
    fun getSelectedPdfFiles(): List<PdfFileItem> {
        return _allPdfFiles.value?.filter { it.isSelected } ?: emptyList() //Filtra da _allPdfFiles
    }

    /**
     * Rimuove una lista specifica di PdfFileItem dalla lista principale.
     * Dopo la rimozione, gli elementi rimanenti vengono ri-assegnati al LiveData.
     * @param filesToRemove La lista di PdfFileItem da rimuovere.
     */
    fun removePdfFiles(filesToRemove: List<PdfFileItem>) {
        viewModelScope.launch { // Esegui in coroutine perché salva su datasource
            val currentAllList = _allPdfFiles.value.orEmpty().toMutableList()
            // Rimuove tutti gli elementi presenti in 'filesToRemove' dalla 'currentAllList'.
            currentAllList.removeAll(filesToRemove.toSet())
            datasource.savePdfFiles(currentAllList) // Salva la lista completa aggiornata
            _allPdfFiles.value = currentAllList // Aggiorna la lista completa
            applyFilter(currentSearchQuery) //Riapplica il filtro dopo la rimozione
        }
    }

    /**
     * Toggla lo stato di selezione (isSelected) di un PdfFileItem.
     * Questo metodo crea una nuova lista con l'item aggiornato e la emette tramite LiveData.
     * Non salva lo stato di selezione in persistenza, in quanto è uno stato transitorio per la UI.
     *
     * @param pdfFile L'oggetto PdfFileItem di cui togglare la selezione.
     */
    fun togglePdfSelection(pdfFile: PdfFileItem) {
        val currentAllList = _allPdfFiles.value.orEmpty().toMutableList()
        val index = currentAllList.indexOfFirst { it.uriString == pdfFile.uriString }
        if (index != -1) {
            val updatedPdf = pdfFile.copy(isSelected = !pdfFile.isSelected)
            currentAllList[index] = updatedPdf
            _allPdfFiles.value = currentAllList // Aggiorna _allPdfFiles
            // Non è necessario chiamare applyFilter qui, a meno che tu non voglia che la selezione
            // influenzi la visibilità (es. nascondere elementi non selezionati in modalità ricerca).
            // Per ora, la selezione non influisce sul filtro di ricerca.
            applyFilter(currentSearchQuery)
        }
    }

    /**
     * Deseleziona tutti i PdfFileItem nella lista COMPLETA.
     * Utile quando si esce dalla modalità di selezione.
     * Emette una nuova lista tramite LiveData.
     */
    fun clearAllSelections() {
        val currentAllList = _allPdfFiles.value.orEmpty().map { it.copy(isSelected = false) } // Lavora su _allPdfFiles
        _allPdfFiles.value = currentAllList // Aggiorna _allPdfFiles
        // Non è necessario chiamare applyFilter qui, a meno che tu non voglia che la deselezione
        // influenzi il filtro di ricerca.
        applyFilter(currentSearchQuery)
    }

    /**
     * Aggiunge più file PDF alla lista del ViewModel partendo dai loro URI e nomi visualizzati.
     * Questa funzione ora aggiorna la lista completa e poi applica il filtro corrente.
     * @param pdfUris A list di URI che puntano ai file PDF.
     * @param displayNames A list di nomi visualizzati corrispondenti ai pdfUris.
     */
    fun addPdfFilesFromUris(pdfUris: List<Uri>, displayNames: List<String>) {
        viewModelScope.launch {
            val currentAllList = _allPdfFiles.value.orEmpty().toMutableList()
            val newPdfItems = mutableListOf<PdfFileItem>()

            if (pdfUris.size != displayNames.size) {
                Log.e("PdfListViewModel", "Le liste di URI e nomi non corrispondono in dimensione.")
                return@launch // Usa return@launch per uscire dalla coroutine
            }

            for (i in pdfUris.indices) {
                val uri = pdfUris[i]
                val displayName = displayNames[i]

                val newPdfItem = PdfFileItem(
                    uriString = uri.toString(),
                    displayName = displayName,
                    isSelected = false,
                    lastModified = System.currentTimeMillis(),
                    isFavorite = false
                )

                if (!currentAllList.any { it.uriString == newPdfItem.uriString }) {
                    newPdfItems.add(newPdfItem)
                } else {
                    Log.d("PdfListViewModel", "PDF già presente: ${newPdfItem.displayName} (URI: ${newPdfItem.uriString})")
                }
            }

            if (newPdfItems.isNotEmpty()) {
                currentAllList.addAll(newPdfItems)
                datasource.savePdfFiles(currentAllList) // Salva la lista completa aggiornata
                _allPdfFiles.value = currentAllList // Aggiorna la lista completa
                applyFilter(currentSearchQuery) // Applica il filtro dopo aver aggiunto nuovi elementi
                Log.d("PdfListViewModel", "Added ${newPdfItems.size} new PDF(s) to the list.")
            } else {
                Log.d("PdfListViewModel", "No PDF to add.")
            }
        }
    }

    /**
     * NUOVO: Toggla lo stato isFavorite di un PdfFileItem e salva la lista aggiornata.
     * @param pdfFile L'oggetto PdfFileItem di cui togglare lo stato preferito.
     */
    fun toggleFavorite(pdfFile: PdfFileItem) {
        viewModelScope.launch {
            val currentAllList = _allPdfFiles.value.orEmpty().toMutableList()
            val index = currentAllList.indexOfFirst { it.uriString == pdfFile.uriString }
            if (index != -1) {
                // Crea una nuova istanza con isFavorite invertito
                val updatedPdf = currentAllList[index].copy(isFavorite = !currentAllList[index].isFavorite)
                currentAllList[index] = updatedPdf // Sostituisci l'elemento nella lista
                datasource.savePdfFiles(currentAllList) // Salva la lista aggiornata in persistenza
                _allPdfFiles.value = currentAllList // Aggiorna il LiveData della lista completa
                applyFilter(currentSearchQuery) // Riapplica il filtro per aggiornare _pdfFilesFiltered e l'UI
            }
        }
    }

    /**
     * Applica un filtro alla lista completa dei PDF e aggiorna la lista filtrata.
     * @param query La stringa di ricerca da applicare. Se vuota, mostra tutti i PDF.
     */
    fun applyFilter(query: String) {
        currentSearchQuery = query.lowercase() // Salva la query e convertila in minuscolo per ricerca case-insensitive
        val allFiles = _allPdfFiles.value.orEmpty()

        if (currentSearchQuery.isEmpty()) {
            _pdfFilesFiltered.value = allFiles // Se la query è vuota, mostra tutti i file
        } else {
            val filteredList = allFiles.filter {
                it.displayName.lowercase().contains(currentSearchQuery)
            }.sortedBy { it.displayName.lowercase() } // Riordina anche la lista filtrata
            _pdfFilesFiltered.value = filteredList
        }
    }
}