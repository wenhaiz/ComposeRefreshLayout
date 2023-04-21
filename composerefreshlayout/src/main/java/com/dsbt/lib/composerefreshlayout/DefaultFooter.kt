package com.dsbt.lib.composerefreshlayout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp


@Composable
fun DefaultRefreshFooter(state: RefreshLayoutState, color: Color = Color(0xFF808080)) {
    val heightDp = 80.dp
    var text by remember {
        mutableStateOf("")
    }
    val newText = when {
        state.loadMoreState.gestureState == GestureState.Resetting -> ""
        (state.loadMoreState.gestureState == GestureState.IDLE || state.loadMoreState.gestureState == GestureState.Dragging) && !state.loadMoreState.hasMoreData -> stringResource(
            id = R.string.footer_no_more
        )

        state.loadMoreState.gestureState == GestureState.InProgress -> stringResource(id = R.string.footer_refreshing)
        state.loadMoreState.gestureState == GestureState.Success -> stringResource(id = R.string.footer_complete)
        state.loadMoreState.gestureState == GestureState.Failed -> stringResource(id = R.string.footer_failed)
        state.loadMoreState.gestureState == GestureState.ReadyForAction -> stringResource(id = R.string.footer_pulling)
        else -> stringResource(id = R.string.footer_idle)
    }
    if (newText.isNotEmpty()) {
        text = newText
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp)
    ) {

        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val loadMoreState = state.loadMoreState.gestureState
            val agree =
                if (loadMoreState == GestureState.ReadyForAction || loadMoreState == GestureState.InProgress) {
                    -90f
                } else {
                    90f
                }
            val rotation by animateFloatAsState(targetValue = agree)
            if (loadMoreState == GestureState.InProgress) {
                androidx.compose.material.CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp),
                    color = color,
                    strokeWidth = 2.dp
                )
            } else if (!loadMoreState.isFinishing) {
                Image(
                    painter = painterResource(id = R.drawable.ic_arrow_left),
                    contentDescription = "", modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp)
                        .padding(2.dp)
                        .graphicsLayer {
                            this.rotationZ = rotation
                        },
                    colorFilter = ColorFilter.tint(color = color)
                )
            }
            Text(text = text, modifier = Modifier, color = color)
        }
    }
}