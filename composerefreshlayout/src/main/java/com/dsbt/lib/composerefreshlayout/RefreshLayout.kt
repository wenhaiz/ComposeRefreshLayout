package com.dsbt.lib.composerefreshlayout

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch

/**
 * A refresh layout that can be used to refresh and load more data.
 *
 * The refresh layout is composed of a header and a footer, and the content is placed in the middle.
 * When the content is pulled down, the header will be displayed, and when the content is pulled up,
 * the footer will be displayed.
 *
 * Thanks to Github Copilot to generate this comment :)
 *
 *
 * @param state The state of the refresh layout. You can use [rememberRefreshLayoutState] to create one.
 * @param modifier The modifier to be applied to the layout.
 * @param header The header of the refresh layout. Default is [DefaultRefreshHeader].
 * @param footer The footer of the refresh layout. Default is [DefaultRefreshFooter].
 * @param enableRefresh Whether to enable refresh.
 * @param enableLoadMore Whether to enable load more.
 * @param onRefresh The callback to be invoked when the refresh action is triggered.
 * @param onLoadMore The callback to be invoked when the load more action is triggered.
 * @param contentScrollState The scroll state of the content, used to scroll the content when loading more.
 * @param content The content of the refresh layout.
 *
 * @see RefreshLayoutState for more information about the state of the refresh layout.
 * @see RefreshNestedScrollConnection for more information about the nested scroll connection.
 */
@Composable
fun RefreshLayout(
    state: RefreshLayoutState,
    modifier: Modifier = Modifier,
    header: @Composable BoxScope.(RefreshLayoutState) -> Unit = {
        DefaultRefreshHeader(state = it)
    },
    footer: @Composable BoxScope.(RefreshLayoutState) -> Unit = {
        DefaultRefreshFooter(state = it)
    },
    enableRefresh: Boolean = true,
    enableLoadMore: Boolean = true,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    contentScrollState: ScrollableState,
    content: @Composable BoxScope.(RefreshLayoutState) -> Unit,
) {
    var headerHeight by remember { mutableStateOf(0) }
    var footerHeight by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(state.isRefreshing) {
        coroutineScope.launch {
            if (state.isRefreshing.state == State.InProgress) {
                onRefresh()
                state.animateOffsetTo(headerHeight.toFloat())
            } else if (state.isRefreshing.state.isResetting) {
                state.animateOffsetTo(0f)
                state.idle()
            }
        }
    }
    LaunchedEffect(state.isLoadingMore) {
        coroutineScope.launch {
            if (state.isLoadingMore.state == State.InProgress) {
                onLoadMore()
                state.animateOffsetTo(-footerHeight.toFloat())
            } else if (state.isLoadingMore.state.isResetting) {
                state.animateOffsetTo(0f)
                state.idle()
            }
        }
    }
    val conn = remember {
        RefreshNestedScrollConnection(
            state = state,
            coroutineScope = coroutineScope,
            enableRefresh = enableRefresh,
            enableLoadMore = enableLoadMore
        )
    }.apply {
        this.enableRefresh = enableRefresh
        this.enableLoadMore = enableLoadMore
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .nestedScroll(conn)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    //Hide the header by moving it off the screen
                    IntOffset(0, -headerHeight)
                }
                .graphicsLayer {
                    //translate the header by the offset if we are refreshing (scrolling down)
                    val y = if (state.offsetY > 0) state.offsetY else 0f
                    translationY = y
                }
                .onSizeChanged {
                    state.refreshTriggerPx = it.height
                    headerHeight = it.height
                }
        ) {
            header(state)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    //Hide the footer by moving it off the screen
                    IntOffset(0, footerHeight)
                }
                .graphicsLayer {
                    //translate the footer by the offset if we are loading more (scrolling up)
                    val y = if (state.offsetY < 0) state.offsetY else 0f
                    translationY = y
                }
                .onSizeChanged {
                    state.loadMoreTriggerPx = -it.height
                    footerHeight = it.height
                }
                .align(Alignment.BottomCenter)
        ) {
            footer(state)
        }
        var stateOffsetY = remember {
            0f
        }
        LaunchedEffect(state.isLoadingMore) {
            if (state.isLoadingMore.state == State.InProgress) {
                stateOffsetY = state.offsetY
            } else if (state.isLoadingMore.state.isResetting) {
                val delta = stateOffsetY - state.offsetY
                stateOffsetY = state.offsetY
                if (delta != 0f) {
                    contentScrollState.animateScrollBy(delta)
                }
            }
        }
        LaunchedEffect(state.isRefreshing) {
            if (state.isRefreshing.state == State.InProgress) {
                stateOffsetY = state.offsetY
            } else if (state.isRefreshing.state.isResetting) {
                val delta = stateOffsetY - state.offsetY
                stateOffsetY = state.offsetY
                if (delta != 0f) {
                    contentScrollState.animateScrollBy(delta)
                }
            }
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .offset {
                //Move the content by the offset
                IntOffset(0, state.offsetY.toInt())
            }) {
            content(state)
        }
    }
}