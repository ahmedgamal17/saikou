package ani.saikou.anime

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.databinding.ItemEpisodeCompactBinding
import ani.saikou.databinding.ItemEpisodeGridBinding
import ani.saikou.databinding.ItemEpisodeListBinding
import ani.saikou.loadData
import ani.saikou.media.Media
import ani.saikou.setAnimation
import ani.saikou.updateAnilistProgress
import com.squareup.picasso.Picasso
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

fun episodeAdapter(media:Media,fragment: AnimeWatchFragment,screenWidth:Float,style:Int,reversed:Boolean=false,start:Int=0,e:Int?=null): RecyclerView.Adapter<*> {
    val end = if(e!=null && e<media.anime!!.episodes!!.size) e else null
    var arr = media.anime!!.episodes!!.values.toList().slice(start..(end?:(media.anime.episodes!!.size-1)))
    arr = if (reversed) arr.reversed() else arr

    val adapters: ArrayList<RecyclerView.Adapter<out RecyclerView.ViewHolder>> = arrayListOf()

    val perRow = when (style){
        0->1
        1->(screenWidth/200f).roundToInt()
        2->(screenWidth/92f).roundToInt()
        else->1
    }

    for (i in 0 until max(1, ceil(arr.size.toDouble() / perRow).roundToInt())) {
        val en = (i + 1) * perRow
        val a = ArrayList(arr.subList(i * perRow, if(en<arr.size) en else arr.size))
        adapters.add(EpisodesGridAdapter(style,media,fragment,a))
    }

    return ConcatAdapter(adapters)
}


fun handleProgress(cont:LinearLayout,bar:View,empty:View,mediaId:Int,ep:String){
    val curr = loadData<Long>("${mediaId}_${ep}")
    val max = loadData<Long>("${mediaId}_${ep}_max")
    if(curr!=null && max!=null){
        cont.visibility=View.VISIBLE
        val div = curr.toFloat()/max
        val barParams = bar.layoutParams as LinearLayout.LayoutParams
        barParams.weight = div
        bar.layoutParams = barParams
        val params = empty.layoutParams as LinearLayout.LayoutParams
        params.weight = 1-div
        empty.layoutParams = params
    }else{
        cont.visibility = View.GONE
    }
}

class EpisodeCompactAdapter(
    private val media: Media,
    private val fragment: AnimeWatchFragment,
    private val arr: List<Episode>,
): RecyclerView.Adapter<EpisodeCompactAdapter.EpisodeCompactViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeCompactViewHolder {
        val binding = ItemEpisodeCompactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeCompactViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EpisodeCompactViewHolder, position: Int) {
        val binding = holder.binding
        setAnimation(fragment.requireContext(),holder.binding.root)
        val ep = arr[position]
        binding.itemEpisodeNumber.text = ep.number
        binding.itemEpisodeFillerView.visibility = if (ep.filler)  View.VISIBLE else View.GONE
        if (media.userProgress!=null) {
            if (ep.number.toFloatOrNull()?:9999f<=media.userProgress!!.toFloat())
                binding.root.alpha = 0.5f
            else{
                binding.root.setOnLongClickListener{
                    updateAnilistProgress(media.id, ep.number)
                    true
                }
            }
        }
        handleProgress(binding.itemEpisodeProgressCont,binding.itemEpisodeProgress,binding.itemEpisodeProgressEmpty,media.id,ep.number)
    }

    override fun getItemCount(): Int = arr.size

    inner class EpisodeCompactViewHolder(val binding: ItemEpisodeCompactBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                fragment.onEpisodeClick(arr[bindingAdapterPosition].number)
            }
        }
    }
}

