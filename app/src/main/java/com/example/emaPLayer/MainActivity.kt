package com.example.emaPLayer

import android.Manifest
import kotlinx.coroutines.delay
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    private var isPlaying by mutableStateOf(false)
    private var currentSongTitle by mutableStateOf("")
    private var currentSongPath by mutableStateOf("")
    private var lastSongPath by mutableStateOf("")
//    private var progress by  mutableFloatStateOf(0f)
//    private var duration by mutableFloatStateOf(0f)

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permission launcher for Android 13+
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                launchPlayerUI()
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

        val permission = Manifest.permission.READ_MEDIA_AUDIO

        // Check permission
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            launchPlayerUI()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun launchPlayerUI() {
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xff131313)
                ) {
                    MusicPlayerScreen(
                        songs = loadSongs(LocalContext.current).sortedBy { it.first.lowercase() },
                        onSongSelected = { title, path ->
                            currentSongTitle = title
                            currentSongPath = path
                            startOrPauseMusic(path)
                        },
                        currentSongTitle = currentSongTitle,
                        isPlaying = isPlaying,
                        mediaPlayer = mediaPlayer,
                        onPlayPauseClick = { startOrPauseMusic(currentSongPath) }
                    )
                }
            }
        }
    }

    private fun startOrPauseMusic(path: String) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
            }
            isPlaying = true
            lastSongPath = path
        } else {
            if (isPlaying) {
                if(path != lastSongPath) {
                    mediaPlayer?.reset()
                    mediaPlayer?.setDataSource(path)
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()
//                    Toast.makeText(this, "false $path, $lastSongPath", Toast.LENGTH_LONG).show()
                    isPlaying = true
                    lastSongPath = path
                }else{
                    mediaPlayer?.pause()
                    isPlaying = false
                }
            } else {
                if(path != lastSongPath) {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(path)
                        prepare()
                        start()
                    }
                    isPlaying = true
//                    Toast.makeText(this, "false $path, $lastSongPath", Toast.LENGTH_LONG).show()
                    lastSongPath = path
                }else {
                    mediaPlayer?.start()
                    isPlaying = true
                }
            }
        }
    }

    private fun loadSongs(context: android.content.Context): List<Pair<String, String>> {
        val songList = mutableListOf<Pair<String, String>>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA
        )

        val cursor = context.contentResolver.query(uri, projection, selection, null, null)
        cursor?.use {
            val titleIndex = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val pathIndex = it.getColumnIndex(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                val title = it.getString(titleIndex)
                val path = it.getString(pathIndex)
                songList.add(title to path)
            }
        }
        return songList
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}


@Composable
fun MusicPlayerScreen(
    songs: List<Pair<String, String>>,
    onSongSelected: (String, String) -> Unit,
    currentSongTitle: String,  // Pass currentSongTitle as a parameter
    isPlaying: Boolean,
    mediaPlayer : MediaPlayer?,
    onPlayPauseClick: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xff131313))
    ) {
        // Display Song List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(songs) { (title, path) ->
                Box (
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable { onSongSelected(title, path) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ){
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xffffffff),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
//                            .clickable { onSongSelected(title, path) }
                            .padding(6.dp)

                    )
                }
                Divider()
            }
        }

        // Display the currently playing song title
        if (currentSongTitle.isNotEmpty()) {
            Text(
                text = currentSongTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xffffffff),
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Slider(
                    value = progress,
                    onValueChange = { newValue ->
                        progress = newValue
                    },
                    onValueChangeFinished = {
                        mediaPlayer?.seekTo(progress.toInt())
                    },
                    valueRange = 0f..(duration.takeIf { it > 0f } ?: 1f),
                    modifier = Modifier.fillMaxWidth()
                )

                // Timer Text
                Text(
                    text = "${formatTime(progress.toLong())} / ${formatTime(duration.toLong())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xffffffff)
                )
            }
            // Play/Pause Button
            Button(
                onClick = onPlayPauseClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying) Color.Green else Color.Gray
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White
                )
            }
        }
    }
    // Periodically update progress
    LaunchedEffect(isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            progress = mediaPlayer.currentPosition.toFloat()
            duration = mediaPlayer.duration.toFloat()
            delay(500)
        }
    }
}

@Composable
fun MusicPlayerProgressBar(
    mediaPlayer: MediaPlayer?,
    isPlaying: Boolean
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }

    // Periodically update progress
    LaunchedEffect(isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            progress = mediaPlayer.currentPosition.toFloat()
            duration = mediaPlayer.duration.toFloat()
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            value = progress,
            onValueChange = { newValue ->
                progress = newValue
            },
            onValueChangeFinished = {
                mediaPlayer?.seekTo(progress.toInt())
            },
            valueRange = 0f..(duration.takeIf { it > 0f } ?: 1f),
            modifier = Modifier.fillMaxWidth()
        )

        // Timer Text
        Text(
            text = "${formatTime(progress.toLong())} / ${formatTime(duration.toLong())}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}


@Preview(showBackground = true)
@Composable
fun MusicPlayerScreenPrevPreview() {
    val dummySongs = listOf(
        "Song bdfsdfsdsd sdfsdfdsfsd sdfsdfsdf sdd" +
                "sadfasfasdfasfasfsafasfdfdsfdfdfdfdfsd afsdafsdf" to "/music/songA.mp3",
        "Song a" to "/music/songB.mp3",
        "Song c" to "/music/songC.mp3"
    )

    MusicPlayerScreen(
        songs = dummySongs,
        onSongSelected = { _, _ -> },
        currentSongTitle = "Song A",
        isPlaying = true,
        mediaPlayer = null,
        onPlayPauseClick = {}
    )
}
