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
import androidx.compose.material.icons.filled.Audiotrack
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
                        audioFiles = loadSongs(LocalContext.current).sortedBy { it.first.lowercase() },
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
    audioFiles: List<Pair<String, String>>,
    onSongSelected: (String, String) -> Unit,
    currentSongTitle: String,  // Pass currentSongTitle as a parameter
    isPlaying: Boolean,
    mediaPlayer : MediaPlayer?,
    onPlayPauseClick: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF131313))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // header
            Text(
                text = "My Library",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(16.dp).padding(top = 16.dp)
            )

            // song list
            if (audioFiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Audio Found", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp) // Space for the player
                ) {
                    items(audioFiles) { (title, path) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSongSelected(title, path) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon placeholder
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(45.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Audiotrack, // Use this instead
                                    contentDescription = null,
                                    tint = if (currentSongTitle == title) Color.Green else Color.White,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (currentSongTitle == title) Color.Green else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // player panel
        if (currentSongTitle.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                color = Color(0xFF1E1E1E), // Slightly lighter than background for depth
                tonalElevation = 8.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Title and Play Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSongTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isPlaying) "Playing" else "Paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        // Play/Pause Fab-style button
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier
                                .background(
                                    if (isPlaying) Color.Green else Color.Gray,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Slider
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        onValueChangeFinished = { mediaPlayer?.seekTo(progress.toInt()) },
                        valueRange = 0f..(duration.coerceAtLeast(1f)),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Green,
                            activeTrackColor = Color.Green,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    )

                    // Time Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(progress.toLong()), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        Text(formatTime(duration.toLong()), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }
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
        audioFiles = dummySongs,
        onSongSelected = { _, _ -> },
        currentSongTitle = "Song A",
        isPlaying = true,
        mediaPlayer = null,
        onPlayPauseClick = {}
    )
}
