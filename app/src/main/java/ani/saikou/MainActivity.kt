package ani.saikou

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.saikou.anilist.Anilist
import ani.saikou.anilist.AnilistHomeViewModel
import ani.saikou.databinding.ActivityMainBinding
import ani.saikou.media.MediaDetailsActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joery.animatedbottombar.AnimatedBottomBar
import java.io.Serializable

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private val scope = lifecycleScope
    private var load = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.doOnAttach {
        initActivity(this)

            binding.navbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }

        if (!isOnline(this)) {
            toastString("No Internet Connection")
            startActivity(Intent(this, NoInternet::class.java))
        }
        else{
            val  model : AnilistHomeViewModel by viewModels()
            model.genres.observe(this) {
                if (it) {
                    val navbar = binding.navbar
                    bottomBar = navbar
                    navbar.visibility = View.VISIBLE
                    binding.mainProgressBar.visibility = View.GONE
                    val mainViewPager = binding.viewpager
                    mainViewPager.isUserInputEnabled = false
                    mainViewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
                    mainViewPager.setPageTransformer(ZoomOutPageTransformer(true))
                    navbar.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
                        override fun onTabSelected(
                            lastIndex: Int,
                            lastTab: AnimatedBottomBar.Tab?,
                            newIndex: Int,
                            newTab: AnimatedBottomBar.Tab
                        ) {
                            navbar.animate().translationZ(12f).setDuration(200).start()
                            selectedOption = newIndex
                            mainViewPager.setCurrentItem(newIndex, false)
                        }
                    })
                    navbar.selectTabAt(selectedOption)
                    mainViewPager.post { mainViewPager.setCurrentItem(selectedOption, false) }

                    if (loadMedia != null) {
                        scope.launch {
                            val media = withContext(Dispatchers.IO){ Anilist.query.getMedia(loadMedia!!, loadIsMAL) }
                            if (media != null) {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        MediaDetailsActivity::class.java
                                    ).putExtra("media", media as Serializable)
                                )
                            } else {
                                toastString("Seems like that wasn't found on Anilist.")
                            }
                        }
                    }
                }
            }
            //Load Data
            if (!load) {
                Anilist.getSavedToken(this)
                scope.launch(Dispatchers.IO) { model.genres.postValue(Anilist.query.genreCollection(this@MainActivity)) }
                load = true
            }
        }
        }
    }

    //Double Tap Back
    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        val snackBar = Snackbar.make(binding.root, "Please click BACK again to exit", Snackbar.LENGTH_LONG)
        snackBar.view.translationY = -navBarHeight.dp - if(binding.navbar.scaleX==1f) binding.navbar.height - 2f else 0f
        snackBar.show()

        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    //ViewPager
    private class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            when (position){
                0-> return AnimeFragment()
                1-> return if (Anilist.token!=null) HomeFragment() else LoginFragment()
                2-> return MangaFragment()
            }
            return LoginFragment()
        }
    }

}