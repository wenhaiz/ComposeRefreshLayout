# ComposeRefreshLayout

[![](https://jitpack.io/v/wenhaiz/ComposeRefreshLayout.svg)](https://jitpack.io/#wenhaiz/ComposeRefreshLayout)

An Android [Compose](https://developer.android.com/jetpack/compose) component which supports refreshing and loading more data.

## Sample
![](/media/demo.gif)

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
    enableRefresh = true
) {
    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
        // items goes here
    }
}

```

After refreshing or loading more, you should call `state.finishRefresh()`
or `state.finishLoadMore()` to finish the refresh or load more action.
