package com.example.yomu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.yomu.theme.YomuTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    
    coil3.SingletonImageLoader.setSafe { context ->
        coil3.ImageLoader.Builder(context)
            .components {
                add(coil3.network.okhttp.OkHttpNetworkFetcherFactory())
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(coil3.gif.AnimatedImageDecoder.Factory())
                } else {
                    add(coil3.gif.GifDecoder.Factory())
                }
            }
            .build()
    }

    setContent {
      YomuTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