class EpisodeGridAdapter(
    private val media: Media,
    private val fragment: AnimeWatchFragment,
    private val arr: List<Episode>,
): RecyclerView.Adapter<EpisodeGridAdapter.EpisodeGridViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeGridViewHolder {
        val binding = ItemEpisodeGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeGridViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EpisodeGridViewHolder, position: Int) {
        val binding = holder.binding
        setAnimation(fragment.requireContext(),holder.binding.root)
        val ep = arr[position]
        Picasso.get().load(ep.thumb?:media.cover).resize(400,0).into(binding.itemEpisodeImage)
        binding.itemEpisodeNumber.text = ep.number
        binding.itemEpisodeTitle.text = ep.title?:media.name
        if(ep.filler){
            binding.itemEpisodeFiller.visibility = View.VISIBLE
            binding.itemEpisodeFillerView.visibility = View.VISIBLE
        }else{
            binding.itemEpisodeFiller.visibility = View.GONE
            binding.itemEpisodeFillerView.visibility = View.GONE
        }
        if (media.userProgress!=null) {
            if (ep.number.toFloatOrNull()?:9999f<=media.userProgress!!.toFloat()) {
                binding.root.alpha = 0.66f
                binding.itemEpisodeViewed.visibility = View.VISIBLE
            }else{
                binding.root.setOnLongClickListener{
                    updateAnilistProgress(media.id, ep.number)
                    true
                }
            }
        }
        handleProgress(binding.itemEpisodeProgressCont,binding.itemEpisodeProgress,binding.itemEpisodeProgressEmpty,media.id,ep.number)
    }

    override fun getItemCount(): Int = arr.size

    inner class EpisodeGridViewHolder(val binding: ItemEpisodeGridBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                fragment.onEpisodeClick(arr[bindingAdapterPosition].number)
            }
        }
    }
}

class EpisodeListAdapter(
    private val media: Media,
    private val fragment: AnimeWatchFragment,
    private val arr: List<Episode>
): RecyclerView.Adapter<EpisodeListAdapter.EpisodeListViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeListViewHolder {
        val binding = ItemEpisodeListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeListViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EpisodeListViewHolder, position: Int) {
        val binding = holder.binding
        setAnimation(fragment.requireContext(),holder.binding.root)
        val ep = arr[position]
        Picasso.get().load(ep.thumb?:media.cover).resize(400,0).into(binding.itemEpisodeImage)
        binding.itemEpisodeNumber.text = ep.number
        if(ep.filler){
            binding.itemEpisodeFiller.visibility = View.VISIBLE
            binding.itemEpisodeFillerView.visibility = View.VISIBLE
        }else{
            binding.itemEpisodeFiller.visibility = View.GONE
            binding.itemEpisodeFillerView.visibility = View.GONE
        }
        binding.itemEpisodeDesc.visibility = if (ep.desc!=null && ep.desc?.trim(' ')!="") View.VISIBLE else View.GONE
        binding.itemEpisodeDesc.text = ep.desc?:""
        binding.itemEpisodeTitle.text = ep.title?:media.userPreferredName
        if (media.userProgress!=null) {
            if (ep.number.toFloatOrNull()?:9999f<=media.userProgress!!.toFloat()) {
                binding.root.alpha = 0.66f
                binding.itemEpisodeViewed.visibility = View.VISIBLE
            }
            else{
                binding.root.alpha=1f
                binding.itemEpisodeViewed.visibility = View.GONE
                binding.root.setOnLongClickListener{
                    updateAnilistProgress(media.id, ep.number)
                    true
                }
            }
        }else{
            binding.root.alpha=1f
            binding.itemEpisodeViewed.visibility = View.GONE
        }

        handleProgress(binding.itemEpisodeProgressCont,binding.itemEpisodeProgress,binding.itemEpisodeProgressEmpty,media.id,ep.number)
    }

    override fun getItemCount(): Int = arr.size

    inner class EpisodeListViewHolder(val binding: ItemEpisodeListBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                fragment.onEpisodeClick(arr[bindingAdapterPosition].number)
            }
            binding.itemEpisodeDesc.setOnClickListener {
                if(binding.itemEpisodeDesc.maxLines == 3)
                    binding.itemEpisodeDesc.maxLines = 100
                else
                    binding.itemEpisodeDesc.maxLines = 3
            }
        }
    }
}

