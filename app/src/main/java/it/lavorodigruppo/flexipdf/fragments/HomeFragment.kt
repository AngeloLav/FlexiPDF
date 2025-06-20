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
import it.lavorodigruppo.flexipdf.adapters.OnPdfItemClick // Importa le nuove typealias
import it.lavorodigruppo.flexipdf.adapters.OnPdfItemLongClick // Importa le nuove typealias
import it.lavorodigruppo.flexipdf.adapters.PdfHorizontalAdapter
import it.lavorodigruppo.flexipdf.databinding.FragmentHomeBinding
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import it.lavorodigruppo.flexipdf.viewmodels.FileSystemViewModel
import it.lavorodigruppo.flexipdf.viewmodels.FileSystemViewModel.FileSystemViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.ContentResolver


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var fileSystemViewModel: FileSystemViewModel

    private lateinit var recentPdfsAdapter: PdfHorizontalAdapter
    private lateinit var favoritePdfsAdapter: PdfHorizontalAdapter

    private var originalBannerPaddingTop = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza il ViewModel con lo scope dell'Activity usando la Factory
        fileSystemViewModel = ViewModelProvider(requireActivity(),
            FileSystemViewModel.FileSystemViewModelFactory(requireActivity().application)
        )[FileSystemViewModel::class.java]

        setupRecentPdfsRecyclerView()
        setupFavoritePdfsRecyclerView()

        observePdfsFromViewModel()
    }

    private fun setupRecentPdfsRecyclerView() {
        // Passa le lambda corrette al costruttore dell'adapter
        recentPdfsAdapter = PdfHorizontalAdapter(
            onPdfItemClick = { pdfFile ->
                openPdfFile(pdfFile)
            },
            onPdfItemLongClick = { pdfFile ->
                // Per la HomeFragment, un long click può togglare il preferito
                fileSystemViewModel.toggleFavorite(pdfFile)
                true // Consuma l'evento
            }
        )
        binding.recentPdfRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentPdfsAdapter
        }
    }

    private fun setupFavoritePdfsRecyclerView() {
        // Passa le lambda corrette al costruttore dell'adapter
        favoritePdfsAdapter = PdfHorizontalAdapter(
            onPdfItemClick = { pdfFile ->
                openPdfFile(pdfFile)
            },
            onPdfItemLongClick = { pdfFile ->
                // Per la HomeFragment, un long click può togglare il preferito
                fileSystemViewModel.toggleFavorite(pdfFile)
                true // Consuma l'evento
            }
        )
        binding.favoritePdfRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = favoritePdfsAdapter
        }
    }

    private fun observePdfsFromViewModel() {
        // Osserva recentPdfs come StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            fileSystemViewModel.recentPdfs.collectLatest { pdfs ->
                recentPdfsAdapter.submitList(pdfs)
            }
        }

        // Osserva favoritePdfs come StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            fileSystemViewModel.favoritePdfs.collectLatest { pdfs ->
                favoritePdfsAdapter.submitList(pdfs)
            }
        }
    }

    // Metodo helper per aprire un PDF (spostato dalla vecchia onPdfFileClick)
    private fun openPdfFile(pdfFile: PdfFileItem) {
        val uri = pdfFile.uriString.toUri()
        Log.d("HomeFragment", "Tentativo di aprire PDF con URI: $uri") // <--- AGGIUNTO LOG
        val intent = Intent(requireContext(), PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", uri)
            putExtra("pdf_display_name", pdfFile.displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
        Toast.makeText(context, "Opening ${pdfFile.displayName}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}