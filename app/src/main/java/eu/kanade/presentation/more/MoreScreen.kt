package eu.kanade.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.AppleGroupRow
import eu.kanade.presentation.theme.AppleLargeTitle
import eu.kanade.presentation.theme.AppleMetrics
import eu.kanade.presentation.theme.AppleSectionHeader
import eu.kanade.presentation.theme.applePalette
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import eu.kanade.tachiyomi.ui.more.MoreOverview
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Label
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import mihon.icons.materialsymbols.rounded.Public
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.Storage
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun MoreScreen(
    overview: MoreOverview,
    downloadQueueStateProvider: () -> DownloadQueueState,
    onClickContinueReading: (Long) -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickTracking: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
) {
    val palette = applePalette()

    Scaffold { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 20.dp,
            ),
        ) {
            item(key = "title") {
                Spacer(Modifier.height(6.dp))
                AppleLargeTitle(title = "我的")
            }

            overview.continueReading?.let { history ->
                item(key = "continue") {
                    Column(modifier = Modifier.padding(top = 16.dp, start = AppleMetrics.PageHorizontalPadding, end = AppleMetrics.PageHorizontalPadding)) {
                        AppleGroupRow(
                            isFirst = true,
                            isLast = true,
                            onClick = { onClickContinueReading(history.mangaId) },
                        ) {
                            MangaCover.Book(
                                data = history.coverData,
                                modifier = Modifier.width(44.dp),
                                shape = RoundedCornerShape(6.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = history.title,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = continueLabel(history.chapterNumber),
                                    fontSize = 13.sp,
                                    color = palette.secondaryLabel,
                                    maxLines = 1,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.tint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = MaterialSymbols.RoundedFilled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = palette.tint,
                                )
                            }
                        }
                    }
                }
            }

            if (overview.libraryCount > 0) {
                item(key = "overview") {
                    Column(modifier = Modifier.padding(top = 10.dp, start = AppleMetrics.PageHorizontalPadding, end = AppleMetrics.PageHorizontalPadding)) {
                        AppleGroupRow(
                            isFirst = true,
                            isLast = true,
                            onClick = onClickStats,
                        ) {
                            OverviewStat(
                                value = overview.libraryCount.toString(),
                                label = "收藏",
                                modifier = Modifier.weight(1f),
                            )
                            OverviewStat(
                                value = overview.unreadCount.toString(),
                                label = "未读",
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = MaterialSymbols.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = palette.tertiaryLabel,
                            )
                        }
                    }
                }
            }

            item(key = "content_header") { AppleSectionHeader(title = "内容") }
            item(key = "content_group") {
                val pending = when (val state = downloadQueueStateProvider()) {
                    DownloadQueueState.Stopped -> null
                    is DownloadQueueState.Paused -> state.pending
                    is DownloadQueueState.Downloading -> state.pending
                }
                Column(modifier = Modifier.padding(horizontal = AppleMetrics.PageHorizontalPadding)) {
                    MoreRow(
                        icon = MaterialSymbols.AutoMirroredRounded.Label,
                        title = "分类",
                        isFirst = true,
                        onClick = onClickCategories,
                    )
                    MoreRow(
                        icon = MaterialSymbols.Rounded.Download,
                        title = "下载队列",
                        badge = pending?.takeIf { it > 0 }?.toString(),
                        onClick = onClickDownloadQueue,
                    )
                    MoreRow(
                        icon = MaterialSymbols.Rounded.Public,
                        title = "在线追踪",
                        isLast = true,
                        onClick = onClickTracking,
                    )
                }
            }

            item(key = "system_header") { AppleSectionHeader(title = "系统") }
            item(key = "system_group") {
                Column(modifier = Modifier.padding(horizontal = AppleMetrics.PageHorizontalPadding)) {
                    MoreRow(
                        icon = MaterialSymbols.Rounded.Storage,
                        title = "数据与存储",
                        isFirst = true,
                        onClick = onClickDataAndStorage,
                    )
                    MoreRow(
                        icon = MaterialSymbols.Rounded.Settings,
                        title = "设置",
                        isLast = true,
                        onClick = onClickSettings,
                    )
                }
            }

            item(key = "about_header") { AppleSectionHeader(title = "关于") }
            item(key = "about_group") {
                Column(modifier = Modifier.padding(horizontal = AppleMetrics.PageHorizontalPadding)) {
                    MoreRow(
                        icon = MaterialSymbols.Rounded.Info,
                        title = "关于",
                        badge = BuildConfig.VERSION_NAME,
                        isFirst = true,
                        isLast = true,
                        onClick = onClickAbout,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onClick: () -> Unit,
) {
    AppleGroupRow(
        isFirst = isFirst,
        isLast = isLast,
        modifier = modifier,
        onClick = onClick,
    ) {
        MoreRowLabel(icon = icon, title = title, badge = badge)
    }
}

@Composable
private fun RowScope.MoreRowLabel(
    icon: ImageVector,
    title: String,
    badge: String? = null,
) {
    val palette = applePalette()
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(palette.tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = palette.tint,
        )
    }
    Text(
        text = title,
        modifier = Modifier.weight(1f),
        fontSize = 16.sp,
        color = palette.label,
    )
    if (badge != null) {
        Text(text = badge, fontSize = 13.sp, color = palette.secondaryLabel)
    }
    Icon(
        imageVector = MaterialSymbols.Rounded.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = palette.tertiaryLabel,
    )
}

@Composable
private fun OverviewStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val palette = applePalette()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = palette.label,
        )
        Text(text = label, fontSize = 12.sp, color = palette.secondaryLabel)
    }
}

private fun continueLabel(chapterNumber: Double): String {
    if (chapterNumber < 0.0) return "继续阅读"
    return "继续阅读 · 第 ${chapterNumber.toString().removeSuffix(".0")} 话"
}
