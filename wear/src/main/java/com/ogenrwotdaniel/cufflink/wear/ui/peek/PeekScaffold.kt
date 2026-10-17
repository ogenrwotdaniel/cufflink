package com.ogenrwotdaniel.cufflink.wear.ui.peek

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.ogenrwotdaniel.cufflink.wear.ui.theme.Cuff

/** Full-bleed viewer frame with a small "⋯" stud at the bottom that opens the file's actions. */
@Composable
fun PeekFrame(onActions: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        content()
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Cuff.Navy.copy(alpha = 0.85f))
                .clickable(onClick = onActions)
                .semantics { contentDescription = "File actions" },
            contentAlignment = Alignment.Center,
        ) {
            Text("⋯", color = Cuff.Brass, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun CenterNote(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Cuff.Mist, style = MaterialTheme.typography.bodySmall)
    }
}
