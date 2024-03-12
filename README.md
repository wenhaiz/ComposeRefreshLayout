# ComposeRefreshLayout

[![](https://jitpack.io/v/wenhaiz/ComposeRefreshLayout.svg)](https://jitpack.io/#wenhaiz/ComposeRefreshLayout)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

An Android [Compose](https://developer.android.com/jetpack/compose) component which supports
refreshing and loading more data.

## Sample

![](/media/sample.gif)

## Usage

### Step 1

Add the JitPack repository to your build file

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2

Add the dependency

```groovy
dependencies {
    implementation 'com.github.wenhaiz:ComposeRefreshLayout:{latest_version}'
}
```

### Step 3

Use it in your Compose content.

```kotlin
import com.wenhaiz.lib.composerefreshlayout.RefreshLayout
import com.wenhaiz.lib.composerefreshlayout.RefreshLayoutState
import com.wenhaiz.lib.composerefreshlayout.rememberRefreshLayoutState
import com.wenhaiz.lib.composerefreshlayout.DefaultRefreshFooter
import com.wenhaiz.lib.composerefreshlayout.DefaultRefreshHeader


val state = rememberRefreshLayoutState()

val lazyListState = rememberLazyListState()

RefreshLayout(
    state = state,
    contentScrollState = lazyListState,
    onRefresh = {
        // do refresh 
    },
    onLoadMore = {
        // do load more
    },
    enableLoadMore = true,
    enableRefresh = true,
    header = {
        DefaultRefreshHeader(state = it)
    },
    footer = {
        DefaultRefreshFooter(state = it)
    }
) {
    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
        // items goes here
    }
}

```

After refreshing or loading more, you should call `state.finishRefresh()`
or `state.finishLoadMore()` to finish the refresh or load more action.

To refresh and load more programmatically, you can call `state.refresh()` or `state.loadMore()` and
pass an animationSpec to control the animation.

## Customize

By default, `RefreshLayout` uses `DefaultRefreshHeader` and `DefaultRefreshFooter` as header and
footer.
You can customize them by passing your own header and footer.
You can define the behavior of header and footer depends on the state
of `ActionState.RefreshingState` or `ActionState.LoadingMoreState`.

For example:

```kotlin
@Composable
fun DefaultRefreshHeader(state: ActionState.RefreshingState) {
    var text by remember {
        mutableStateOf("")
    }
    // update text depends on ActionState.RefreshingState
    val newText = when {
        state.componentStatus == ActionComponentStatus.Resetting -> ""
        (state.componentStatus == ActionComponentStatus.IDLE || state.componentStatus == ActionComponentStatus.Dragging) && !state.hasMoreData -> "No more data"
        state.componentStatus == ActionComponentStatus.InProgress -> "Refreshing"
        state.componentStatus == ActionComponentStatus.Success -> "Refresh success"
        state.componentStatus == ActionComponentStatus.Failed -> "Refresh failed"
        state.componentStatus == ActionComponentStatus.ReadyForAction -> "Release to refresh"
        else -> "Pull down to refresh"
    }
    if (newText.isNotEmpty()) {
        text = newText
    }
    Box(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth()
    ) {
        Text(text = text, modifier = Modifier.align(Alignment.Center))
    }
}
```

# TODO
- Support NestedScrolling in Android `View`
- Refine gesture handling
