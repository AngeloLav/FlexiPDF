/**
 * @file MainActivity.kt
 *
 * @overview
 *
 * Questa activity è il punto di ingresso dell'applicazione: ho utilizzato il viewBinding per potermi muovere attraverso
 * i diversi fragment dell'applicazione. All'inizio dichiaro delle variabili tramite lateinit (significa che le variabili
 * non saranno inizializzate immediatamente ma solo durante il loro primo utilizzo).
 *
 * binding: dichiarando questo oggetto della classe ActivityMainBinding (che viene generata automaticamente da AS quando nel
 * gradle:app aggiungi "buildFeatures { viewBinding true }") posso facilmente accedere a tutti i componenti dichiarati nel file
 * activity_main.xml. Attraverso binding = ActivityMainBinding.inflate(layoutInflater) e il metodo .inflate(), attacco al mio oggetto binding
 * il layout gonfiato e attraverso setContentView(binding.root) lo imposto come il layout della activity.
 * Attraverso il viewBinding secondo me è più facile impostare il layout rispetto all'uso di findViewById.
 * Utilizzo poi l'oggetto binding nel metodo onCreate per selezionare l'elemento bottomNavigationView: su questo uso il metodo
 * setOnItemSelectedListener per far in modo attraverso un when che quando clicco su un'icona del menù in base all'id dell'icona
 * selezionata si attivi la funzione replaceFragment che mi permette di selezionare il fragment corrispondente. Il true alla fine del metodo
 * è necessario perchè setOnItemSelectedListener ha bisogno di restituire un valore booleano; se fosse false vorrebbe dire che
 * l'evento non sarebbe stato gestito correttamente e non aggiornerebbe visivamente il bottomNav con l'item cliccato.
 *
 * pdfListViewModel: Questa variabile contiene un'istanza di PdfListViewModel: un ViewModel è una
 * classe progettata per memorizzare e gestire i dati relativi all'interfaccia utente in un modo che sopravviva ai cambiamenti
 * di configurazione dell'Activity (ad esempio, quando l'utente ruota lo schermo). pdfListViewModel gestisce la lista dei file PDF,
 * assicurandosi che non vada persa se l'Activity viene ricreata. L'inizializzazione avviene in 'onCreate'
 * tramite 'ViewModelProvider(this)[PdfListViewModel::class.java]', che garantisce il recupero o la creazione corretta del ViewModel. (this) fa in
 * modo che la MainActivity possieda il ciclo vita dei viewModel. [PdfListViewModel::class.java] mi permette di determinare il tipo
 * di viewModel che mi interessa; in questo caso è un PdfListViewModel.
 *      @see
 *      it.lavorodigruppo.flexipdf/viewmodels/PdfListViewModel.kt
 *
 * pdfManager: Questa variabile contiene un'istanza della classe personalizzata PdfManager: è responsabile di  operazioni
 * specifiche legate ai PDF. L'ho creata inizialmente per dividere la logica di alcune funzioni per i pdf dal fragment folders.
 * Durante la sua inizializzazione in onCreate, gli viene passato un callback (una funzione lambda: '{ uri: Uri, displayName: String -> ... }').
 * Un 'callback' è una funzione che verrà eseguita in un secondo momento, quando una specifica operazione è completata.
 * In questo caso, il 'PdfManager' invocherà questa funzione lambda quando l'utente avrà selezionato un PDF,
 * passando l'URI (Uniform Resource Identifier) del file e il suo nome. La lambda poi si occuperà di:
 * 1. Creare un nuovo oggetto PdfFileItem con le informazioni del PDF.
 * 2. Aggiungere questo nuovo PdfFileItem al pdfListViewModel, in modo che i dati della lista PDF vengano aggiornati.
 * 3. Mostrare un messaggio Toast all'utente per confermare l'avvenuta importazione del PDF.
 *      @see
 *      it.lavorodigruppo.flexipdf/utils/PdfManager
 *
 * replaceFragment: metodo che mi permette di cambiare fragment (come parametro riceve un'istanza del fragment da sostituire).
 * Crea prima un oggetto della classe FragmentManager, ereditata da AppCompatActivity. Attraverso questo oggetto con il metodo beginTransaction()
 * inizializzo l'oggetto e rendo possibile iniziare le operazioni sui fragment, con il metodo replace() seleziono e rimuovo qualunque fragment
 * attualmente presente nella mia activity_main.xml (R.id.frame_layout è l'id del FrameLayout in activity_main.xml) e lo sostituisco con il nuovo
 * fragment passato come parametro. commit() è un metodo che mi permette di finalizzare questi cambiamenti.
 *
 *
 */

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
        fragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
    }
}