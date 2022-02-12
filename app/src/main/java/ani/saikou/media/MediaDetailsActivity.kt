package ani.saikou.media

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.anime.AnimeWatchFragment
import ani.saikou.anime.HWatchFragment
import ani.saikou.databinding.ActivityMediaBinding
import ani.saikou.manga.MangaSourceFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import kotlin.math.abs

class MediaDetailsActivity : AppCompatActivity(), AppBarLayout.OnOffsetChangedListener {

    private lateinit var binding: ActivityMediaBinding
    private val scope = lifecycleScope
    private val model: MediaDetailsViewModel by viewModels()
    private lateinit var tabLayout : BottomNavigationView
    var selected = 0
    var anime = true
    private var adult = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        screenWidth = resources.displayMetrics.widthPixels.toFloat()

        //Ui init
        initActivity(this)
        this.window.statusBarColor = ContextCompat.getColor(this, R.color.nav_status)

        binding.mediaBanner.updateLayoutParams{ height += statusBarHeight }
        binding.mediaBannerStatus.updateLayoutParams{ height += statusBarHeight }
        binding.mediaBanner.translationY = -statusBarHeight.toFloat()
        binding.mediaClose.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.mediaAppBar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.mediaCover.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.mediaTab.updatePadding(bottom = navBarHeight)

        binding.mediaTitle.isSelected = true
        binding.mediaTitleCollapse.isSelected = true
        binding.mediaUserStatus.isSelected = true
        binding.mediaAddToList.isSelected = true
        binding.mediaTotal.isSelected = true
        mMaxScrollSize = binding.mediaAppBar.totalScrollRange
        binding.mediaAppBar.addOnOffsetChangedListener(this)

        binding.mediaClose.setOnClickListener{
            onBackPressed()
        }
        val viewPager = binding.mediaViewPager
        tabLayout = binding.mediaTab
        viewPager.isUserInputEnabled = false
        viewPager.setPageTransformer(ZoomOutPageTransformer(true))

        var media: Media = intent.getSerializableExtra("media") as Media
        media.selected = model.loadSelected(media.id)
        loadImage(media.cover,binding.mediaCoverImage)
        binding.mediaCoverImage.setOnClickListener{ openImage(media.cover) }
        loadImage(media.banner?:media.cover,binding.mediaBanner)
//        binding.mediaBanner.setOnClickListener{ openImage(media.banner?:media.cover) }
        loadImage(media.banner?:media.cover,binding.mediaBannerStatus)
        binding.mediaTitle.text=media.userPreferredName
        binding.mediaTitleCollapse.text=media.userPreferredName

        //Fav Button
        if (media.isFav) binding.mediaFav.setImageDrawable(AppCompatResources.getDrawable(this,R.drawable.ic_round_favorite_24))
        val favButton = PopImageButton(scope,this,binding.mediaFav,media,R.drawable.ic_round_favorite_24,R.drawable.ic_round_favorite_border_24,R.color.nav_tab,R.color.fav,true)
        binding.mediaFav.setOnClickListener {
            favButton.clicked()
        }

        fun progress() {
            if (media.userStatus != null) {
                binding.mediaUserStatus.visibility = View.VISIBLE
                binding.mediaUserProgress.visibility = View.VISIBLE
                binding.mediaTotal.visibility = View.VISIBLE
                binding.mediaAddToList.setText(R.string.list_editor)

                binding.mediaUserStatus.text = media.userStatus
                binding.mediaUserProgress.text = (media.userProgress ?: "~").toString()
            } else {
                binding.mediaUserStatus.visibility = View.GONE
                binding.mediaUserProgress.visibility = View.GONE
                binding.mediaTotal.visibility = View.GONE
                binding.mediaAddToList.setText(R.string.add)
            }
            binding.mediaAddToList.setOnClickListener{
                if (Anilist.userid!=null)
                    MediaListDialogFragment().show(supportFragmentManager, "dialog")
                else toastString("Please Login with Anilist!")
            }
        }
        progress()

