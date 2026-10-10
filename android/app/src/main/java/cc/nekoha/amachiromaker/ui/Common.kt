package cc.nekoha.amachiromaker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.nekoha.amachiromaker.data.AssetStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes an APK asset off the main thread and caches the result per URL. */
@Composable
fun rememberAssetBitmap(store: AssetStore, url: String): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, url, store) {
        value = if (url.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.IO) { store.decodeBitmap(url)?.asImageBitmap() }
        }
    }
    return state.value
}

@Composable
fun AssetImage(
    store: AssetStore,
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val bitmap = rememberAssetBitmap(store, url)
    Box(modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
fun FlatButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val background = when {
        !enabled -> scheme.surfaceVariant
        prominent -> scheme.primary
        else -> scheme.surface
    }
    val contentColour = when {
        !enabled -> scheme.onSurfaceVariant
        prominent -> scheme.onPrimary
        else -> scheme.onSurface
    }
    Box(
        modifier = modifier
            .height(38.dp)
            .shadow(if (prominent) 3.dp else 1.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.dp, scheme.outline, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColour,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** "#RRGGBB" -> Compose colour, falling back to a neutral grey. */
fun parseHexColor(hex: String?): Color = try {
    if (hex.isNullOrBlank()) Color(0xFFB0BEC5) else Color(android.graphics.Color.parseColor(hex))
} catch (t: Throwable) {
    Color(0xFFB0BEC5)
}
