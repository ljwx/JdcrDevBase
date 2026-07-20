# Kotlin Flow 速查

这份只记最常用的东西：怎么创建 Flow、怎么把回调/Channel 转 Flow、常用操作符怎么选。

## 1. Flow 基本概念

`Flow<T>` 是一个可以连续发出多个 `T` 的异步数据流。

```kotlin
flow.collect { value ->
    // 每收到一个数据都会回调
}
```

注意：普通 `Flow` 默认是冷流，不 `collect` 就不会执行；每次重新 `collect`，上游通常都会重新执行。

`StateFlow`、`SharedFlow` 和 `Channel.receiveAsFlow()` 属于热流：数据生产不依赖某一个 collector 是否存在。

---

## 2. flow {}

适合你自己主动生产数据。

```kotlin
fun countFlow(): Flow<Int> = flow {
    emit(1)
    emit(2)
    emit(3)
}
```

常见场景：请求流程、顺序任务、分页加载。

---

## 3. flowOf / asFlow

固定几个值：

```kotlin
val flow = flowOf("A", "B", "C")
```

集合转 Flow：

```kotlin
val flow = listOf(1, 2, 3).asFlow()
```

---

## 4. callbackFlow

适合把回调 API 转成 Flow。

模板：

```kotlin
fun observeSomething(): Flow<String> = callbackFlow {
    val callback = object : SomeCallback {
        override fun onChanged(value: String) {
            trySend(value)
        }
    }

    register(callback)

    awaitClose {
        unregister(callback)
    }
}
```

记住三件事：

```text
注册监听：register(callback)
发送数据：trySend(value)
清理监听：awaitClose { unregister(callback) }
```

适合：网络监听、定位、TextWatcher、播放器回调、蓝牙扫描、传统 SDK 回调。

---

## 5. channelFlow

适合在 Flow 内部启动多个协程并发发送数据。

```kotlin
fun taskFlow(): Flow<String> = channelFlow {
    launch {
        send("任务 A 完成")
    }

    launch {
        send("任务 B 完成")
    }
}
```

大部分普通场景不用它，优先考虑 `flow {}` 或 `callbackFlow {}`。

---

## 6. Channel.receiveAsFlow

适合你已经有一个 `Channel<T>`，想暴露成 `Flow<T>`。

```kotlin
val channel = Channel<String>(Channel.BUFFERED)
val flow = channel.receiveAsFlow()
```

SSE / WebSocket 常用模型：

```kotlin
val events = Channel<Event>(Channel.BUFFERED)

val readJob = scope.launch {
    try {
        while (true) {
            val event = readEvent()
            events.send(event)
        }
    } finally {
        events.close()
    }
}

val flow = events.receiveAsFlow()
    .onCompletion {
        readJob.cancel()
    }
```

注意：`receiveAsFlow()` 不是广播。多个地方同时 collect 时，不保证每个地方都收到完整数据。要广播用 `SharedFlow`。

---

## 7. StateFlow

适合表示“当前状态”。

```kotlin
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState

fun updateLoading(loading: Boolean) {
    _uiState.value = _uiState.value.copy(loading = loading)
}
```

特点：

```text
永远有当前值
新订阅者马上收到最新值
适合 UI 状态、网络状态、登录状态
```

---

## 8. SharedFlow

适合表示“一次性事件”或“广播消息”。

```kotlin
private val _events = MutableSharedFlow<UiEvent>()
val events: SharedFlow<UiEvent> = _events

suspend fun showToast(message: String) {
    _events.emit(UiEvent.Toast(message))
}
```

特点：

```text
可以多个订阅者同时收到
默认不保存旧值
适合 Toast、跳转、弹窗、全局事件
```

注意：默认的 `MutableSharedFlow()` 使用 `replay = 0`，没有订阅者时不会保存事件，新订阅者也收不到旧事件。需要可靠消费的一次性事件时，应根据场景考虑 `Channel`、状态建模或设置合适的 `replay`。

---

## 9. 常用操作符

### map

转换数据。

```kotlin
flow.map { it.toString() }
```

### filter

过滤数据。

```kotlin
flow.filter { it > 0 }
```

### take / takeWhile

只收指定数量的数据，或者满足条件时继续收集。

```kotlin
flow.take(3)

events
    .takeWhile { it !is Finished }
    .collect { handleEvent(it) }
```

注意：`takeWhile` 不会把第一个不满足条件的值交给下游，所以上面的 `Finished` 不会进入 `collect`。

如果结束事件也需要处理，可以先处理，再判断是否结束：

```kotlin
events
    .onEach { handleEvent(it) }
    .takeWhile { it !is Finished }
    .collect()
```

适合只取前几个值、等待某个状态，以及在 SSE / WebSocket 收到业务结束事件后主动结束收集。

### onEach

每个数据经过时做点事，不改变数据。

```kotlin
flow.onEach { log(it) }
```

### distinctUntilChanged

相同数据不重复发。

```kotlin
flow.distinctUntilChanged()
```

适合网络状态、UI 状态。

### debounce

防抖，一段时间内一直变化，只取最后一次。

```kotlin
textFlow
    .debounce(300)
    .distinctUntilChanged()
    .collectLatest { keyword ->
        search(keyword)
    }
```

适合搜索框。

### timeout

限制上游两次发出数据之间的最大等待时间，适合检测 SSE / WebSocket 长时间没有业务数据。

```kotlin
@OptIn(FlowPreview::class)
events
    .timeout(10.seconds)
    .catch { cause ->
        if (cause is TimeoutCancellationException) {
            handleTimeout()
        } else {
            throw cause
        }
    }
    .collect { event ->
        handleEvent(event)
    }
```

注意：

