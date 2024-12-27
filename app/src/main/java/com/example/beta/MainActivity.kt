package com.example.beta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.beta.ui.hex.HexGridView
import com.example.beta.ui.hex.HexTile
import com.example.beta.ui.theme.BetaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Instead of a traditional setContentView(hexGridView),
        // we use Compose but embed our custom View via AndroidView.
        setContent {
            BetaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Add our custom HexGridView within the Compose layout
                    Box(Modifier.padding(innerPadding)) {
                        HexGridLauncher()
                    }
                }
            }
        }
    }

    /**
     * A small composable that creates and configures [HexGridView] programmatically.
     */
    @Composable
    private fun HexGridLauncher() {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                // Create HexGridView in code
                HexGridView(context).apply {
                    // Example: set a custom hex size
                    setHexagonSize(100f)

                    // Create some example tiles
                    val iconDrawable =
                        ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
                    val tiles = (1..20).map { i ->
                        HexTile(
                            icon = iconDrawable,
                            label = "App $i",
                            onClick = {
                                // Handle tile click
                            }
                        )
                    }

                    // Pass the tiles to the custom view
                    setTiles(tiles)
                }
            }
        )
    }
}