        //Share Button
        model.getMedia().observe(this) {
            if (it != null) {
                media = it
                if (it.notify) binding.mediaNotify.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_round_share_24))
                val notifyButton = PopImageButton(scope, this, binding.mediaNotify, it, R.drawable.ic_round_share_24, R.drawable.ic_round_share_24, R.color.nav_tab, R.color.violet_400, false)
                binding.mediaNotify.setOnClickListener { notifyButton.clicked() }
                progress()
            }
        }

        adult = media.isAdult

        tabLayout.menu.clear()
        if (media.anime!=null){
            binding.mediaTotal.text = if (media.anime!!.nextAiringEpisode!=null) " | "+(media.anime!!.nextAiringEpisode.toString()+" | "+(media.anime!!.totalEpisodes?:"~").toString()) else " | "+(media.anime!!.totalEpisodes?:"~").toString()
            viewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle,true,adult)
            tabLayout.inflateMenu(R.menu.anime_menu_detail)
        }
        else if (media.manga!=null){
            binding.mediaTotal.text = " | "+(media.manga!!.totalChapters?:"~").toString()
            viewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle,false,adult)
            tabLayout.inflateMenu(R.menu.manga_menu_detail)
            anime = false
        }


        selected = media.selected!!.window
        binding.mediaTitle.translationX = -screenWidth
        tabLayout.visibility = View.VISIBLE

        tabLayout.setOnItemSelectedListener { item ->
            val sel = loadData<Selected>(media.id.toString())?:media.selected!!
            selectFromID(item.itemId)
            viewPager.setCurrentItem(selected,false)
            sel.window = selected
            saveData(media.id.toString(),sel)
            true
        }


        tabLayout.selectedItemId = idFromSelect()
        viewPager.setCurrentItem(selected,false)

        if(model.continueMedia==null) model.continueMedia = media.cameFromContinue
        if(media.cameFromContinue) selected = 1
        scope.launch {
            withContext(Dispatchers.IO){ model.loadMedia(media) }
        }

        val live = Refresh.activity.getOrPut(this.hashCode()){ MutableLiveData(false) }
        live.observe(this){
            if(it){
                scope.launch {
                    withContext(Dispatchers.IO){ model.loadMedia(media) }
                    live.postValue(false)
                }
            }
        }
    }


    private fun selectFromID(id:Int){
        when(id) {
            R.id.info -> { selected = 0 }
            R.id.watch,R.id.read -> { selected = 1 }
        }
    }

    private fun idFromSelect():Int{
        if(anime) when(selected){
            0 -> return R.id.info
            1 -> return R.id.watch
        }
        else when(selected){
            0 -> return R.id.info
            1 -> return R.id.read
        }
        return R.id.info
    }

    override fun onResume() {
        tabLayout.selectedItemId = idFromSelect()
        binding.mediaBannerStatus.visibility=if (!isCollapsed) View.VISIBLE else View.GONE
        super.onResume()
    }
    //ViewPager
    private class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle,private val anime:Boolean,private val adult:Boolean) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            if (anime){
                when (position) {
                    0 -> return MediaInfoFragment()
                    1 -> return if(!adult) AnimeWatchFragment() else HWatchFragment()
                }
            }
            else{
                when (position) {
                    0 -> return MediaInfoFragment()
                    1 -> return MangaSourceFragment()
                }
            }
            return MediaInfoFragment()
        }
    }
    //Collapsing UI Stuff
    private var isCollapsed = false
    private val percent = 30
    private var mMaxScrollSize = 0
    private var screenWidth:Float = 0f

    override fun onOffsetChanged(appBar: AppBarLayout, i: Int) {
        if (mMaxScrollSize == 0) mMaxScrollSize = appBar.totalScrollRange
        val percentage = abs(i) * 100 / mMaxScrollSize
        val cap = MathUtils.clamp((percent - percentage) / percent.toFloat(), 0f, 1f)

        binding.mediaCover.scaleX = 1f*cap
        binding.mediaCover.scaleY = 1f*cap
        binding.mediaCover.cardElevation = 32f*cap

        binding.mediaCover.visibility= if(binding.mediaCover.scaleX==0f) View.GONE else View.VISIBLE

        if (percentage >= percent && !isCollapsed) {
            isCollapsed = true
            ObjectAnimator.ofFloat(binding.mediaTitle,"translationX",0f).setDuration(200).start()
            ObjectAnimator.ofFloat(binding.mediaAccessContainer,"translationX",screenWidth).setDuration(200).start()
            ObjectAnimator.ofFloat(binding.mediaTitleCollapse,"translationX",screenWidth).setDuration(200).start()
            binding.mediaBannerStatus.visibility=View.GONE
            this.window.statusBarColor = ContextCompat.getColor(this, R.color.nav_bg)
        }
        if (percentage <= percent && isCollapsed) {
            isCollapsed = false
            ObjectAnimator.ofFloat(binding.mediaTitle,"translationX",-screenWidth).setDuration(200).start()
            ObjectAnimator.ofFloat(binding.mediaAccessContainer,"translationX",0f).setDuration(200).start()
            ObjectAnimator.ofFloat(binding.mediaTitleCollapse,"translationX",0f).setDuration(200).start()
            binding.mediaBannerStatus.visibility=View.VISIBLE
            this.window.statusBarColor = ContextCompat.getColor(this, R.color.nav_status)
        }
    }
    inner class PopImageButton(private val scope: CoroutineScope,private val activity: Activity,private val image:ImageView,private val media:Media,private val d1:Int,private val d2:Int,private val c1:Int,private val c2:Int,private val fav_or_not:Boolean? = null){
        private var pressable = true
        private var clicked = false
        fun clicked(){
            if (pressable){
                pressable = false
                if (fav_or_not!=null) {
                    if (fav_or_not) {
                        media.isFav = !media.isFav
                        clicked = media.isFav
                        scope.launch(Dispatchers.IO) { Anilist.mutation.toggleFav(media.anime!=null,media.id) }
                    }
                    else {
                        media.notify = !media.notify
                        clicked = media.notify
                        val i = Intent(Intent.ACTION_SEND)
                        i.type = "text/plain"
                        i.putExtra(Intent.EXTRA_TEXT, media.shareLink)
                        startActivity(Intent.createChooser(i, media.userPreferredName))
                    }
                }
                else clicked = !clicked
                ObjectAnimator.ofFloat(image,"scaleX",1f,0f).setDuration(69).start()
                ObjectAnimator.ofFloat(image,"scaleY",1f,0f).setDuration(100).start()
                scope.launch {
                    delay(100)
                    if (clicked) {
                        ObjectAnimator.ofArgb(image,"ColorFilter",ContextCompat.getColor(activity, c1),ContextCompat.getColor(activity, c2)).setDuration(120).start()
                        image.setImageDrawable(AppCompatResources.getDrawable(activity,d1))
                    }
                    else image.setImageDrawable(AppCompatResources.getDrawable(activity,d2))
                    ObjectAnimator.ofFloat(image,"scaleX",0f,1.5f).setDuration(120).start()
                    ObjectAnimator.ofFloat(image,"scaleY",0f,1.5f).setDuration(100).start()
                    delay(120)
                    ObjectAnimator.ofFloat(image,"scaleX",1.5f,1f).setDuration(100).start()
                    ObjectAnimator.ofFloat(image,"scaleY",1.5f,1f).setDuration(100).start()
                    delay(200)
                    if (clicked) ObjectAnimator.ofArgb(image,"ColorFilter", ContextCompat.getColor(activity, c2), ContextCompat.getColor(activity, c1)).setDuration(200).start()
                    pressable = true
                }
            }
        }
    }
}

