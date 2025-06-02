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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

import androidx.recyclerview.widget.LinearLayoutManager
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.adapters.OnPdfFileClickListener
import it.lavorodigruppo.flexipdf.adapters.PdfHorizontalAdapter
import it.lavorodigruppo.flexipdf.databinding.FragmentHomeBinding
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import it.lavorodigruppo.flexipdf.viewmodels.PdfListViewModel

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import it.lavorodigruppo.flexipdf.activities.PDFViewerActivity


class HomeFragment : Fragment(), OnPdfFileClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Ottieni il ViewModel con lo scope dell'Activity
    private lateinit var pdfListViewModel: PdfListViewModel

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

        // Assicurati che bannerContentLayout non sia null prima di accedere a paddingTop
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

        // Inizializza il ViewModel con lo scope dell'Activity
        pdfListViewModel = ViewModelProvider(requireActivity())[PdfListViewModel::class.java]

        setupRecentPdfsRecyclerView()
        setupFavoritePdfsRecyclerView()

        observePdfsFromViewModel()
    }

    private fun setupRecentPdfsRecyclerView() {
        recentPdfsAdapter = PdfHorizontalAdapter(this)
        binding.recentPdfRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentPdfsAdapter
        }
    }

    private fun setupFavoritePdfsRecyclerView() {
        favoritePdfsAdapter = PdfHorizontalAdapter(this)
        binding.favoritePdfRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = favoritePdfsAdapter
        }
    }

    private fun observePdfsFromViewModel() {
        pdfListViewModel.recentPdfs.observe(viewLifecycleOwner) { pdfs ->
            pdfs?.let {
                recentPdfsAdapter.submitList(it)
            }

        }

        pdfListViewModel.favoritePdfs.observe(viewLifecycleOwner) { pdfs ->
            pdfs?.let {
                favoritePdfsAdapter.submitList(it)
            }

        }
    }

    override fun onPdfFileClick(pdfFile: PdfFileItem) {
        val intent = Intent(requireContext(), PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", pdfFile.uriString.toUri())
            putExtra("pdf_display_name", pdfFile.displayName)
        }
        startActivity(intent)
        Toast.makeText(context, "Opening ${pdfFile.displayName}", Toast.LENGTH_SHORT).show()
    }

    override fun onPdfFileLongClick(pdfFile: PdfFileItem) {
        pdfListViewModel.toggleFavorite(pdfFile)
    }

    override fun onDeleteIconClick(pdfFile: PdfFileItem) {
        // Nessuna azione qui per HomeFragment
    }

    override fun onFavoriteIconClick(pdfFile: PdfFileItem) {
        // Nessuna azione qui per HomeFragment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}