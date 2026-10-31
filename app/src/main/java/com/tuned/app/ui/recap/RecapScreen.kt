package com.tuned.app.ui.recap

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.R
import com.tuned.app.data.local.db.YearlyListenEntity
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.common.AppButton
import com.tuned.app.ui.common.Artwork
import com.tuned.app.ui.common.GlassCircleButton
import com.tuned.app.ui.common.MediaTile
import com.tuned.app.ui.common.MediaTileShape
import com.tuned.app.ui.common.TunedIcons
import com.tuned.app.ui.player.FluidArtworkBackground
import com.tuned.app.ui.player.LocalMusicPlayer
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.tunedColors
import kotlinx.coroutines.launch

/** Type size that lets the minutes fill the line: short numbers huge, long ones still fitting. */
internal fun recapNumberSp(widthDp: Float, digits: Int): Float =
    (widthDp / (digits.coerceAtLeast(1) * RECAP_DIGIT_WIDTH_EM)).coerceIn(RECAP_NUMBER_MIN_SP, RECAP_NUMBER_MAX_SP)

private const val RECAP_DIGIT_WIDTH_EM = 0.72f
private const val RECAP_NUMBER_MIN_SP = 56f
private const val RECAP_NUMBER_MAX_SP = 148f
private val HeroHeight = 460.dp

private fun YearlyListenEntity.toPlayable() =
    PlayableTrack(title = title, artist = artist, artworkUrl = artworkUrl, videoId = videoId)

/**
 * A year of listening: the number on the top song's colours, then the top song and the top
 * artist at full size, each followed by the rest of their list.
 */
@Composable
fun RecapScreen(
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    viewModel: RecapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val moving by viewModel.movingBackground.collectAsStateWithLifecycle()
    val player = LocalMusicPlayer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.tunedColors
    val share: () -> Unit = { scope.launch { shareRecap(context, state) } }
    val playFrom: (Int) -> Unit = { index ->
        player.playQueue(state.topSongs.map { it.toPlayable() }, startIndex = index, sourceLabel = "${state.year}")
    }
    val topSong = state.topSongs.firstOrNull()
    val topArtist = state.topArtists.firstOrNull()

    Box(Modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 130.dp)) {
            item(key = "hero") {
                RecapHero(state, topSong, moving, onSelectYear = viewModel::show)
            }
            if (topSong != null) {
                item(key = "topSong") {
                    RecapCaption(stringResource(R.string.recap_top_song))
                    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = Keyline.detail)) {
                        val side: Dp = maxWidth
                        Column(Modifier.clickable { playFrom(0) }) {
                            Artwork(url = topSong.artworkUrl, size = side, shape = RoundedCornerShape(22.dp))
                            Spacer(Modifier.height(14.dp))
                            Text(topSong.title, style = AppType.headline, color = colors.text1, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(topSong.artist, style = MaterialTheme.typography.bodyLarge, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            RecapMinutes(topSong.listenedMs / 60_000L)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                itemsIndexed(state.topSongs.drop(1), key = { _, song -> "song_${song.trackKey}" }) { index, song ->
                    RecapSongRow(rank = index + 2, song = song, onClick = { playFrom(index + 1) })
                }
            }
            if (topArtist != null) {
                item(key = "topArtist") {
                    Spacer(Modifier.height(20.dp))
                    RecapCaption(stringResource(R.string.recap_top_artist))
                    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = Keyline.detail)) {
                        val side: Dp = maxWidth
                        Box(Modifier.clip(RoundedCornerShape(22.dp)).clickable { onOpenArtist(topArtist.name) }) {
                            Artwork(url = topArtist.artworkUrl, size = side, artistName = topArtist.name, shape = RoundedCornerShape(22.dp))
                            // A functional fade, so the name stays readable on any photo.
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f))),
                            )
                            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                                Text(topArtist.name, style = AppType.display, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    pluralStringResource(R.plurals.recap_artist_minutes, topArtist.minutes.toInt(), topArtist.minutes.toInt()),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
                if (state.topArtists.size > 1) {
                    item(key = "otherArtists") {
                        Spacer(Modifier.height(20.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Keyline.detail),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.topArtists.drop(1), key = { it.name }) { artist ->
                                MediaTile(
                                    title = artist.name,
                                    subtitle = pluralStringResource(R.plurals.recap_artist_minutes, artist.minutes.toInt(), artist.minutes.toInt()),
                                    artworkUrl = artist.artworkUrl,
                                    artistName = artist.name,
                                    shape = MediaTileShape.Circle,
                                    width = Sizes.tileArtist,
                                    onClick = { onOpenArtist(artist.name) },
                                )
                            }
                        }
                    }
                }
            }
            if (state.songCount > 0) {
                item(key = "share") {
                    Spacer(Modifier.height(32.dp))
                    AppButton(
                        text = stringResource(R.string.recap_share),
                        onClick = { share() },
                        icon = Icons.Filled.Share,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Keyline.detail),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Keyline.detail, vertical = 8.dp)) {
            GlassCircleButton(TunedIcons.ChevronBack, stringResource(R.string.common_back), onBack)
        }
    }
}

