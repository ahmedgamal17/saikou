package ani.saikou.anime

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.R
import ani.saikou.anime.source.Sources
import ani.saikou.databinding.ItemAnimeWatchBinding
import ani.saikou.loadData
import ani.saikou.loadImage
import ani.saikou.media.Media
import ani.saikou.media.SourceSearchDialogFragment

class AnimeWatchAdapter(private val media: Media, private val fragment: AnimeWatchFragment, private val chips:AnimeWatchChipAdapter?=null,private val sources: Sources): RecyclerView.Adapter<AnimeWatchAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnimeWatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding

        //Timer
        if (media.anime?.nextAiringEpisodeTime != null && (media.anime.nextAiringEpisodeTime!! - System.currentTimeMillis() / 1000) <= 86400 * 7.toLong()) {
            binding.mediaCountdownContainer.visibility = View.VISIBLE
            object : CountDownTimer((media.anime.nextAiringEpisodeTime!! + 10000) * 1000 - System.currentTimeMillis(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val a = millisUntilFinished / 1000
                    binding.mediaCountdown.text = "Next Episode will be released in \n ${a / 86400} days ${a % 86400 / 3600} hrs ${a % 86400 % 3600 / 60} mins ${a % 86400 % 3600 % 60} secs"
                }
                override fun onFinish() { binding.mediaCountdownContainer.visibility = View.GONE }
            }.start()
        }

        //Youtube
        if (media.anime?.youtube != null) {
            binding.animeSourceYT.visibility = View.VISIBLE
            binding.animeSourceYT.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(media.anime.youtube))
                fragment.requireContext().startActivity(intent)
            }
        }

        //Source Selection
        binding.animeSource.setText(sources.names[media.selected!!.source])
        sources[media.selected!!.source]!!.live.observe(fragment.viewLifecycleOwner){ binding.animeSourceTitle.text = it }
        binding.animeSource.setAdapter(ArrayAdapter(fragment.requireContext(), R.layout.item_dropdown, sources.names))
        binding.animeSourceTitle.isSelected = true
        binding.animeSource.setOnItemClickListener { _, _, i, _ ->
            binding.animeSourceTitle.text = ""
            fragment.onSourceChange(i).observe(fragment.viewLifecycleOwner){ binding.animeSourceTitle.text = it }
        }

        //Wrong Title
        binding.animeSourceSearch.setOnClickListener {
            SourceSearchDialogFragment().show(fragment.requireActivity().supportFragmentManager, null)
        }

        //Icons
        var reversed = media.selected!!.recyclerReversed
        var style = media.selected!!.recyclerStyle
        binding.animeSourceTop.rotation = if (!reversed) 90f else -90f
        binding.animeSourceTop.setOnClickListener {
            binding.animeSourceTop.rotation = if (reversed) 90f else -90f
            reversed = !reversed
            fragment.onIconPressed(style,reversed)
        }
        when (media.selected!!.recyclerStyle) {
            0 -> binding.animeSourceList
            1 -> binding.animeSourceGrid
            2 -> binding.animeSourceCompact
            else -> binding.animeSourceList
        }.alpha = 1f
        binding.animeSourceList.setOnClickListener {
            style = 0
            fragment.onIconPressed(style,reversed)
        }
        binding.animeSourceGrid.setOnClickListener {
            style = 1
            fragment.onIconPressed(style,reversed)
        }
        binding.animeSourceCompact.setOnClickListener {
            style = 2
            fragment.onIconPressed(style,reversed)
        }

        //Chips
        if(chips!=null) {
            binding.animeSourceChipRecycler.adapter = chips
            val myLayoutManager = LinearLayoutManager(fragment.requireContext(), RecyclerView.HORIZONTAL, false)
            binding.animeSourceChipRecycler.layoutManager = myLayoutManager
            myLayoutManager.scrollToPosition(media.selected!!.chip)
        }

        //Episode Handling
        if(media.anime?.episodes!=null) {
            val episodes = media.anime.episodes!!.keys.toTypedArray()
            var continueEp = loadData<String>("${media.id}_current_ep")?:media.userProgress?.plus(1).toString()
            if(episodes.contains(continueEp)) {
                binding.animeSourceContinue.visibility = View.VISIBLE
                handleProgress(binding.itemEpisodeProgressCont,binding.itemEpisodeProgress,binding.itemEpisodeProgressEmpty,media.id,continueEp)
                if((binding.itemEpisodeProgress.layoutParams as LinearLayout.LayoutParams).weight>0.8f){
                    val  e = episodes.indexOf(continueEp)
                    if (e != - 1 && e+1 < episodes.size) {
                        continueEp = episodes[e + 1]
                        handleProgress(binding.itemEpisodeProgressCont,binding.itemEpisodeProgress,binding.itemEpisodeProgressEmpty,media.id,continueEp)
                    }
                }
                val ep = media.anime.episodes!![continueEp]!!
                loadImage(ep.thumb?:media.banner?:media.cover,binding.itemEpisodeImage)
                if(ep.filler) binding.itemEpisodeFillerView.visibility = View.VISIBLE
                binding.animeSourceContinueText.text = "Continue : Episode ${ep.number}${if(ep.filler) " - Filler" else ""}${if(ep.title!=null) "\n${ep.title}" else ""}"
                binding.animeSourceContinue.setOnClickListener {
                    fragment.onEpisodeClick(continueEp)
                }
                if(fragment.continueEp) {
                    if((binding.itemEpisodeProgress.layoutParams as LinearLayout.LayoutParams).weight<0.8f) {
                        binding.animeSourceContinue.performClick()
                        fragment.continueEp = false
                    }

                }
            }
            binding.animeSourceProgressBar.visibility = View.GONE
            if(media.anime.episodes!!.isNotEmpty())
                binding.animeSourceNotFound.visibility = View.GONE
            else
                binding.animeSourceNotFound.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(val binding: ItemAnimeWatchBinding) : RecyclerView.ViewHolder(binding.root)
}