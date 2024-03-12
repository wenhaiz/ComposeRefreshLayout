package com.wenhaiz.lib.composerefreshlayout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
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
fun DefaultRefreshHeader(
    state: ActionState.RefreshingState,
    color: Color = Color(0xFF808080)
) {
    Box(
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()
    ) {
        val agree =
            if (state.componentStatus == ActionComponentStatus.ReadyForAction || state.componentStatus == ActionComponentStatus.ActionInProgress) {
                90f
            } else {
                -90f
            }
        val rotation by animateFloatAsState(targetValue = agree)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.componentStatus == ActionComponentStatus.ActionInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp),
                    color = color,
                    strokeWidth = 2.dp
                )
            } else if (!state.componentStatus.isFinishing) {
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
            var headerText by remember {
                mutableStateOf("")
            }
            val text = when (state.componentStatus) {
                ActionComponentStatus.IDLE -> stringResource(id = R.string.header_idle)
                ActionComponentStatus.ReadyForAction -> stringResource(id = R.string.header_pulling)
                ActionComponentStatus.ActionInProgress -> stringResource(id = R.string.header_refreshing)
                ActionComponentStatus.ActionSuccess -> stringResource(id = R.string.header_complete)
                ActionComponentStatus.ActionFailed -> stringResource(id = R.string.header_failed)
                else -> ""
            }
            if (headerText != text && text.isNotEmpty()) {
                headerText = text
            }
            Text(text = headerText, color = color)
        }
    }
}