package it.lavorodigruppo.flexipdf.activities

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.databinding.ActivityMainBinding
import it.lavorodigruppo.flexipdf.fragments.FoldersFragment
import it.lavorodigruppo.flexipdf.fragments.HomeFragment
import it.lavorodigruppo.flexipdf.fragments.SettingsFragment
import it.lavorodigruppo.flexipdf.fragments.SharedFragment

import it.lavorodigruppo.flexipdf.fragments.OnPdfPickerListener
import it.lavorodigruppo.flexipdf.utils.PdfManager
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import it.lavorodigruppo.flexipdf.viewmodels.PdfListViewModel


class MainActivity : AppCompatActivity(), OnPdfPickerListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pdfManager: PdfManager
    private lateinit var pdfListViewModel: PdfListViewModel


    override fun launchPdfPicker() {
        pdfManager.launchPdfPicker()
    }

    // --- MainActivity Lifecycle ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pdfListViewModel = ViewModelProvider(this)[PdfListViewModel::class.java]

        pdfManager = PdfManager(this) { uri: Uri, displayName: String ->
            val newPdfFile = PdfFileItem(uri.toString(), displayName)
            pdfListViewModel.addPdfFile(newPdfFile)
            Toast.makeText(this, "PDF imported: $displayName", Toast.LENGTH_SHORT).show()
        }

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.folders -> replaceFragment(FoldersFragment())
                R.id.shared -> replaceFragment(SharedFragment())
                R.id.settings -> replaceFragment(SettingsFragment())
                else -> {
                }
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}