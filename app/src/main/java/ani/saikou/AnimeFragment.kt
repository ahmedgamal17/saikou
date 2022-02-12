package ani.saikou

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import ani.saikou.anilist.Anilist
import ani.saikou.anilist.AnilistAnimeViewModel
import ani.saikou.databinding.FragmentAnimeBinding
import ani.saikou.media.MediaAdaptor
import ani.saikou.media.MediaLargeAdaptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AnimeFragment : Fragment() {
    private var _binding: FragmentAnimeBinding? = null
    private val binding get() = _binding!!
    private var trendHandler :Handler?=null
    private lateinit var trendRun : Runnable
    val model: AnilistAnimeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() { super.onDestroyView();_binding = null }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scope = viewLifecycleOwner.lifecycleScope
        binding.animeContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = statusBarHeight }

        binding.animeScroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, _, _, _ ->
            if(!v.canScrollVertically(1)) {
                binding.animePopularRecyclerView.requestDisallowInterceptTouchEvent(false)
                activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.bg)
                ObjectAnimator.ofFloat(bottomBar,"scaleX",0f).setDuration(200).start()
                ObjectAnimator.ofFloat(bottomBar,"scaleY",0f).setDuration(200).start()
            }
            if(!v.canScrollVertically(-1)){
                binding.animePopularRecyclerView.requestDisallowInterceptTouchEvent(true)
                activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.status)
                ObjectAnimator.ofFloat(bottomBar,"scaleX",1f).setDuration(200).start()
                ObjectAnimator.ofFloat(bottomBar,"scaleY",1f).setDuration(200).start()
            }
        })

        binding.animePopularRecyclerView.updateLayoutParams{ height=resources.displayMetrics.heightPixels+navBarHeight-80f.px }
        binding.animePopularProgress.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += navBarHeight }
        binding.animePopularRecyclerView.updatePaddingRelative(bottom = navBarHeight+80f.px)
        var height = statusBarHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = activity?.window?.decorView?.rootWindowInsets?.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size>0) {
                    height = max(statusBarHeight,
                        min(displayCutout.boundingRects[0].width(),displayCutout.boundingRects[0].height())
                    )
                }
            }
        }
        binding.animeRefresh.setSlingshotDistance(height+128)
        binding.animeRefresh.setProgressViewEndTarget(false, height+128)
        binding.animeRefresh.setOnRefreshListener {
            Refresh.activity[this.hashCode()]!!.postValue(true)
        }
        if(Anilist.avatar!=null){
            loadImage(Anilist.avatar,binding.animeUserAvatar)
            binding.animeUserAvatar.scaleType = ImageView.ScaleType.FIT_CENTER
        }

        binding.animeSearchBar.hint = "ANIME"
        binding.animeSearchBarText.setOnClickListener{
            ContextCompat.startActivity(
                requireActivity(),
                Intent(requireActivity(), SearchActivity::class.java).putExtra("type","ANIME"),
                ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(),
                    Pair.create(binding.animeSearchBar, ViewCompat.getTransitionName(binding.animeSearchBar)!!),
                ).toBundle()
            )
        }

        loadImage("https://bit.ly/31bsIHq",binding.animeGenreImage)
        loadImage( "https://bit.ly/2ZGfcuG",binding.animeTopScoreImage)

        binding.animeGenre.setOnClickListener {
            ContextCompat.startActivity(
                requireActivity(), Intent(requireActivity(), GenreActivity::class.java).putExtra("type","ANIME"),null)
        }

        binding.animeTopScore.setOnClickListener {
            ContextCompat.startActivity(
                requireActivity(), Intent(requireActivity(), SearchActivity::class.java).putExtra("type","ANIME").putExtra("sortBy","Score"),null)
        }

        model.getTrending().observe(viewLifecycleOwner) {
            if (it != null) {
                binding.animeTrendingProgressBar.visibility = View.GONE
                binding.animeTrendingViewPager.adapter =
                    MediaLargeAdaptor(it, requireActivity(), binding.animeTrendingViewPager)
                binding.animeTrendingViewPager.offscreenPageLimit = 3
                binding.animeTrendingViewPager.getChildAt(0).overScrollMode =
                    RecyclerView.OVER_SCROLL_NEVER

                val a = CompositePageTransformer()
                a.addTransformer(MarginPageTransformer(8f.px))
                a.addTransformer { page, position ->
                    page.scaleY = 0.85f + (1 - abs(position)) * 0.15f
                }
                binding.animeTrendingViewPager.setPageTransformer(a)
                trendHandler = Handler(Looper.getMainLooper())
                trendRun = Runnable {
                    if (_binding != null) binding.animeTrendingViewPager.currentItem =
                        binding.animeTrendingViewPager.currentItem + 1
                }
                binding.animeTrendingViewPager.registerOnPageChangeCallback(
                    object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            trendHandler!!.removeCallbacks(trendRun)
                            trendHandler!!.postDelayed(trendRun, 4000)
                        }
                    }
                )
            }
        }

        model.getUpdated().observe(viewLifecycleOwner) {
            if (it != null) {
                binding.animeUpdatedProgressBar.visibility = View.GONE
                binding.animeUpdatedRecyclerView.adapter = MediaAdaptor(it, requireActivity())
                binding.animeUpdatedRecyclerView.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                binding.animeUpdatedRecyclerView.visibility = View.VISIBLE
            }
        }

        model.getPopular().observe(viewLifecycleOwner) {
            if (it != null) {
                val adapter = MediaLargeAdaptor(it.results, requireActivity())
                var loading = false
                binding.animePopularRecyclerView.adapter = adapter
                binding.animePopularRecyclerView.layoutManager =
                    LinearLayoutManager(requireContext())
                binding.animePopularProgress.visibility = View.GONE
                if (it.hasNextPage) {
                    binding.animePopularRecyclerView.addOnScrollListener(object :
                        RecyclerView.OnScrollListener() {
                        override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
                            if (!v.canScrollVertically(1)) {
                                if (it.hasNextPage)
                                    if (!loading) {
                                        binding.animePopularProgress.visibility = View.VISIBLE
                                        scope.launch {
                                            loading = true
                                            val get = withContext(Dispatchers.IO){ model.loadNextPage(it) }
                                            if (get != null) {
                                                val a = it.results.size
                                                it.results.addAll(get.results)
                                                adapter.notifyItemRangeInserted(a, get.results.size)
                                                binding.animePopularProgress.visibility = View.GONE
                                                it.page = get.page
                                                it.hasNextPage = get.hasNextPage
                                                loading = false
                                            }
                                        }
                                    } else binding.animePopularProgress.visibility = View.GONE
                            }
                            if (!v.canScrollVertically(-1)) {
                                _binding?.animePopularRecyclerView?.post {
                                    val a = _binding
                                    a?.animePopularRecyclerView?.requestDisallowInterceptTouchEvent(
                                        true
                                    )
                                }
                                activity?.window?.statusBarColor =
                                    ContextCompat.getColor(requireContext(), R.color.status)
                                ObjectAnimator.ofFloat(bottomBar, "scaleX", 1f).setDuration(200)
                                    .start()
                                ObjectAnimator.ofFloat(bottomBar, "scaleY", 1f).setDuration(200)
                                    .start()
                            }
                            super.onScrolled(v, dx, dy)
                        }
                    })
                }
            }
        }

        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO){
                        model.loaded = true
                        model.loadTrending()
                        model.loadUpdated()
                        model.loadPopular("ANIME", sort = "POPULARITY_DESC")
                    }
                    live.postValue(false)
                    _binding?.animeRefresh?.isRefreshing = false
                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
        trendHandler?.removeCallbacks(trendRun)
    }

    override fun onResume() {
        if(!model.loaded) Refresh.activity[this.hashCode()]!!.postValue(true)
        super.onResume()
        trendHandler?.postDelayed(trendRun,4000)
    }
}