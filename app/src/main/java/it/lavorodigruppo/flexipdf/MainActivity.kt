package it.lavorodigruppo.flexipdf

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import it.lavorodigruppo.flexipdf.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(HomeFragment())

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.folders -> replaceFragment(FoldersFragment())
                R.id.shared -> replaceFragment(SharedFragment())
                R.id.settings -> replaceFragment(SettingsFragment())
                else ->{
                }
            }
            true
        }
        enableEdgeToEdge()
    }

    private fun replaceFragment(fragment: Fragment) {

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()

    }
}