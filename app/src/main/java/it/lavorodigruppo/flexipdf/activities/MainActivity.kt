package it.lavorodigruppo.flexipdf.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import it.lavorodigruppo.flexipdf.fragments.HomeFragment
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.fragments.SettingsFragment
import it.lavorodigruppo.flexipdf.fragments.SharedFragment
import it.lavorodigruppo.flexipdf.databinding.ActivityMainBinding
import it.lavorodigruppo.flexipdf.fragments.FoldersFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(HomeFragment())

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.folders -> replaceFragment(FoldersFragment())
                R.id.shared -> replaceFragment(SharedFragment())
                R.id.settings -> replaceFragment(SettingsFragment())

                //No other case should be added
                else ->{
                }
            }
            //It must be true or the item selected in the bottom navigation bar won't change
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