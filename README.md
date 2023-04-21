# ComposeRefreshLayout

[![](https://jitpack.io/v/wenhaiz/ComposeRefreshLayout.svg)](https://jitpack.io/#wenhaiz/ComposeRefreshLayout)

An Android [Compose](https://developer.android.com/jetpack/compose) component which supports refreshing and loading more data.

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
import com.dsbt.lib.composerefreshlayout.RefreshLayout
import com.dsbt.lib.composerefreshlayout.RefreshLayoutState
import com.dsbt.lib.composerefreshlayout.rememberRefreshLayoutState
import com.dsbt.lib.composerefreshlayout.DefaultRefreshFooter
import com.dsbt.lib.composerefreshlayout.DefaultRefreshHeader


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

## Customize
By default, `RefreshLayout` uses `DefaultRefreshHeader` and `DefaultRefreshFooter` as header and footer.
You can customize them by passing your own header and footer.
You can define the behavior of header and footer depends on the state of `RefreshLayoutState`.

For example:  

```kotlin
@Composable
fun DefaultRefreshHeader(state: RefreshLayoutState) {
    val heightDp = 80.dp
    var text by remember {
        mutableStateOf("")
    }
    // update text depends on the state of RefreshLayoutState
    val newText = when {
        state.isRefreshing.state == State.Resetting -> ""
        (state.isRefreshing.state == State.IDLE || state.isRefreshing.state == State.Dragging) && !state.isRefreshing.hasMoreData -> "No more data"
        state.isRefreshing.state == State.InProgress -> "Refreshing"
        state.isRefreshing.state == State.Success -> "Refresh success"
        state.isRefreshing.state == State.Failed -> "Refresh failed"
        state.isRefreshing.state == State.ReadyForAction -> "Release to refresh"
        else -> "Pull down to refresh"
    }
    if (newText.isNotEmpty()) {
        text = newText
    }
    Box(
        modifier = Modifier
            .height(heightDp)
            .fillMaxWidth()
    ) {
        Text(text = text, modifier = Modifier.align(Alignment.Center))
    }
}
```



# TODO
- Cover more cases    
- Refine refresh state definition
