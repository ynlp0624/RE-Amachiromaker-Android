package cc.nekoha.amachiromaker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.nekoha.amachiromaker.data.AssetStore
import cc.nekoha.amachiromaker.model.LayerItem
import cc.nekoha.amachiromaker.model.LayerOriginal
import cc.nekoha.amachiromaker.model.LayerPart
import cc.nekoha.amachiromaker.model.LayerSelection

@Composable
fun PartTabs(
    parts: List<LayerPart>,
    store: AssetStore,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        itemsIndexed(parts) { index, part ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) scheme.primaryContainer else scheme.surface)
                    .border(
                        1.dp,
                        if (selected) scheme.primary else scheme.outline,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AssetImage(
                    store = store,
                    url = part.thumbUrl,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = part.pNm,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ItemGrid(
    part: LayerPart,
    selection: LayerSelection,
    store: AssetStore,
    columns: Int,
    onPick: (LayerItem) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (part.isRmv != 0) {
            item(key = "empty-slot") {
                EmptyTile(selected = selection.itmId == 0, onClick = onClear)
            }
        }
        gridItems(part.items, key = { it.itmId }) { item ->
            ItemTile(
                item = item,
                store = store,
                selected = selection.itmId == item.itmId,
                onClick = { onPick(item) }
            )
        }
    }
}

@Composable
private fun ItemTile(
    item: LayerItem,
    store: AssetStore,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) scheme.primaryContainer else scheme.surface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) scheme.primary else scheme.outline,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        AssetImage(
            store = store,
            url = item.thumbUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun EmptyTile(selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) scheme.primaryContainer else scheme.surface)
            .clickable(onClick = onClick)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = if (selected) scheme.primary else scheme.outline,
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    ),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
                drawLine(
                    color = scheme.onSurfaceVariant,
                    start = Offset(size.width * 0.33f, size.height * 0.33f),
                    end = Offset(size.width * 0.67f, size.height * 0.67f),
                    strokeWidth = stroke * 1.4f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = scheme.onSurfaceVariant,
                    start = Offset(size.width * 0.67f, size.height * 0.33f),
                    end = Offset(size.width * 0.33f, size.height * 0.67f),
                    strokeWidth = stroke * 1.4f,
                    cap = StrokeCap.Round
                )
            }
    )
}

@Composable
fun ColourRow(
    originals: List<LayerOriginal>,
    colours: Map<String, String>,
    selectedCId: Int,
    onPick: (LayerOriginal) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        items(originals, key = { it.cId }) { original ->
            val selected = original.cId == selectedCId
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(colours[original.cId.toString()]))
                        .border(
                            if (selected) 3.dp else 1.dp,
                            if (selected) scheme.primary else scheme.outline,
                            CircleShape
                        )
                        .clickable { onPick(original) }
                )
            }
        }
    }
}
