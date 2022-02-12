package ani.saikou.anime

import ani.saikou.anime.source.HSources
import ani.saikou.anime.source.Sources

class HWatchFragment:AnimeWatchFragment() {
    override val sources: Sources = HSources
}