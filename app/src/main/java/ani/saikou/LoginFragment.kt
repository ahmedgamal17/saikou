package ani.saikou

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ani.saikou.anilist.Anilist
import ani.saikou.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.loginButton.setOnClickListener { Anilist.loginIntent(requireActivity()) }
        binding.loginDiscord.setOnClickListener { requireActivity().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.discord)))) }
        binding.loginTelegram.setOnClickListener { requireActivity().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.telegram)))) }
        binding.loginGithub.setOnClickListener { requireActivity().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github)))) }
    }
}