package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.AppleGroupRow
import eu.kanade.presentation.theme.applePalette
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.ChapterPreviewState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover

private val ResultCoverWidth = 44.dp
private val ResultCoverShape = RoundedCornerShape(6.dp)

/**
 * One search result: cover, title and the latest chapter of that "线路" (source).
 * The source itself is shown once, as the sticky section header of the group.
 */
@Composable
fun GlobalSearchResultRow(
    manga: Manga,
    chapterPreview: ChapterPreviewState?,
    onRequestChapterPreview: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    val palette = applePalette()

    // Rows are only composed when they are (nearly) on screen, so this doubles as lazy loading.
    LaunchedEffect(manga.source, manga.url) {
        onRequestChapterPreview()
    }

    AppleGroupRow(
        isFirst = isFirst,
        isLast = isLast,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        MangaCover.Book(
            data = manga.asMangaCover(),
            modifier = Modifier.width(ResultCoverWidth),
            shape = ResultCoverShape,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = manga.title,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = palette.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = chapterPreview.toChapterLine(),
                fontSize = 13.sp,
                color = palette.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = MaterialSymbols.Rounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = palette.tertiaryLabel,
        )
    }
}

@Composable
fun GlobalSearchLoadingRow(
    modifier: Modifier = Modifier,
    isFirst: Boolean = true,
    isLast: Boolean = true,
) {
    val palette = applePalette()
    AppleGroupRow(isFirst = isFirst, isLast = isLast, modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = palette.tint,
            )
        }
    }
}

@Composable
fun GlobalSearchEmptyRow(
    modifier: Modifier = Modifier,
    isFirst: Boolean = true,
    isLast: Boolean = true,
) {
    val palette = applePalette()
    AppleGroupRow(isFirst = isFirst, isLast = isLast, modifier = modifier) {
        Text(text = "没有结果", fontSize = 15.sp, color = palette.secondaryLabel)
    }
}

@Composable
fun GlobalSearchErrorRow(
    message: String?,
    modifier: Modifier = Modifier,
    isFirst: Boolean = true,
    isLast: Boolean = true,
) {
    val palette = applePalette()
    AppleGroupRow(isFirst = isFirst, isLast = isLast, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "搜索失败",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = palette.label,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = message ?: "未知错误",
                fontSize = 13.sp,
                color = palette.secondaryLabel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun ChapterPreviewState?.toChapterLine(): String = when (this) {
    null -> "读取中…"
    ChapterPreviewState.Unavailable -> "暂无章节"
    is ChapterPreviewState.Available -> "更新至 $label"
}