```text
开始 collect 到第一条数据也会计算超时
每收到一次上游 emit，计时重新开始
下游 collect 处理数据的时间不计算在内
它限制的是相邻数据的等待时间，不是整个任务的总时长；总时长用 withTimeout
SSE / WebSocket 通常要等完整业务事件 emit，底层收到零散字节不一定会刷新它
业务结束不等于 Flow 自动结束；上游未结束时，之后仍可能触发 timeout
catch 要区分超时和其他异常，不要把所有异常都包装成超时
```

`timeout` 在部分协程版本中属于 `FlowPreview`。当前工程使用的 `kotlinx-coroutines 1.6.4` 没有该操作符，升级到提供该 API 的版本后才能使用。

### collectLatest

只处理最新值，旧任务没完成会被取消。

```kotlin
flow.collectLatest { value ->
    request(value)
}
```

### flatMapLatest

一个值切换成一个新的 Flow，只保留最新的 Flow。

```kotlin
keywordFlow.flatMapLatest { keyword ->
    searchFlow(keyword)
}
```

适合搜索、切换会话、切换用户。

### combine

多个 Flow 合并成一个状态。任意一个变化都会重新计算。

```kotlin
combine(userFlow, networkFlow) { user, network ->
    UiState(user = user, network = network)
}
```

### buffer / conflate

默认情况下，上游和下游顺序执行；collector 处理慢时，上游也会等待。

```kotlin
flow
    .buffer()
    .collect { slowHandle(it) }
```

`buffer` 允许上下游并发运行，但消费者持续跟不上时需要注意积压和内存占用。

```kotlin
sensorFlow
    .conflate()
    .collect { render(it) }
```

`conflate` 只保留最新值，可能丢掉中间值。它适合 UI 状态，不适合音频块、文本增量、SSE / WebSocket 消息等不能丢失的数据。`StateFlow` 本身就只表示最新状态，通常不需要再调用 `conflate()`。

### stateIn / shareIn

把冷流转换成热流，避免多个 collector 重复执行上游。

```kotlin
val state = source.stateIn(
    scope,
    SharingStarted.WhileSubscribed(5_000),
    initialValue
)

val shared = source.shareIn(
    scope,
    SharingStarted.WhileSubscribed(5_000),
    replay = 0
)
```

`stateIn` 适合共享当前状态，必须有初始值；`shareIn` 适合共享事件，可以用 `replay` 控制新订阅者能收到多少个旧值。

### catch

捕获上游异常。

```kotlin
flow
    .catch { e -> emit(defaultValue) }
    .collect { value -> render(value) }
```

注意：`catch` 捕不到 `collect {}` 里面抛出的异常。

### onCompletion

Flow 结束时回调，适合资源清理。正常完成、异常和取消都会执行。

```kotlin
flow.onCompletion { cause ->
    closeResource()

    if (cause == null) {
        // 正常完成
    } else {
        // 异常或取消
    }
}
```

SSE / WebSocket 里常用于：

```kotlin
channel.receiveAsFlow()
    .onCompletion {
        readJob.cancel()
    }
```

### flowOn

切换上游执行线程。

```kotlin
flow {
    emit(readFile())
}
    .flowOn(Dispatchers.IO)
    .collect { render(it) }
```

`flowOn` 只影响它上面的操作符。

---

## 10. 怎么选

| 场景 | 推荐 |
|---|---|
| 自己顺序发数据 | `flow {}` |
| 固定几个值 | `flowOf(...)` |
| 集合转 Flow | `asFlow()` |
| 回调转 Flow | `callbackFlow {}` |
| 内部多个协程发数据 | `channelFlow {}` |
| 已有 Channel 暴露出去 | `receiveAsFlow()` |
| 当前状态 | `StateFlow` |
| 一次性事件 / 广播 | `SharedFlow` |
| 普通 Flow 转状态 | `stateIn` |
| 普通 Flow 转共享流 | `shareIn` |
| 一段时间没有新数据就失败 | `timeout` |
| 收到业务结束条件后停止 | `takeWhile` |
| 上下游并发、缓冲数据 | `buffer` |
| 只关心最新状态 | `conflate` |

---

## 11. SSE / WebSocket 推荐模型

```text
连接方法返回 Connection
Connection 里暴露 events: Flow<Event>
内部 readJob 负责读网络
Channel 负责中转消息
receiveAsFlow 暴露给业务 collect
onCompletion 负责取消 readJob
```

这类 Connection 通常只应该有一个业务 collector。`receiveAsFlow()` 的多个 collector 会竞争 Channel 中的数据；其中一个 collector 完成后如果通过 `onCompletion` 取消 `readJob`，还会影响其他 collector。

业务结束事件和网络连接结束是两回事。收到 `Finished` 后如果不结束收集或关闭上游，空闲超时仍会继续计时。

简化代码：

```kotlin
class Connection(
    val events: Flow<Event>,
    private val readJob: Job
) {
    suspend fun close() {
        readJob.cancel()
    }
}
```

内部：

```kotlin
val events = Channel<Event>(Channel.BUFFERED)

val readJob = scope.launch {
    try {
        readEvents().collect { event ->
            events.send(event)
        }
    } finally {
        events.close()
    }
}

return Connection(
    events = events.receiveAsFlow()
        .onCompletion { readJob.cancel() },
    readJob = readJob
)
```

---

## 12. 一句话记忆

```text
自己发数据：flow
回调转 Flow：callbackFlow
已有 Channel：receiveAsFlow
状态：StateFlow
事件/广播：SharedFlow
防重复：distinctUntilChanged
防抖：debounce
空闲超时：timeout
满足条件后停止：take / takeWhile
只处理最新：collectLatest / flatMapLatest
上下游缓冲：buffer
只保留最新状态：conflate
冷流转热流：stateIn / shareIn
收尾清理：onCompletion / awaitClose
```
