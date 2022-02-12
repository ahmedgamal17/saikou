package ani.saikou.manga

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.saikou.databinding.ActivityMangaReaderBinding
import ani.saikou.media.Media
import ani.saikou.toastString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MangaReaderActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMangaReaderBinding
    private val scope = lifecycleScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMangaReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val a = intent.getSerializableExtra("media")
        if(a!=null) {
            var media = a as Media
            val model: MangaChapterViewModel by viewModels()
            model.getMangaChapMedia().observe(this) {
                if (it != null) {
                    media = it
                    val chapImages = media.manga!!.chapters!![media.manga!!.selectedChapter]?.images
                    val referer = media.manga!!.chapters!![media.manga!!.selectedChapter]?.referer
                    if (chapImages != null) {
                        binding.mangaReaderRecyclerView.adapter = ImageAdapter(chapImages, referer)
                        binding.mangaReaderRecyclerView.layoutManager =
                            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
//                    binding.mangaReaderRecyclerView.recycledViewPool.setMaxRecycledViews(1,0)
                    }
                }
            }
            scope.launch(Dispatchers.IO) { model.loadChapMedia(media) }
        }
        else{
            toastString("Please Reload.")
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}