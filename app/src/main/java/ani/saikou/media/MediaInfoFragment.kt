package ani.saikou.media

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ani.saikou.R
import ani.saikou.databinding.FragmentMediaInfoBinding
import ani.saikou.navBarHeight
import ani.saikou.px
import java.io.Serializable

@SuppressLint("SetTextI18n")
class MediaInfoFragment : Fragment() {
    private var _binding: FragmentMediaInfoBinding? = null
    private val binding get() = _binding!!
    private var timer: CountDownTimer? = null
    private var loaded = false
    private var type = "ANIME"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMediaInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val screenWidth = resources.displayMetrics.run { widthPixels / density }
        binding.mediaInfoProgressBar.visibility = if (!loaded) View.VISIBLE else View.GONE
        binding.mediaInfoContainer.visibility = if (loaded) View.VISIBLE else View.GONE
        binding.mediaInfoContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += 128f.px + navBarHeight }

        val model : MediaDetailsViewModel by activityViewModels()
        model.getMedia().observe(viewLifecycleOwner) {
            val media = it
            if (media != null) {
                loaded = true
                binding.mediaInfoProgressBar.visibility = View.GONE
                binding.mediaInfoContainer.visibility = View.VISIBLE
                binding.mediaInfoName.text = media.getMainName()
                if (media.name != "null")
                    binding.mediaInfoNameRomajiContainer.visibility = View.VISIBLE
                binding.mediaInfoNameRomaji.text = media.nameRomaji
                binding.mediaInfoMeanScore.text =
                    if (media.meanScore != null) (media.meanScore / 10.0).toString() else "??"
                binding.mediaInfoStatus.text = media.status
                binding.mediaInfoFormat.text = media.format
                binding.mediaInfoSource.text = media.source
                binding.mediaInfoStart.text =
                    if (media.startDate.toString() != "") media.startDate.toString() else "??"
                binding.mediaInfoEnd.text =
                    if (media.endDate.toString() != "") media.endDate.toString() else "??"
                if (media.anime != null) {
                    binding.mediaInfoDuration.text =
                        if (media.anime.episodeDuration != null) media.anime.episodeDuration.toString() else "??"
                    binding.mediaInfoDurationContainer.visibility = View.VISIBLE
                    binding.mediaInfoSeasonContainer.visibility = View.VISIBLE
                    binding.mediaInfoSeason.text =
                        media.anime.season ?: "??" + " " + media.anime.seasonYear
                    if (media.anime.mainStudio != null) {
                        binding.mediaInfoStudioContainer.visibility = View.VISIBLE
                        binding.mediaInfoStudio.text = media.anime.mainStudio!!.name
                        binding.mediaInfoStudioContainer.setOnClickListener {
                            ContextCompat.startActivity(
                                requireActivity(),
                                Intent(activity, StudioActivity::class.java).putExtra(
                                    "studio",
                                    media.anime.mainStudio!! as Serializable
                                ),
                                null
                            )
                        }
                    }
                    binding.mediaInfoTotalTitle.setText(R.string.total_eps)
                    binding.mediaInfoTotal.text =
                        if (media.anime.nextAiringEpisode != null) (media.anime.nextAiringEpisode.toString() + " | " + (media.anime.totalEpisodes
                            ?: "~").toString()) else (media.anime.totalEpisodes ?: "~").toString()
                } else if (media.manga != null) {
                    type = "MANGA"
                    binding.mediaInfoTotalTitle.setText(R.string.total_chaps)
                    binding.mediaInfoTotal.text = (media.manga.totalChapters ?: "~").toString()
                }
                val desc = HtmlCompat.fromHtml(
                    (media.description ?: "null").replace("\\n", "<br>").replace("\\\"", "\""),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
                binding.mediaInfoDescription.text =
                    "\t\t\t" + if (desc.toString() != "null") desc else "No Description Available"
                binding.mediaInfoDescription.setOnClickListener {
                    if (binding.mediaInfoDescription.maxLines == 5) {
                        ObjectAnimator.ofInt(binding.mediaInfoDescription, "maxLines", 100)
                            .setDuration(950).start()
                    } else {
                        ObjectAnimator.ofInt(binding.mediaInfoDescription, "maxLines", 5)
                            .setDuration(400).start()
                    }
                }
                binding.mediaInfoRelationRecyclerView.adapter =
                    MediaAdaptor(media.relations!!, requireActivity())
                binding.mediaInfoRelationRecyclerView.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                binding.mediaInfoGenresRecyclerView.adapter =
                    GenreAdapter(media.genres!!, type, requireActivity())
                binding.mediaInfoGenresRecyclerView.layoutManager =
                    GridLayoutManager(requireContext(), (screenWidth / 156f).toInt())

                binding.mediaInfoCharacterRecyclerView.adapter =
                    CharacterAdapter(media.characters!!, requireActivity())
                binding.mediaInfoCharacterRecyclerView.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                binding.mediaInfoRecommendedRecyclerView.adapter =
                    MediaAdaptor(media.recommendations!!, requireActivity())
                binding.mediaInfoRecommendedRecyclerView.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                if (media.anime?.nextAiringEpisodeTime != null && (media.anime.nextAiringEpisodeTime!! - System.currentTimeMillis() / 1000) <= 86400 * 7.toLong()) {
                    binding.mediaCountdownContainer.visibility = View.VISIBLE
                    timer = object : CountDownTimer(
                        (media.anime.nextAiringEpisodeTime!! + 10000) * 1000 - System.currentTimeMillis(),
                        1000
                    ) {
                        override fun onTick(millisUntilFinished: Long) {
                            val a = millisUntilFinished / 1000
                            _binding?.mediaCountdown?.text =
                                "Next Episode will be released in \n ${a / 86400} days ${a % 86400 / 3600} hrs ${a % 86400 % 3600 / 60} mins ${a % 86400 % 3600 % 60} secs"
                        }

                        override fun onFinish() {
                            _binding?.mediaCountdownContainer?.visibility = View.GONE
                        }
                    }
                    timer?.start()
                }
            }
        }
        super.onViewCreated(view, null)
    }

    override fun onResume() {
        binding.mediaInfoProgressBar.visibility = if (!loaded) View.VISIBLE else View.GONE
        super.onResume()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
