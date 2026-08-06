package com.notesnync.app.ui.components

import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import com.notesnync.app.R
import com.notesnync.app.branding.BrandPalette
import com.notesnync.app.branding.palette
import com.notesnync.app.domain.ThemeProfile
import kotlinx.coroutines.delay

@Composable
fun ComposeLoadingScreen(palette: BrandPalette = ThemeProfile.Neon.palette()) {
    var showText by remember { mutableStateOf(false) }
    var showSub by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(600)
        showText = true
        delay(400)
        showSub = true
    }

    val textAlpha by animateFloatAsState(
        targetValue = if (showText) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "textAlpha",
    )
    val textOffset by animateFloatAsState(
        targetValue = if (showText) 0f else 24f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "textOffset",
    )
    val subAlpha by animateFloatAsState(
        targetValue = if (showSub) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "subAlpha",
    )

    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgColors = if (dark) listOf(Color(0xFF241653), Color(0xFF0A061A)) else listOf(Color(0xFFFFFFFF), Color(0xFFECE4FA))
    val titleColor = if (dark) Color.White else Color(0xFF2A1B4D)
    val subColor = if (dark) Color(0xFFBBAAE0) else Color(0xFF6E5A9E)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(bgColors)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AndroidView(
                modifier = Modifier.size(180.dp),
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        overScrollMode = WebView.OVER_SCROLL_NEVER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        settings.domStorageEnabled = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        loadUrl("file:///android_asset/notes_nync_logo_animation.html")
                    }
                },
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "Notes'nync",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = titleColor,
                modifier = Modifier
                    .alpha(textAlpha)
                    .padding(top = textOffset.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sync. Write. Create.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = subColor,
                modifier = Modifier.alpha(subAlpha),
            )
        }

        // Pulsing "Loading..." at bottom
        val pulse by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing),
            label = "pulse",
        )
        Text(
            "Loading…",
            fontSize = 12.sp,
            color = titleColor.copy(alpha = 0.35f * pulse),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        )
    }
}

@Composable
fun AnimatedFab(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, containerColor = MaterialTheme.colorScheme.primary) {
        Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(value, onValueChange, modifier = modifier, singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) }, label = { Text("Search everything") })
}

@Composable
fun NavBarItem(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun EmptyNotes(onCreate: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.notesnync_cutout), null, Modifier.height(72.dp))
            Text("No notes yet", fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(top = 12.dp))
            Text("Capture ideas before they fade away.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onCreate, modifier = Modifier.padding(top = 12.dp)) { Text("Create note") }
        }
    }
}

@Composable
fun EmptyEditor(onCreate: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(100.dp))
        Image(painterResource(R.drawable.notesnync_cutout), null, Modifier.height(96.dp))
        Text("Choose or create a note", fontWeight = FontWeight.Black, fontSize = 24.sp, modifier = Modifier.padding(top = 16.dp))
        Button(onClick = onCreate, modifier = Modifier.padding(top = 12.dp)) { Text("New note") }
    }
}
