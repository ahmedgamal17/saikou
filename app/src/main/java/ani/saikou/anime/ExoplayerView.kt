
package ani.saikou.anime

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.hardware.SensorManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings.System
import android.util.TypedValue
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.math.MathUtils.clamp
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.anime.source.AnimeSources
import ani.saikou.anime.source.HSources
import ani.saikou.databinding.ActivityExoplayerBinding
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.slider.Slider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.roundToInt

class ExoplayerView : AppCompatActivity(), Player.Listener {
    private lateinit var binding : ActivityExoplayerBinding
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var cacheFactory : CacheDataSource.Factory
    private lateinit var playbackParameters: PlaybackParameters
    private lateinit var mediaItem : MediaItem

    private lateinit var playerView: PlayerView
    private lateinit var exoSource: ImageButton
    private lateinit var exoRotate: ImageButton
    private lateinit var exoQuality: ImageButton
    private lateinit var exoSpeed: ImageButton
    private lateinit var exoScreen: ImageButton
    private lateinit var exoBrightness: Slider
    private lateinit var exoVolume: Slider
    private lateinit var exoBrightnessCont: View
    private lateinit var exoVolumeCont: View
    private lateinit var animeTitle : TextView
    private lateinit var episodeTitle : TextView
    private var orientationListener : OrientationEventListener? =null

    private lateinit var media: Media
    private lateinit var episodeArr: List<String>
    private var currentEpisodeIndex = 0
    private var epChanging = false
    private var progressDialog : AlertDialog.Builder?=null
    private var dontAskProgressDialog = false

    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var episodeLength: Float = 0f
    private var isFullscreen : Int = 0
    private var isInitialized = false
    private var isPlayerPlaying = true
    private var changingServer = false

    val handler = Handler(Looper.getMainLooper())
    private val model: MediaDetailsViewModel by viewModels()

    override fun onDestroy() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")

