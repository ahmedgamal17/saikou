package ani.saikou.anime

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.databinding.ItemChipBinding

class AnimeWatchChipAdapter(private val activity: AnimeWatchFragment,private val limit:Int,private val names : Array<String>,private val arr: Array<Int>,var selected:Int=0) : RecyclerView.Adapter<AnimeWatchChipAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemChipBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val chip =  ItemChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(chip)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chip = holder.binding.root
        if(position==selected){
            chip.isCheckable = true
            chip.isChecked = true
        }else{
            chip.isChecked=false
        }
        val last = if (position+1 == arr.size) names.size else (limit * (position+1))
        chip.text = "${names[limit * (position)]} - ${names[last-1]}"
        chip.setOnClickListener {
            activity.onChipClicked(position, limit * (position), last-1)
        }
    }

    override fun getItemCount(): Int = arr.size
}