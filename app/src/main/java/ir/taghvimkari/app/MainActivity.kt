package ir.taghvimkari.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.graphics.toArgb
import ir.taghvimkari.app.ui.Navy
import ir.taghvimkari.app.ui.PeopleViewModel
import ir.taghvimkari.app.ui.TaghvimApp
import ir.taghvimkari.app.ui.TaghvimTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PeopleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35 enforces edge-to-edge; the header is dark navy, the bottom bar is white.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Navy.toArgb()),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        setContent {
            TaghvimTheme { TaghvimApp(viewModel) }
        }
    }
}
