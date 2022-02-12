package ani.saikou.anime.source

abstract class Sources {
    open val names : ArrayList<String> = arrayListOf()
    abstract operator fun get(i:Int) : AnimeParser?
    abstract fun flushLive()
}