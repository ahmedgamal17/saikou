package ani.saikou.anime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.math.MathUtils.clamp
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.anime.source.AnimeSources
import ani.saikou.anime.source.HSources
import ani.saikou.anime.source.Sources
import ani.saikou.databinding.FragmentAnimeWatchBinding
import ani.saikou.dp
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.navBarHeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.math.ceil


open class AnimeWatchFragment : Fragment() {
    open val sources : Sources = AnimeSources
    private var _binding: FragmentAnimeWatchBinding? = null
    private val binding get() = _binding!!
    private val model : MediaDetailsViewModel by activityViewModels()

    private lateinit var media : Media

    private var start = 0
    private var end : Int? = null
    private var chipAdapter : AnimeWatchChipAdapter? = null

    private var progress = View.VISIBLE

    var continueEp:Boolean=false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAnimeWatchBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.animeSourceRecycler.updatePadding(bottom = binding.animeSourceRecycler.paddingBottom + navBarHeight)
        binding.animeSourceRecycler.layoutManager = LinearLayoutManager(requireContext())
        continueEp = model.continueMedia?:false
        model.getMedia().observe(viewLifecycleOwner){
            if(it!=null){
                media = it
                media.selected = model.loadSelected(media.id)
                progress = View.GONE
                binding.mediaInfoProgressBar.visibility = progress

                model.watchSources = if(media.isAdult) HSources else AnimeSources

                val adapter = AnimeWatchAdapter(it,this,null,sources)
                binding.animeSourceRecycler.adapter = adapter

                model.getEpisodes().observe(viewLifecycleOwner) { loadedEpisodes ->
                    if (loadedEpisodes != null) {
                        val episodes = loadedEpisodes[media.selected!!.source]
                        if (episodes != null) {
                            episodes.forEach { (i, episode) ->
                                if (media.anime?.fillerEpisodes != null) {
                                    if (media.anime!!.fillerEpisodes!!.containsKey(i)) {
                                        episode.title = media.anime!!.fillerEpisodes!![i]?.title
                                        episode.filler = media.anime!!.fillerEpisodes!![i]?.filler ?: false
                                    }
                                }
                                if (media.anime?.kitsuEpisodes != null) {
                                    if (media.anime!!.kitsuEpisodes!!.containsKey(i)) {
                                        episode.desc = media.anime!!.kitsuEpisodes!![i]?.desc
                                        episode.title = media.anime!!.kitsuEpisodes!![i]?.title
                                        episode.thumb = media.anime!!.kitsuEpisodes!![i]?.thumb ?: media.cover
                                    }
                                }
                            }
                            media.anime?.episodes = episodes

                            //CHIP GROUP
                            val total = episodes.size
                            val divisions = total.toDouble() / 10
                            start = 0
                            end = null
                            val limit = when{
                                (divisions < 25) -> 25
                                (divisions < 50) -> 50
                                else -> 100
                            }
                            chipAdapter = null
                            if (total>limit) {
                                val arr = media.anime!!.episodes!!.keys.toTypedArray()
                                val stored = ceil((total).toDouble() / limit).toInt()
                                val position = clamp(media.selected!!.chip,0,stored-1)
                                val last = if (position+1 == stored) total else (limit * (position+1))
                                start = limit * (position)
                                end = last-1
                                chipAdapter = AnimeWatchChipAdapter(this,limit,arr,(1..stored).toList().toTypedArray(),position)
                            }
                            reload()
                        }
                    }
                }

                model.getKitsuEpisodes().observe(viewLifecycleOwner) { i ->
                    media.anime?.kitsuEpisodes = i
                }
                model.getFillerEpisodes().observe(viewLifecycleOwner) { i ->
                    media.anime?.fillerEpisodes = i
                }

                lifecycleScope.launch(Dispatchers.IO){
                    awaitAll(
                        async { model.loadKitsuEpisodes(media) },
                        async { model.loadFillerEpisodes(media) }
                    )
                    model.loadEpisodes(media, media.selected!!.source)
                }
            }
        }
    }

    fun onSourceChange(i:Int):LiveData<String>{
        media.anime?.episodes = null
        reload()
        media.selected!!.source = i
        model.saveSelected(media.id, media.selected!!, requireActivity())
        lifecycleScope.launch(Dispatchers.IO) { model.loadEpisodes(media, i) }
        return sources[i]!!.live
    }

    fun onIconPressed(viewType:Int,reverse:Boolean){
        media.selected!!.recyclerStyle = viewType
        media.selected!!.recyclerReversed = reverse
        reload()
    }

    fun onChipClicked(i:Int,s:Int,e:Int){
        media.selected!!.chip = i
        start = s
        end = e
        chipAdapter?.selected = i
        reload()
    }

    fun onEpisodeClick(i:String){
        model.continueMedia = false
        model.onEpisodeClick(media,i,requireActivity().supportFragmentManager)
    }

    private fun reload(){
        val selected = media.selected!!
        model.saveSelected(media.id,selected,requireActivity())
        val recyclerViewState = binding.animeSourceRecycler.layoutManager?.onSaveInstanceState()
        val adapters: ArrayList<RecyclerView.Adapter<out RecyclerView.ViewHolder>> = arrayListOf(AnimeWatchAdapter(media,this,chipAdapter,sources))
        if(media.anime?.episodes?.isNotEmpty()==true)
            adapters.add(episodeAdapter(media,this,resources.displayMetrics.widthPixels.dp,selected.recyclerStyle,selected.recyclerReversed,start,end))
        binding.animeSourceRecycler.adapter = ConcatAdapter(adapters)
        binding.animeSourceRecycler.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    override fun onDestroy() {
        sources.flushLive()
        super.onDestroy()
    }

    override fun onResume() {
        binding.mediaInfoProgressBar.visibility = progress
        super.onResume()
    }
}