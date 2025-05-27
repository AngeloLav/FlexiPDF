package it.lavorodigruppo.flexipdf.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import it.lavorodigruppo.flexipdf.fragments.HomeFragment
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.fragments.SettingsFragment
import it.lavorodigruppo.flexipdf.fragments.SharedFragment
import it.lavorodigruppo.flexipdf.databinding.ActivityMainBinding
import it.lavorodigruppo.flexipdf.fragments.FoldersFragment

import androidx.preference.PreferenceManager
import android.content.SharedPreferences
import it.lavorodigruppo.flexipdf.fragments.OnPdfPickerListener


class MainActivity : AppCompatActivity(), OnPdfPickerListener {

    private lateinit var binding: ActivityMainBinding


    // --- Variables and logic for picking and saving PDF files URI ---
    private val pickPdfFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                Log.d("PDF_PICKER", "PDF selected: $it")

                savePdfUri(it)

                // Temporary just to test the PDF viewer
                openPdfViewerActivity(it)

            } ?: run {
                Snackbar.make(binding.root, "No PDF selected", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(binding.root, "PDF selection interrupted", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun launchPdfPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"

        }
        pickPdfFile.launch(intent)
    }

    private fun savePdfUri(uri: Uri) {
        try {
            val contentResolver = applicationContext.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            contentResolver.takePersistableUriPermission(uri, takeFlags)

            sharedPreferences.edit().putString(PDF_URI_KEY, uri.toString()).apply()
            Snackbar.make(binding.root, "PDF imported!", Snackbar.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("PDF_PICKER", "Error in saving the permission for URI: ${e.message}")
            Snackbar.make(binding.root, "Error in saving the PDF", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun openPdfViewerActivity(pdfUri: Uri) {
        val intent = Intent(this, PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", pdfUri)
        }
        startActivity(intent)
    }

    private lateinit var sharedPreferences: SharedPreferences
    private val PDF_URI_KEY = "saved_pdf_uri"
    // --- End of variables and logic for picking and saving PDF files URI


    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }



    // --- MainActivity Lifecycle ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

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
}