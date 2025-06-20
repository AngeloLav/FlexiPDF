/**
 * @file HomeFragment.kt
 *
 * @brief Fragment che rappresenta la schermata principale dell'applicazione (Home).
 *
 * Questo Fragment è responsabile di visualizzare il contenuto della schermata Home.
 * Implementa una gestione specifica degli WindowInsets per assicurare che il layout
 * del banner si adatti correttamente alle barre di sistema, evitando che il contenuto venga coperto.
 *
 */
package it.lavorodigruppo.flexipdf.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import it.lavorodigruppo.flexipdf.activities.PDFViewerActivity
import it.lavorodigruppo.flexipdf.adapters.OnPdfItemClick
import it.lavorodigruppo.flexipdf.adapters.OnPdfItemLongClick
import it.lavorodigruppo.flexipdf.adapters.PdfHorizontalAdapter
import it.lavorodigruppo.flexipdf.databinding.FragmentHomeBinding
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import it.lavorodigruppo.flexipdf.viewmodels.FileSystemViewModel
import it.lavorodigruppo.flexipdf.viewmodels.FileSystemViewModel.FileSystemViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.ContentResolver

/**
 * `HomeFragment` è il frammento che visualizza la schermata principale dell'applicazione.
 * Mostra liste orizzontali di file PDF recenti e preferiti e gestisce l'apertura dei PDF.
 * Si adatta agli `WindowInsets` per posizionare correttamente gli elementi UI rispetto alle barre di sistema.
 */
class HomeFragment : Fragment() {

    /**
     * L'istanza del binding View per il layout del fragment.
     * Viene utilizzata per accedere alle viste nel layout in modo sicuro.
     */
    private var _binding: FragmentHomeBinding? = null

    /**
     * Proprietà di convenienza per accedere all'istanza del binding, assicurando che non sia nullo.
     */
    private val binding get() = _binding!!

    /**
     * Il ViewModel che fornisce i dati per le liste di PDF recenti e preferiti.
     */
    private lateinit var fileSystemViewModel: FileSystemViewModel

    /**
     * L'adapter per la RecyclerView che visualizza i PDF recenti.
     */
    private lateinit var recentPdfsAdapter: PdfHorizontalAdapter

    /**
     * L'adapter per la RecyclerView che visualizza i PDF preferiti.
     */
    private lateinit var favoritePdfsAdapter: PdfHorizontalAdapter

    /**
     * Memorizza il padding superiore originale del banner per ripristinare o calcolare gli insets correttamente.
     */
    private var originalBannerPaddingTop = 0

