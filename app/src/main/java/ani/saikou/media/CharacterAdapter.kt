package ani.saikou.media

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.databinding.ItemCharacterBinding
import ani.saikou.loadImage
import ani.saikou.setAnimation
import java.io.Serializable

class CharacterAdapter(
    private val characterList: ArrayList<Character>,
    private val activity: Activity
): RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val binding = ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CharacterViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        val binding = holder.binding
        setAnimation(activity,holder.binding.root)
        val character = characterList[position]
        binding.itemCompactRelation.text = character.role+"  "
        loadImage(character.image,binding.itemCompactImage)
        binding.itemCompactTitle.text = character.name
    }

    override fun getItemCount(): Int = characterList.size
    inner class CharacterViewHolder(val binding: ItemCharacterBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val char = characterList[bindingAdapterPosition]
                ContextCompat.startActivity(
                    activity,
                    Intent(activity, CharacterDetailsActivity::class.java).putExtra("character",char as Serializable),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                        Pair.create(binding.itemCompactImage, ViewCompat.getTransitionName(binding.itemCompactImage)!!),
                    ).toBundle()
                )
            }
        }
    }
}