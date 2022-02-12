package ani.saikou.manga

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import ani.saikou.R
import ani.saikou.databinding.FragmentMangaSourceBinding
import ani.saikou.manga.source.MangaSources
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.media.SourceSearchDialogFragment
import ani.saikou.navBarHeight
import ani.saikou.px
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

@SuppressLint("SetTextI18n")
class MangaSourceFragment : Fragment() {
    private var _binding: FragmentMangaSourceBinding? = null
    private val binding get() = _binding!!
    private var screenWidth:Float =0f
    private var timer: CountDownTimer?=null

    private var selected: ImageView?=null
    private var selectedChip: Chip?= null
    private var start = 0
    private var end:Int?=null
    private var loading = true
    private var progress = View.VISIBLE
    private lateinit var model : MediaDetailsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMangaSourceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() { super.onDestroyView();_binding = null }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        screenWidth = resources.displayMetrics.run { widthPixels / density }
        binding.mangaSourceContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += 128f.px+navBarHeight }
        binding.mangaSourceTitle.isSelected = true
        super.onViewCreated(view, savedInstanceState)
        val a : MediaDetailsViewModel by activityViewModels()
        val scope = viewLifecycleOwner.lifecycleScope
        model = a
        model.getMedia().observe(viewLifecycleOwner) {
            val media = it
            if (media?.manga != null) {
//                if (media.manga.nextAiringChapterTime!=null && (media.manga.nextAiringChapterTime!!-System.currentTimeMillis()/1000)<=86400*7.toLong()) {
//                    binding.mangaSourceCountdownContainer.visibility = View.VISIBLE
//                    timer = object :
//                        CountDownTimer((media.manga.nextAiringChapterTime!! + 10000)*1000-System.currentTimeMillis(), 1000) {
//                        override fun onTick(millisUntilFinished: Long) {
//                            val a = millisUntilFinished/1000
//                            binding.mangaSourceCountdown.text = "Next Chapter will be released in \n ${a/86400} days ${a%86400/3600} hrs ${a%86400%3600/60} mins ${a%86400%3600%60} secs"
//                        }
//                        override fun onFinish() {
//                            binding.mangaSourceCountdownContainer.visibility = View.GONE
//                        }
//                    }
//                    timer?.start()
//                }

                binding.mediaLoadProgressBar.visibility = View.GONE
                progress = View.GONE

                if (media.format == "MANGA") {
                    binding.mangaSourceContainer.visibility = View.VISIBLE
                    val sources: Array<String> = resources.getStringArray(R.array.manga_sources)
                    binding.mangaSource.setText(sources[media.selected!!.source])
                    binding.mangaSource.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            R.layout.item_dropdown,
                            sources
                        )
                    )
                    binding.mangaSource.setOnItemClickListener { _, _, i, _ ->
                        binding.mangaSourceRecycler.adapter = null
                        binding.mangaSourceChipGroup.removeAllViews()
                        loading = true
                        binding.mangaSourceProgressBar.visibility = View.VISIBLE
                        media.selected!!.source = i
                        model.saveSelected(media.id, media.selected!!, requireActivity())
                        MangaSources[i]!!.live.observe(viewLifecycleOwner) { j ->
                            binding.mangaSourceTitle.text = j
                        }
                        scope.launch { withContext(Dispatchers.IO){ model.loadMangaChapters(media, i) } }
                    }

                    binding.mangaSourceSearch.setOnClickListener {
                        SourceSearchDialogFragment().show(
                            requireActivity().supportFragmentManager,
                            null
                        )
                    }

                    selected = when (media.selected!!.recyclerStyle) {
                        0 -> binding.mangaSourceList
                        1 -> binding.mangaSourceCompact
                        else -> binding.mangaSourceList
                    }
                    selected?.alpha = 1f
                    binding.mangaSourceTop.rotation =
                        if (!media.selected!!.recyclerReversed) 90f else -90f
                    binding.mangaSourceTop.setOnClickListener {
                        binding.mangaSourceTop.rotation =
                            if (media.selected!!.recyclerReversed) 90f else -90f
                        media.selected!!.recyclerReversed = !media.selected!!.recyclerReversed
                        updateRecycler(media)
                    }
                    binding.mangaSourceList.setOnClickListener {
                        media.selected!!.recyclerStyle = 0
                        selected?.alpha = 0.33f
                        selected = binding.mangaSourceList
                        selected?.alpha = 1f
                        updateRecycler(media)
                    }
                    binding.mangaSourceCompact.setOnClickListener {
                        media.selected!!.recyclerStyle = 1
                        selected?.alpha = 0.33f
                        selected = binding.mangaSourceCompact
                        selected?.alpha = 1f
                        updateRecycler(media)
                    }

                    model.getMangaChapters().observe(viewLifecycleOwner) { loadedChapters ->
                        binding.mangaSourceRecycler.adapter = null
                        binding.mangaSourceChipGroup.removeAllViews()
                        loading = true
                        binding.mangaSourceProgressBar.visibility = View.VISIBLE
                        if (loadedChapters != null) {
                            val chapters = loadedChapters[media.selected!!.source]
                            if (chapters != null) {
                                media.manga.chapters = chapters
                                //CHIP GROUP
                                addPageChips(media, chapters.size)
                                updateRecycler(media)
                            }
                        }
                    }
                    MangaSources[media.selected!!.source]!!.live.observe(viewLifecycleOwner) { j ->
                        binding.mangaSourceTitle.text = j
                    }
                    scope.launch {
                        withContext(Dispatchers.IO){ model.loadMangaChapters(media, media.selected!!.source) }
                    }
                } else {
                    binding.mangaSourceNovel.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mediaLoadProgressBar.visibility = progress
    }

    override fun onDestroy() {
        timer?.cancel()
        MangaSources.flushLive()
        super.onDestroy()
    }

    private fun updateRecycler(media: Media){
        model.saveSelected(media.id,media.selected!!,requireActivity())
        if(media.manga?.chapters!=null) {
            binding.mangaSourceRecycler.adapter = mangaChapterAdapter(media, this, media.selected!!.recyclerStyle, media.selected!!.recyclerReversed, start, end)
            val gridCount = when (media.selected!!.recyclerStyle){
                0->1
                1->(screenWidth/80f).toInt()
                else->1
            }
            binding.mangaSourceRecycler.layoutManager = GridLayoutManager(requireContext(), gridCount)
            loading = false
            binding.mangaSourceProgressBar.visibility = View.GONE
            if(media.manga.chapters!!.isNotEmpty())
                binding.mangaSourceNotFound.visibility = View.GONE
            else
                binding.mangaSourceNotFound.visibility = View.VISIBLE
        }
    }

    fun onMangaChapterClick(media: Media, i:String){
        if (media.manga?.chapters?.get(i)!=null) {
            media.manga.selectedChapter = i
            val intent = Intent(activity, MangaReaderActivity::class.java).apply { putExtra("media", media) }
            startActivity(intent)
        }
    }

    private fun addPageChips(media: Media, total: Int){
        val divisions = total.toDouble() / 10
        start = 0
        end = null
        val limit = when{
            (divisions < 25) -> 25
            (divisions < 50) -> 50
            else -> 100
        }
        if (total>limit) {
            val arr = media.manga!!.chapters!!.keys.toTypedArray()
            val stored = ceil((total).toDouble() / limit).toInt()
            (1..stored).forEach {
                val chip = Chip(requireContext())
                chip.isCheckable = true
                val last = if (it == stored) total else (limit * it)

                if(it==media.selected!!.chip && selectedChip==null){
                    selectedChip=chip
                    chip.isChecked = true
                    start = limit * (it - 1)
                    end = last - 1
                }
                if (end == null) { end = limit * it - 1 }
                chip.text = "${arr[limit * (it - 1)]} - ${arr[last-1]}"
                chip.setOnClickListener { _ ->
                    media.selected!!.chip = it
                    selectedChip?.isChecked = false
                    selectedChip = chip
                    selectedChip!!.isChecked = true
                    start = limit * (it - 1)
                    end = last - 1
                    updateRecycler(media)
                }
                binding.mangaSourceChipGroup.addView(chip)
            }
        }
    }
}