    /**
     * Override del metodo `onCreate` del ciclo di vita del Fragment.
     * Questo metodo viene chiamato all'inizio del ciclo di vita del Fragment.
     * Al momento, non contiene logica specifica.
     * @param savedInstanceState Se non nullo, questo Fragment viene ricostruito da uno stato precedentemente salvato.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Override del metodo `onCreateView` del ciclo di vita del Fragment.
     * Questo metodo è responsabile di gonfiare il layout del Fragment e restituire la sua vista radice.
     * Inizializza il binding e applica un listener per gli `WindowInsets` al fine di adattare
     * il padding superiore del banner alle barre di sistema (es. status bar).
     * @param inflater L'oggetto `LayoutInflater` che può essere usato per gonfiare qualsiasi vista nel contesto corrente.
     * @param container Se non nullo, questo è il ViewGroup padre a cui la UI del Fragment dovrebbe essere allegata.
     * @param savedInstanceState Se non nullo, questo Fragment viene ricostruito da uno stato precedentemente salvato.
     * @return La vista radice (View) del Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        val bannerContentLayout = binding.bannerContentLayout
        originalBannerPaddingTop = bannerContentLayout.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            bannerContentLayout.setPadding(
                bannerContentLayout.paddingLeft,
                originalBannerPaddingTop + systemBarsInsets.top,
                bannerContentLayout.paddingRight,
                bannerContentLayout.paddingBottom
            )
            insets
        }
        return view
    }

    /**
     * Override del metodo `onViewCreated` del ciclo di vita del Fragment.
     * Questo metodo viene chiamato subito dopo che `onCreateView` ha restituito la sua vista.
     * Qui vengono inizializzati il `FileSystemViewModel` e configurate le due `RecyclerView`
     * per i PDF recenti e preferiti. Infine, avvia l'osservazione dei dati dal ViewModel.
     * @param view La vista radice del Fragment restituita da `onCreateView`.
     * @param savedInstanceState Se non nullo, questo Fragment viene ricostruito da uno stato precedentemente salvato.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileSystemViewModel = ViewModelProvider(requireActivity(),
            FileSystemViewModel.FileSystemViewModelFactory(requireActivity().application)
        )[FileSystemViewModel::class.java]

        setupRecentPdfsRecyclerView()
        setupFavoritePdfsRecyclerView()

        observePdfsFromViewModel()
    }

    /**
     * Configura la `RecyclerView` per visualizzare la lista dei PDF recenti.
     * Inizializza `recentPdfsAdapter` con i listener di click e long click appropriati
     * (che chiamano `openPdfFile` o `toggleFavorite` sul ViewModel) e imposta il `LayoutManager`
     * per una visualizzazione orizzontale.
     */
    private fun setupRecentPdfsRecyclerView() {
        recentPdfsAdapter = PdfHorizontalAdapter(
            onPdfItemClick = { pdfFile ->
                openPdfFile(pdfFile)
            },
            onPdfItemLongClick = { pdfFile ->
                fileSystemViewModel.toggleFavorite(pdfFile)
                true
            }
        )
        binding.recentPdfRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentPdfsAdapter
        }
    }

    /**
     * Configura la `RecyclerView` per visualizzare la lista dei PDF preferiti.
     * Inizializza `favoritePdfsAdapter` con i listener di click e long click appropriati
     * (che chiamano `openPdfFile` o `toggleFavorite` sul ViewModel) e imposta il `LayoutManager`
     * per una visualizzazione orizzontale.
     */
    private fun setupFavoritePdfsRecyclerView() {
        favoritePdfsAdapter = PdfHorizontalAdapter(
            onPdfItemClick = { pdfFile ->
                openPdfFile(pdfFile)
            },
            onPdfItemLongClick = { pdfFile ->
                fileSystemViewModel.toggleFavorite(pdfFile)
                true
            }
        )
        binding.favoritePdfRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = favoritePdfsAdapter
        }
    }

    /**
     * Avvia l'osservazione dei `Flow` dei PDF recenti e preferiti dal `FileSystemViewModel`.
     * Ogni volta che le liste di PDF recenti o preferiti vengono aggiornate nel ViewModel,
     * questo metodo notifica i rispettivi adapter della `RecyclerView` per aggiornare la UI.
     */
    private fun observePdfsFromViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            fileSystemViewModel.recentPdfs.collectLatest { pdfs ->
                recentPdfsAdapter.submitList(pdfs)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            fileSystemViewModel.favoritePdfs.collectLatest { pdfs ->
                favoritePdfsAdapter.submitList(pdfs)
            }
        }
    }

    /**
     * Metodo di supporto per aprire un file PDF.
     * Costruisce un `Intent` per avviare `PDFViewerActivity`, passando l'URI del PDF
     * e il suo nome visualizzato. Concede anche i permessi di lettura URI necessari.
     * Mostra un `Toast` di conferma all'utente.
     * @param pdfFile L'oggetto `PdfFileItem` che rappresenta il file PDF da aprire.
     */
    private fun openPdfFile(pdfFile: PdfFileItem) {
        val uri = pdfFile.uriString.toUri()
        Log.d("HomeFragment", "Tentativo di aprire PDF con URI: $uri")
        val intent = Intent(requireContext(), PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", uri)
            putExtra("pdf_display_name", pdfFile.displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
        Toast.makeText(context, "Opening ${pdfFile.displayName}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Override del metodo `onDestroyView` del ciclo di vita del Fragment.
     * Chiamato quando la vista del Fragment sta per essere distrutta.
     * Esegue la pulizia delle risorse impostando l'istanza del binding a `null`
     * per prevenire memory leak.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}