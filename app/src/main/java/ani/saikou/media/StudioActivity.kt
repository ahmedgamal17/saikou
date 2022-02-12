package ani.saikou.media

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.saikou.*
import ani.saikou.databinding.ActivityStudioBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudioBinding
    private val scope = lifecycleScope
    private val model: OtherDetailsViewModel by viewModels()
    private lateinit var studio: Studio
    private var loaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)
        this.window.statusBarColor = ContextCompat.getColor(this, R.color.nav_bg)

        binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.studioRecycler.updatePadding(bottom = 64f.px + navBarHeight)
        binding.studioTitle.isSelected = true

        studio = intent.getSerializableExtra("studio") as Studio
        binding.studioTitle.text = studio.name

        binding.studioClose.setOnClickListener{
            onBackPressed()
        }

        model.getStudio().observe(this) {
            if (it != null) {
                studio = it
                loaded = true
                binding.studioProgressBar.visibility = View.GONE
                binding.studioRecycler.visibility = View.VISIBLE
                binding.studioRecycler.adapter = MediasWithTitleAdapter(studio.yearMedia!!, this)
                binding.studioRecycler.layoutManager = LinearLayoutManager(this)
            }
        }
        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(this) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO){ model.loadStudio(studio) }
                    live.postValue(false)
                }
            }
        }
    }

    override fun onDestroy() {
        if(Refresh.activity.containsKey(this.hashCode())){
            Refresh.activity.remove(this.hashCode())
        }
        super.onDestroy()
    }

    override fun onResume() {
        binding.studioProgressBar.visibility = if (!loaded) View.VISIBLE else View.GONE
        super.onResume()
    }
}