package com.ogenrwotdaniel.cufflink.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.ogenrwotdaniel.cufflink.wear.data.Entry
import com.ogenrwotdaniel.cufflink.wear.data.MediaShelf
import com.ogenrwotdaniel.cufflink.wear.ui.peek.ActionsScreen
import com.ogenrwotdaniel.cufflink.wear.ui.peek.PeekScreen
import com.ogenrwotdaniel.cufflink.wear.ui.screens.DrawerScreen
import com.ogenrwotdaniel.cufflink.wear.ui.screens.FolderScreen
import com.ogenrwotdaniel.cufflink.wear.ui.screens.HomeScreen
import com.ogenrwotdaniel.cufflink.wear.ui.screens.MediaShelfScreen
import com.ogenrwotdaniel.cufflink.wear.ui.screens.PocketScreen
import com.ogenrwotdaniel.cufflink.wear.ui.screens.PowersScreen
import com.ogenrwotdaniel.cufflink.wear.ui.screens.ShelfScreen
import com.ogenrwotdaniel.cufflink.wear.ui.theme.CufflinkTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private val vm: CufflinkViewModel by viewModels()

    /** A Pocket file a notification asked us to open. */
    private var pendingOpen by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingOpen = intent.getStringExtra(EXTRA_OPEN_POCKET)
        setContent { CufflinkTheme { CufflinkApp() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingOpen = intent.getStringExtra(EXTRA_OPEN_POCKET)
    }

    @Composable
    private fun CufflinkApp() {
        val nav = rememberSwipeDismissableNavController()
        val open: (Entry) -> Unit = { vm.selected = it; nav.navigate("peek") }
        val go: (String) -> Unit = { nav.navigate(it) }

        // Ask once for arrival notifications; everything else is asked for in Powers, in context.
        val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(Unit) {
            val prefs = getSharedPreferences("cufflink", MODE_PRIVATE)
            val granted = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted && !prefs.getBoolean("asked_notify", false)) {
                prefs.edit { putBoolean("asked_notify", true) }
                askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        LaunchedEffect(pendingOpen) {
            val name = pendingOpen ?: return@LaunchedEffect
            pendingOpen = null
            if (vm.openPocketFile(name)) nav.navigate("peek")
        }

        AppScaffold {
            SwipeDismissableNavHost(navController = nav, startDestination = "home") {
                composable("home") { HomeScreen(vm, go) }
                composable("pocket") { PocketScreen(vm, open) }
                composable("shelf") { ShelfScreen(vm, open, go) }
                composable("drawer") { DrawerScreen(vm, go) }
                composable("powers") { PowersScreen(vm) }
                composable("media/{shelf}") { back ->
                    val shelf = MediaShelf.valueOf(back.arguments?.getString("shelf") ?: MediaShelf.IMAGES.name)
                    MediaShelfScreen(vm, shelf, open, go)
                }
                composable("folder/{path}") { back ->
                    val path = Uri.decode(back.arguments?.getString("path").orEmpty())
                    FolderScreen(vm, File(path), open, go)
                }
                composable("peek") { PeekScreen(vm) { nav.navigate("actions") } }
                composable("actions") {
                    ActionsScreen(vm) { nav.popBackStack("peek", inclusive = true) }
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_POCKET = "open_pocket"
    }
}