    override fun onAttachedToWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout =window.decorView.rootWindowInsets.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size>0) {
                    val notchHeight = min(displayCutout.boundingRects[0].width(),displayCutout.boundingRects[0].height())
                    exoBrightnessCont.updateLayoutParams<ViewGroup.MarginLayoutParams> { marginEnd +=notchHeight }
                    exoVolumeCont.updateLayoutParams<ViewGroup.MarginLayoutParams> { marginStart +=notchHeight }
                }
            }
        }
        super.onAttachedToWindow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExoplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialize
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        playerView = findViewById(R.id.player_view)
        exoQuality = playerView.findViewById(R.id.exo_quality)
        exoSource = playerView.findViewById(R.id.exo_source)
        exoRotate = playerView.findViewById(R.id.exo_rotate)
        exoSpeed = playerView.findViewById(R.id.exo_playback_speed)
        exoScreen = playerView.findViewById(R.id.exo_screen)
        exoBrightness = playerView.findViewById(R.id.exo_brightness)
        exoVolume = playerView.findViewById(R.id.exo_volume)
        exoBrightnessCont = playerView.findViewById(R.id.exo_brightness_cont)
        exoVolumeCont = playerView.findViewById(R.id.exo_volume_cont)
        animeTitle = playerView.findViewById(R.id.exo_anime_title)
        episodeTitle = playerView.findViewById(R.id.exo_ep_title)

        playerView.controllerShowTimeoutMs = 5000
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (System.getInt(contentResolver, System.ACCELEROMETER_ROTATION, 0) != 1) {
            var rotation = 0
            requestedOrientation = rotation
            exoRotate.setOnClickListener {
                requestedOrientation = rotation
                it.visibility = View.GONE
            }
            orientationListener =
                object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
                    override fun onOrientationChanged(orientation: Int) {
                        if (orientation in 45..135) {
                            if(rotation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) exoRotate.visibility = View.VISIBLE
                            rotation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        } else if (orientation in 225..315) {
                            if(rotation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) exoRotate.visibility = View.VISIBLE
                            rotation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    }
                }
            orientationListener?.enable()
        }

        playerView.subtitleView?.setStyle(CaptionStyleCompat(Color.WHITE,Color.TRANSPARENT,Color.TRANSPARENT,EDGE_TYPE_OUTLINE,Color.BLACK,
            ResourcesCompat.getFont(this, R.font.poppins_bold)))
        playerView.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

        if (savedInstanceState != null) {
            currentWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW)
            playbackPosition = savedInstanceState.getLong(STATE_RESUME_POSITION)
            isFullscreen = savedInstanceState.getInt(STATE_PLAYER_FULLSCREEN)
            isPlayerPlaying = savedInstanceState.getBoolean(STATE_PLAYER_PLAYING)
        }

        //BackButton
        playerView.findViewById<ImageButton>(R.id.exo_back).setOnClickListener{
            onBackPressed()
        }

        //SliderLock
        var sliderLocked = loadData<Boolean>("sliderLock",this)?:false
        playerView.findViewById<ImageButton>(R.id.exo_slider_lock).setImageDrawable(AppCompatResources.getDrawable(this,if(sliderLocked) R.drawable.ic_round_piano_off_24 else R.drawable.ic_round_piano_24))
        playerView.findViewById<ImageButton>(R.id.exo_slider_lock).setOnClickListener {
            sliderLocked = if(sliderLocked){
                (it as ImageButton).setImageDrawable(AppCompatResources.getDrawable(this,R.drawable.ic_round_piano_24))
                toastString("Turned On Volume & Brightness gestures.")
                false
            } else{
                (it as ImageButton).setImageDrawable(AppCompatResources.getDrawable(this,R.drawable.ic_round_piano_off_24))
                toastString("Turned Off Volume & Brightness gestures.")
                true
            }
            saveData("sliderLock",sliderLocked,this)
        }

        //LockButton
        var prevSliderLocked = sliderLocked
        var locked = false
        val container = playerView.findViewById<View>(R.id.exo_controller_cont)
        val lockButton = playerView.findViewById<ImageButton>(R.id.exo_unlock)
        playerView.findViewById<ImageButton>(R.id.exo_lock).setOnClickListener{
            prevSliderLocked = sliderLocked
            sliderLocked = true
            locked = true
            container.visibility = View.GONE
            lockButton.visibility = View.VISIBLE
        }
        lockButton.setOnClickListener {
            sliderLocked = prevSliderLocked
            locked = false
            container.visibility = View.VISIBLE
            it.visibility = View.GONE
        }

        //+85 Button
        playerView.findViewById<View>(R.id.exo_skip).setOnClickListener {
            exoPlayer.seekTo(exoPlayer.currentPosition + 85000)
        }

        //Player UI Visibility Handler
        val brightnessRunnable = Runnable {
            if(exoBrightnessCont.alpha==1f)
                lifecycleScope.launch {
                    ObjectAnimator.ofFloat(exoBrightnessCont, "alpha", 1f, 0f).setDuration(300).start()
                    delay(300)
                    exoBrightnessCont.visibility = View.GONE
                }
        }
        val volumeRunnable = Runnable {
            if(exoVolumeCont.alpha==1f)
                lifecycleScope.launch {
                    ObjectAnimator.ofFloat(exoVolumeCont, "alpha", 1f, 0f).setDuration(300).start()
                    delay(300)
                    exoVolumeCont.visibility = View.GONE
                }
        }
        playerView.setControllerVisibilityListener {
            if(it==View.GONE) {
                hideSystemBars()
                brightnessRunnable.run()
                volumeRunnable.run()
            }
        }
        fun handleController(){
            if(playerView.isControllerVisible){
                playerView.hideController()
            }else{
                playerView.showController()
                ObjectAnimator.ofFloat(playerView.findViewById(R.id.exo_controller),"alpha",0f,1f).setDuration(200).start()
            }
        }

        //Brightness
        var brightnessTimer = Timer()
        exoBrightnessCont.visibility = View.GONE

        fun brightnessHide(){
            brightnessTimer.cancel()
            brightnessTimer.purge()
            val timerTask: TimerTask = object : TimerTask() {
                override fun run() {
                    handler.post(brightnessRunnable)
                }
            }
            brightnessTimer = Timer()
            brightnessTimer.schedule(timerTask, 3000)
        }
        exoBrightness.value = clamp(System.getInt(contentResolver, System.SCREEN_BRIGHTNESS,127)/255f*10f,0f,10f)
        exoBrightness.addOnChangeListener { _, value, _ ->
            val lp = window.attributes
            lp.screenBrightness = value / 10f
            window.attributes = lp
            brightnessHide()
        }

        //FastRewind (Left Panel)
        val fastRewindCard = playerView.findViewById<View>(R.id.exo_fast_rewind)
        val fastRewindDetector = GestureDetector(this, object : DoubleClickListener() {
            override fun onDoubleClick(event: MotionEvent?) {
                if(!locked) {
                    exoPlayer.seekTo(exoPlayer.currentPosition - 10000)
                    viewDoubleTapped(fastRewindCard,event,playerView.findViewById(R.id.exo_fast_rewind_anim))
                }
            }

            override fun onScrollYClick(y: Float) {
                if(!sliderLocked) {
                    exoBrightness.value = clamp(exoBrightness.value + y / 50, 0f, 10f)
                    if(exoBrightnessCont.visibility != View.VISIBLE){
                        exoBrightnessCont.visibility = View.VISIBLE
                        exoBrightnessCont.alpha = 1f
                    }
                }
            }
            override fun onSingleClick(event: MotionEvent?) = handleController()
        })
        playerView.findViewById<View>(R.id.exo_rewind_area).setOnTouchListener { v, event ->
            fastRewindDetector.onTouchEvent(event)
            v.performClick()
            true
        }

        //Volume
        var volumeTimer = Timer()
        exoVolumeCont.visibility = View.GONE

        val volumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        exoVolume.value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()/volumeMax*10
        fun volumeHide(){
            volumeTimer.cancel()
            volumeTimer.purge()
            val timerTask: TimerTask = object : TimerTask() {
                override fun run() {
                    handler.post(volumeRunnable)
                }
            }
            volumeTimer = Timer()
            volumeTimer.schedule(timerTask, 3000)
        }
        exoVolume.addOnChangeListener { _, value, _ ->
            val volume = (value/10*volumeMax).roundToInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,0)
            volumeHide()
        }

        //FastForward (Right Panel)
        val fastForwardCard = playerView.findViewById<View>(R.id.exo_fast_forward)
        val fastForwardDetector = GestureDetector(this, object : DoubleClickListener() {
            override fun onDoubleClick(event: MotionEvent?) {
                if(!locked) {
                    exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                    viewDoubleTapped(fastForwardCard,event,playerView.findViewById(R.id.exo_fast_forward_anim))
                }
            }
            override fun onScrollYClick(y: Float) {
                if(!sliderLocked) {
                    exoVolume.value = clamp(exoVolume.value+y/50,0f,10f)
                    if(exoVolumeCont.visibility != View.VISIBLE){
                        exoVolumeCont.visibility = View.VISIBLE
                        exoVolumeCont.alpha = 1f
                    }
                }
            }

            override fun onSingleClick(event: MotionEvent?) = handleController()
        })
        playerView.findViewById<View>(R.id.exo_forward_area).setOnTouchListener { v, event ->
            fastForwardDetector.onTouchEvent(event)
            v.performClick()
            true
        }

        //Handle Media
        media = intent.getSerializableExtra("media")!! as Media
        model.setMedia(media)

        model.watchSources = if(media.isAdult) HSources else AnimeSources

        model.epChanged.observe(this) {
            epChanging = !it
        }
        val episodeObserverRunnable = Runnable {
            model.getEpisode().observe(this) {
                hideSystemBars()
                if (it != null && !epChanging) {
                    media.selected = model.loadSelected(media.id)
                    model.setMedia(media)
                    currentEpisodeIndex = episodeArr.indexOf(it.number)
                    if (isInitialized) releasePlayer()
                    playbackPosition = loadData("${media.id}_${it.number}", this) ?: 0
                    initPlayer(it)
                }
            }
        }
        episodeObserverRunnable.run()
        //Anime Title
        animeTitle.text = media.userPreferredName

        //Set Episode, to invoke getEpisode() at Start
        model.setEpisode(media.anime!!.episodes!![media.anime!!.selectedEpisode!!]!!)

        episodeArr = media.anime!!.episodes!!.keys.toList()
        currentEpisodeIndex = episodeArr.indexOf(media.anime!!.selectedEpisode!!)

        //Next Episode
        fun change(index:Int){
            changingServer = false
            saveData("${media.id}_${media.anime!!.selectedEpisode}",exoPlayer.currentPosition,this)
            media.anime!!.selectedEpisode = episodeArr[index]
            model.setMedia(media)
            model.epChanged.postValue(false)
            model.setEpisode(media.anime!!.episodes!![media.anime!!.selectedEpisode!!]!!)
            model.onEpisodeClick(media, media.anime!!.selectedEpisode!!,this.supportFragmentManager,
                launch = false,
                cancellable = false
            )
        }
        playerView.findViewById<ImageButton>(R.id.exo_next_ep).setOnClickListener {
            if(episodeArr.size>currentEpisodeIndex+1 && isInitialized) {
                if(exoPlayer.currentPosition/episodeLength>0.8f && Anilist.userid!=null) {
                    if(progressDialog!=null) {
                        progressDialog?.setCancelable(false)
                            ?.setPositiveButton("Yes") { dialog, _ ->
                                updateAnilistProgress(media.id,media.anime!!.selectedEpisode!!)
                                dialog.dismiss()
                                change(currentEpisodeIndex + 1)
                            }
                            ?.setNegativeButton("No") { dialog, _ ->
                                dialog.dismiss()
                                change(currentEpisodeIndex + 1)
                            }
                        progressDialog?.show()
                    }else{
                        updateAnilistProgress(media.id,media.anime!!.selectedEpisode!!)
                        change(currentEpisodeIndex + 1)
                    }
                } else change(currentEpisodeIndex + 1)
            }
            else
                toastString("No next Episode Found!")
        }
        //Prev Episode
        playerView.findViewById<ImageButton>(R.id.exo_prev_ep).setOnClickListener {
            if(currentEpisodeIndex>0) {
                change(currentEpisodeIndex - 1)
            }
            else
                toastString("This is the 1st Episode!")
        }

        //FullScreen
        isFullscreen = loadData("${media.id}_fullscreenInt",this)?:isFullscreen
        playerView.resizeMode = when(isFullscreen) {
            0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
            exoScreen.setOnClickListener {
                if(isFullscreen<2) isFullscreen += 1 else isFullscreen = 0
                playerView.resizeMode = when(isFullscreen) {
                    0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                toastString(when(isFullscreen) {
                    0 -> "Original"
                    1 -> "Zoom"
                    2 -> "Stretch"
                    else -> "Original"
                })
                saveData("${media.id}_fullscreenInt",isFullscreen,this)
        }

        //Speed
        val speeds     = arrayOf( 0.25f , 0.33f , 0.5f , 0.66f , 0.75f , 1f , 1.25f , 1.33f , 1.5f , 1.66f , 1.75f , 2f )
        val speedsName = speeds.map { "${it}x" }.toTypedArray()
        var curSpeed   = loadData("${media.id}_speed",this)?:5

        playbackParameters = PlaybackParameters(speeds[curSpeed])
        var speed: Float
        val speedDialog = AlertDialog.Builder(this,R.style.DialogTheme).setTitle("Speed")
        exoSpeed.setOnClickListener{
            speedDialog.setSingleChoiceItems(speedsName,curSpeed) { dialog, i ->
                speed = speeds[i]
                curSpeed = i
                playbackParameters = PlaybackParameters(speed)
                exoPlayer.playbackParameters = playbackParameters
                dialog.dismiss()
                hideSystemBars()
            }.show()
        }
        speedDialog.setOnCancelListener { hideSystemBars() }

        dontAskProgressDialog = loadData<Boolean>("${media.id}_progressDialog") != true
        progressDialog = if(dontAskProgressDialog && Anilist.userid!=null) AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Update progress on anilist?").apply {
            setMultiChoiceItems(arrayOf("Don't ask again"), booleanArrayOf(false)) { _, _, isChecked ->
                if (isChecked) saveData("${media.id}_progressDialog", isChecked)
                dontAskProgressDialog = isChecked
            }
            setOnCancelListener { hideSystemBars() }
        } else null
    }

    @SuppressLint("SetTextI18n")
    private fun initPlayer(episode: Episode){
        //Title
        episodeTitle.text = "Episode ${episode.number}${if(episode.title!="" && episode.title!=null && episode.title!="null") " : "+episode.title else ""}${if(episode.filler) "\n[Filler]" else ""}"
        episodeTitle.isSelected = true
        saveData("${media.id}_current_ep",media.anime!!.selectedEpisode!!,this)

        val set = loadData<MutableSet<Int>>("continue_ANIME",this)?: mutableSetOf()
        if(set.contains(media.id)) set.remove(media.id)
        set.add(media.id)
        saveData("continue_ANIME",set,this)

        val stream = episode.streamLinks[episode.selectedStream]?: return

        val simpleCache = VideoCache.getInstance(this)
        val dataSourceFactory = DataSource.Factory {
            val dataSource: HttpDataSource = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true).createDataSource()
            dataSource.setRequestProperty("User-Agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36")
            if(stream.headers!=null) {
                stream.headers.forEach{
                    dataSource.setRequestProperty(it.key,it.value)
                }
            }
            dataSource
        }
        cacheFactory = CacheDataSource.Factory().apply {
            setCache(simpleCache)
            setUpstreamDataSourceFactory(dataSourceFactory)
        }

        //Subtitles
        val a = stream.subtitles
        val subtitle: MediaItem.SubtitleConfiguration? = if (a!=null && a.contains("English"))
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(a["English"]))
                .setMimeType(MimeTypes.TEXT_VTT).setSelectionFlags(C.SELECTION_FLAG_FORCED)
                .build()
        else null

        val url = if(episode.selectedQuality<stream.quality.size) stream.quality[episode.selectedQuality] else return
        val but = playerView.findViewById<ImageButton>(R.id.exo_download)
        if(url.quality!="Multi Quality") {
            but.visibility = View.VISIBLE
            but.setOnClickListener {
                download(this,episode,animeTitle.text.toString())
            }
        }else but.visibility = View.GONE

        val builder =  MediaItem.Builder().setUri(url.url)
        if(subtitle!=null) builder.setSubtitleConfigurations(mutableListOf(subtitle))
        mediaItem = builder.build()

        //Source
        exoSource.setOnClickListener {
            changingServer = true
            media.selected!!.stream = null
            saveData("${media.id}_${media.anime!!.selectedEpisode}", exoPlayer.currentPosition, this)
            model.saveSelected(media.id,media.selected!!,this)
            model.onEpisodeClick(media,episode.number,this.supportFragmentManager,
                launch = false,
                cancellable = true
            )
        }

        //Quality Track
        trackSelector = DefaultTrackSelector(this)
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
            .setMinVideoSize(loadData("maxWidth",this)?:720, loadData("maxHeight",this)?:480)
            .setMaxVideoSize(1,1)
        )

        if(playbackPosition!=0L && !changingServer) {
            val time = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(playbackPosition),
                TimeUnit.MILLISECONDS.toMinutes(playbackPosition) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(playbackPosition)),
                TimeUnit.MILLISECONDS.toSeconds(playbackPosition) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(playbackPosition)))
            AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Continue from ${time}?").apply {
                setCancelable(false)
                setPositiveButton("Yes"){d, _ ->
                    buildExoplayer()
                    d.dismiss()
                }
                setNegativeButton("No"){d,_ ->
                    playbackPosition=0L
                    buildExoplayer()
                    d.dismiss()
                }
            }.show()
        }
        else buildExoplayer()
    }

    private fun buildExoplayer(){
        //Player
        hideSystemBars()
        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .setTrackSelector(trackSelector)
            .build().apply {
                playWhenReady = isPlayerPlaying
                this.playbackParameters = this@ExoplayerView.playbackParameters
                setMediaItem(mediaItem)
                prepare()
                seekTo(playbackPosition)
            }
        playerView.player = exoPlayer
        exoPlayer.addListener(this)
        isInitialized = true
    }

    private fun releasePlayer(){
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        exoPlayer.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if(isInitialized) {
            outState.putInt(STATE_RESUME_WINDOW, exoPlayer.currentMediaItemIndex)
            outState.putLong(STATE_RESUME_POSITION, exoPlayer.currentPosition)
        }
        outState.putInt(STATE_PLAYER_FULLSCREEN, isFullscreen)
        outState.putBoolean(STATE_PLAYER_PLAYING, isPlayerPlaying)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        orientationListener?.disable()
        if(isInitialized) {
            playerView.player?.pause()
            saveData("${media.id}_${media.anime!!.selectedEpisode}", exoPlayer.currentPosition, this)
        }
    }

    override fun onResume() {
        super.onResume()
        orientationListener?.enable()
        hideSystemBars()
        if(isInitialized) {
            playerView.onResume()
            playerView.useController = true
        }
    }

    override fun onStop() {
        playerView.player?.pause()
        super.onStop()
    }

    private var wasPlaying = false
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if(isInitialized && !hasFocus) wasPlaying = exoPlayer.isPlaying
        if (hasFocus) {
            if(isInitialized && wasPlaying) exoPlayer.play()
        } else {
            if(isInitialized) exoPlayer.pause()
        }
        super.onWindowFocusChanged(hasFocus)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        playerView.keepScreenOn = isPlaying
    }

    @SuppressLint("SetTextI18n")
    override fun onRenderedFirstFrame() {
        super.onRenderedFirstFrame()
        saveData("${media.id}_${media.anime!!.selectedEpisode}_max",exoPlayer.duration,this)
        val height = (exoPlayer.videoFormat?:return).height
        val width = (exoPlayer.videoFormat?:return).width
        saveData("maxHeight",height)
        saveData("maxWidth",width)
        playerView.findViewById<TextView>(R.id.exo_video_details).text = "$width x $height"
    }

    override fun onTracksInfoChanged(tracksInfo: TracksInfo) {
        if(tracksInfo.trackGroupInfos.size<=2) exoQuality.visibility = View.GONE
        else {
            exoQuality.visibility = View.VISIBLE
            exoQuality.setOnClickListener {
                initPopupQuality(trackSelector)?.show()
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == ExoPlayer.STATE_READY && episodeLength==0f) {
            episodeLength = exoPlayer.duration.toFloat()
        }
        super.onPlaybackStateChanged(playbackState)
    }

    override fun onBackPressed() {
        if(isInitialized) {
            if (exoPlayer.currentPosition / episodeLength > 0.8f && Anilist.userid != null) {
                if (dontAskProgressDialog) {
                    progressDialog?.setCancelable(false)
                        ?.setPositiveButton("Yes") { dialog, _ ->
                            saveData("${media.id}_save_progress",true)
                            updateAnilistProgress(media.id, media.anime!!.selectedEpisode!!)
                            dialog.dismiss()
                            super.onBackPressed()
                        }
                        ?.setNegativeButton("No") { dialog, _ ->
                            saveData("${media.id}_save_progress",false)
                            dialog.dismiss()
                            super.onBackPressed()
                        }
                    progressDialog?.show()
                } else {
                    if(loadData<Boolean>("${media.id}_save_progress")==true)
                        updateAnilistProgress(media.id, media.anime!!.selectedEpisode!!)
                    super.onBackPressed()
                }
            } else {
                super.onBackPressed()
            }
        }
        else{
            super.onBackPressed()
        }
    }

    private fun hideSystemBars() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
               View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }

    // QUALITY SELECTOR
    private fun initPopupQuality(trackSelector:DefaultTrackSelector):Dialog? {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo?:return null
        var videoRenderer : Int? = null

        fun isVideoRenderer(mappedTrackInfo: MappingTrackSelector.MappedTrackInfo, rendererIndex: Int): Boolean {
            if (mappedTrackInfo.getTrackGroups(rendererIndex).length == 0) return false
            return C.TRACK_TYPE_VIDEO == mappedTrackInfo.getRendererType(rendererIndex)
        }

        for(i in 0 until mappedTrackInfo.rendererCount)
            if(isVideoRenderer(mappedTrackInfo, i))
                videoRenderer = i

        val trackSelectionDialogBuilder = TrackSelectionDialogBuilder(this, "Available Qualities", trackSelector, videoRenderer?:return null)
        trackSelectionDialogBuilder.setTheme(R.style.DialogTheme)
        trackSelectionDialogBuilder.setTrackNameProvider{
            if(it.frameRate>0f) it.height.toString()+"p" else it.height.toString()+"p (fps : N/A)"
        }
        val trackDialog = trackSelectionDialogBuilder.build()
        trackDialog.setOnDismissListener { hideSystemBars() }
        return trackDialog
    }

    //Double Tap Animation
    private var t1=Timer()
    private var t2=Timer()
    private fun hideLayer(v:View,text:View){
        val timerTask = object : TimerTask() {
            override fun run() {
                handler.post {
                    ObjectAnimator.ofFloat(v, "alpha", 1f, 0f).setDuration(150).start()
                    ObjectAnimator.ofFloat(text, "alpha", 1f, 0f).setDuration(150).start()
                }
            }
        }
        if(v.id==R.id.exo_fast_forward) {
            t1.cancel()
            t1.purge()
            t1 = Timer()
            t1.schedule(timerTask, 450)
        }
        else {
            t2.cancel()
            t2.purge()
            t2 = Timer()
            t2.schedule(timerTask, 450)
        }
    }
    private fun viewDoubleTapped(v:View,event:MotionEvent?,text:TextView){
        playerView.hideController()
        if(event!=null) v.circularReveal(event.x.toInt(), event.y.toInt(), 300)

        ObjectAnimator.ofFloat(v,"alpha",1f,1f).setDuration(600).start()
        ObjectAnimator.ofFloat(v,"alpha",0f,1f).setDuration(300).start()
        ObjectAnimator.ofFloat(text,"alpha",1f,1f).setDuration(600).start()
        ObjectAnimator.ofFloat(text,"alpha",0f,1f).setDuration(150).start()

        val a = (text.compoundDrawables[1] as Animatable)
        if(!a.isRunning) a.start()
        v.postDelayed({
            hideLayer(v,text)
        },450)
    }
}