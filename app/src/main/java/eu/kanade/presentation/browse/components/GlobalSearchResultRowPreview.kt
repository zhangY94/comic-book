package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.AppleSectionHeader
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.ChapterPreviewState
import tachiyomi.domain.manga.model.Manga

private val PreviewManga = Manga.create().copy(
    id = 1L,
    source = 1L,
    url = "/manga/1",
    title = "示例漫画标题",
    favoriteAt = Manga.UNKNOWN_FAVORITE_AT,
)

@PreviewLightDark
@Composable
private fun GlobalSearchResultRowPreviews() {
    TachiyomiPreviewTheme {
        Surface {
            Column {
                AppleSectionHeader(title = "拷贝漫画", trailing = "12")
                GlobalSearchResultRow(
                    manga = PreviewManga,
                    chapterPreview = ChapterPreviewState.Available("第 128 话 新的旅程"),
                    onRequestChapterPreview = {},
                    onClick = {},
                    onLongClick = {},
                    isFirst = true,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                GlobalSearchResultRow(
                    manga = PreviewManga.copy(
                        id = 2L,
                        title = "一个特别长的漫画标题，用来确认它最多显示两行并且正确省略",
                    ),
                    chapterPreview = null,
                    onRequestChapterPreview = {},
                    onClick = {},
                    onLongClick = {},
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                GlobalSearchResultRow(
                    manga = PreviewManga.copy(id = 3L),
                    chapterPreview = ChapterPreviewState.Unavailable,
                    onRequestChapterPreview = {},
                    onClick = {},
                    onLongClick = {},
                    isLast = true,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                AppleSectionHeader(title = "包子漫画", trailing = "9")
                GlobalSearchResultRow(
                    manga = PreviewManga.copy(id = 4L, title = "海贼王"),
                    chapterPreview = ChapterPreviewState.Available("第 1128 话"),
                    onRequestChapterPreview = {},
                    onClick = {},
                    onLongClick = {},
                    isFirst = true,
                    isLast = true,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}
