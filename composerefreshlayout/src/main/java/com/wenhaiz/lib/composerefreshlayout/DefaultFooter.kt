package com.wenhaiz.lib.composerefreshlayout

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
fun DefaultRefreshFooter(state: ActionState.LoadingMoreState, color: Color = Color(0xFF808080)) {
    var text by remember {
        mutableStateOf("")
    }
    val loadMoreGestureState = state.componentStatus
    val newText = when {
        loadMoreGestureState == ActionComponentStatus.Resetting -> ""
        (loadMoreGestureState == ActionComponentStatus.IDLE || loadMoreGestureState == ActionComponentStatus.Dragging) && !state.hasMoreData -> stringResource(
            id = R.string.footer_no_more
        )

        loadMoreGestureState == ActionComponentStatus.ActionInProgress -> stringResource(id = R.string.footer_refreshing)
        loadMoreGestureState == ActionComponentStatus.ActionSuccess -> stringResource(id = R.string.footer_complete)
        loadMoreGestureState == ActionComponentStatus.ActionFailed -> stringResource(id = R.string.footer_failed)
        loadMoreGestureState == ActionComponentStatus.ReadyForAction -> stringResource(id = R.string.footer_pulling)
        else -> stringResource(id = R.string.footer_idle)
    }
    if (newText.isNotEmpty()) {
        text = newText
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val loadMoreState = state.componentStatus
            val agree =
                if (loadMoreState == ActionComponentStatus.ReadyForAction || loadMoreState == ActionComponentStatus.ActionInProgress) {
                    -90f
                } else {
                    90f
                }
            val rotation by animateFloatAsState(targetValue = agree)
            if (loadMoreState == ActionComponentStatus.ActionInProgress) {
                androidx.compose.material.CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp),
                    color = color,
                    strokeWidth = 2.dp
                )
            } else if (state.hasMoreData && !loadMoreState.isFinishing) {
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