@Composable
private fun RecapHero(
    state: RecapState,
    topSong: YearlyListenEntity?,
    moving: Boolean,
    onSelectYear: (Int) -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    val number = remember(state.year) { Animatable(0f) }
    LaunchedEffect(state.year, state.minutes) {
        number.animateTo(state.minutes.toFloat(), tween(COUNT_UP_MS, easing = FastOutSlowInEasing))
    }
    Box(Modifier.fillMaxWidth().height(HeroHeight)) {
        if (topSong != null) {
            FluidArtworkBackground(
                track = topSong.toPlayable(),
                coverUrl = topSong.artworkUrl,
                modifier = Modifier.matchParentSize(),
                rotatingBackgroundEnabled = moving,
                fallback = {
                    Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
                },
            )
        }
        // Runs into the page, so the backdrop has no bottom edge.
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to colors.bg)),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = Keyline.detail),
        ) {
            Text(stringResource(R.string.recap_title, state.year), style = AppType.titleL, color = colors.text1)
            if (state.songCount == 0 && !state.isLoading) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.recap_empty), style = MaterialTheme.typography.bodyLarge, color = colors.text2)
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val text = String.format(java.util.Locale.getDefault(), "%,d", number.value.toLong())
                    val finalLength = String.format(java.util.Locale.getDefault(), "%,d", state.minutes).length
                    val size = recapNumberSp(maxWidth.value, finalLength).sp
                    Text(text, style = AppType.display.copy(fontSize = size, lineHeight = size * 1.05f), color = colors.text1, maxLines = 1)
                }
                Text(stringResource(R.string.recap_minutes_label), style = MaterialTheme.typography.bodyLarge, color = colors.text1)
                Spacer(Modifier.height(12.dp))
                Text(pluralStringResource(R.plurals.recap_songs, state.songCount, state.songCount), style = MaterialTheme.typography.bodyLarge, color = colors.text2)
                Text(pluralStringResource(R.plurals.recap_artists, state.artistCount, state.artistCount), style = MaterialTheme.typography.bodyLarge, color = colors.text2)
            }
            if (state.years.size > 1) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.years.forEach { year ->
                        FilterChip(
                            selected = year == state.year,
                            onClick = { onSelectYear(year) },
                            shape = RoundedCornerShape(50),
                            border = null,
                            label = { Text(year.toString(), fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = colors.text1.copy(alpha = 0.1f),
                                labelColor = colors.text1,
                                selectedContainerColor = colors.text1,
                                selectedLabelColor = colors.bg,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecapCaption(text: String) {
    Text(
        text,
        style = AppType.titleL,
        color = MaterialTheme.tunedColors.text1,
        modifier = Modifier.padding(start = Keyline.detail, end = Keyline.detail, top = 28.dp, bottom = 12.dp),
    )
}

@Composable
private fun RecapMinutes(minutes: Long) {
    Text(
        pluralStringResource(R.plurals.recap_artist_minutes, minutes.toInt(), minutes.toInt()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.tunedColors.text2,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun RecapSongRow(rank: Int, song: YearlyListenEntity, onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Keyline.detail, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.text2,
            modifier = Modifier.width(28.dp),
        )
        Artwork(url = song.artworkUrl, size = 48.dp, shape = RoundedCornerShape(10.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, color = colors.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The way into the recap from the You tab: the top song's cover, the year and the minutes. */
@Composable
fun RecapEntryTile(onClick: () -> Unit, modifier: Modifier = Modifier, viewModel: RecapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.tunedColors
    if (state.songCount == 0) return
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.show(state.year)
        onPauseOrDispose { }
    }
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent)))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(url = state.topSongs.firstOrNull()?.artworkUrl, size = 76.dp, shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.recap_title, state.year), style = AppType.titleL, color = colors.text1, maxLines = 1)
            Text(
                pluralStringResource(R.plurals.recap_artist_minutes, state.minutes.toInt(), state.minutes.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text2,
            )
        }
        androidx.compose.material3.Icon(
            TunedIcons.ChevronRight,
            contentDescription = null,
            tint = colors.text2,
        )
    }
}

private const val COUNT_UP_MS = 900
