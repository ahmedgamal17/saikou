package ani.saikou.anilist

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ani.saikou.loadIsMAL
import ani.saikou.loadMedia
import ani.saikou.startMainActivity

class UrlMedia: AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data: Uri? = intent?.data
//        Toast.makeText(this,"data : ${data?.pathSegments}", Toast.LENGTH_SHORT).show()
        if (data?.host!="anilist.co") loadIsMAL = true
        if (data?.pathSegments?.get(1)!=null) loadMedia = data.pathSegments?.get(1)!!.toIntOrNull()
        startMainActivity(this)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            val intent = Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            val uri = Uri.fromParts("package", packageName, null)
//            intent.data = uri
//            startActivity(intent)
//        }
    }
}