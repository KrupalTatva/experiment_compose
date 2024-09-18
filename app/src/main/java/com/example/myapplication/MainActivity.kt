package com.example.myapplication

import android.media.MediaPlayer
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {
    private lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePreferences.init(this)
        this.installSplashScreen()

        setContent {

            // Create a state to hold theme preference
            var isDarkTheme by remember { mutableStateOf(false) }

            // Observe theme preference
            LaunchedEffect(Unit) {
                ThemePreferences.isDarkTheme.collect { isDark ->
                    isDarkTheme = isDark
                }
            }
            MyApplicationTheme(isDarkTheme) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicCompose()
                }
            }
        }
    }
}

@Composable
fun MusicCompose(
    url: String? = "https://commondatastorage.googleapis.com/codeskulptor-demos/pyman_assets/ateapill.ogg"
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Initialize and prepare MediaPlayer with the URL
    LaunchedEffect(Unit) {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    isPlaying = true
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Handle stopping and releasing the media player
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    ///Animation from JSON
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.music_animation))

    // Animate the Lottie composition with state control
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = LottieConstants.IterateForever
    )

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            coroutineScope.launch {
                ThemePreferences.toggleTheme()
            }
        }) {
            Text("Toggle Theme")
        }
        LottieAnimation(
            composition = composition,
            progress = progress,
            modifier = Modifier.size(200.dp)
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://media2.giphy.com/media/v1.Y2lkPTc5MGI3NjExaTRpZGdoYWJpM3Flem9oNWxjcjNlazNybG9ydW91dmp6Z2J2NmF4aCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/186DbBrxSQZOmY9NTp/giphy.webp")
                .size(Size.ORIGINAL) // Use original size of the GIF
                .build(),
            imageLoader = ImageLoader.Builder(context)
                .components {
                    if ( SDK_INT >= 28 ) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }.build(),
            contentDescription = "Animated GIF",
            modifier = Modifier.size(200.dp) // Adjust size as needed
        )
        Button(onClick = {
            if (isPlaying) {
                mediaPlayer?.pause()
            } else {
                mediaPlayer?.start()
            }
            isPlaying = !isPlaying
        }) {
            Text(if (isPlaying) "Pause" else "Play")
        }
    }
}
