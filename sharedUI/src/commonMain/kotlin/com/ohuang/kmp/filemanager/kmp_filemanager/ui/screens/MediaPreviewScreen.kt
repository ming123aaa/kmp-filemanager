package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import coil3.compose.SubcomposeAsyncImage
import com.ohuang.kmp.filemanager.kmp_filemanager.Platform
import com.ohuang.kmp.filemanager.kmp_filemanager.PlatformType
import com.ohuang.kmp.filemanager.kmp_filemanager.getPlatform
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs

data class MediaFileInfo(
    val url: String,
    val name: String
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewScreen(
    mediaList: List<MediaFileInfo>,
    initialIndex: Int,
    onClose: () -> Unit
) {
    if (mediaList.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialIndex) { mediaList.size }
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    var uiVisible by remember { mutableStateOf(true) }
    val scale = remember { mutableStateOf(1f) }
    var offset = remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Reset zoom on page change
    LaunchedEffect(currentPage) {
        scale.value = 1f
        offset.value = Offset.Zero
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun clampOffset(s: Float, o: Offset): Offset {
        val maxX = 2000f * (s - 1) / 2
        val maxY = 2000f * (s - 1) / 2
        return Offset(
            o.x.coerceIn(-maxX, maxX),
            o.y.coerceIn(-maxY, maxY)
        )
    }

    fun handleZoom(newScale: Float) {
        val clampedScale = newScale.coerceIn(1f, 4f)
        scale.value = clampedScale
        offset.value = if (clampedScale <= 1f) Offset.Zero else clampOffset(clampedScale, offset.value)
    }

    fun goToPage(delta: Int) {
        val target = (currentPage + delta).coerceIn(0, mediaList.size - 1)
        scope.launch { pagerState.animateScrollToPage(target) }
    }

    val isDesktop = remember { getPlatform().type == PlatformType.Desktop }

    Scaffold() { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(bottom = paddingValues.calculateBottomPadding())
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            androidx.compose.ui.input.key.Key.Escape -> {
                                onClose(); true
                            }

                            androidx.compose.ui.input.key.Key.DirectionLeft -> {
                                goToPage(-1); true
                            }

                            androidx.compose.ui.input.key.Key.DirectionRight -> {
                                goToPage(1); true
                            }

                            androidx.compose.ui.input.key.Key.Equals -> {
                                handleZoom(scale.value + 0.5f); true
                            }

                            androidx.compose.ui.input.key.Key.Minus -> {
                                handleZoom(scale.value - 0.5f); true
                            }

                            else -> false
                        }
                    } else {
                        false
                    }
                }


        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val mediaFile = mediaList[page]
                Box(modifier = Modifier.fillMaxSize()) {
                    ZoomableImage(
                        url = mediaFile.url,
                        scale = scale,
                        offset = offset,
                        isSelect = page == pagerState.currentPage,
                        onScaleChange = { newScale -> handleZoom(newScale) },
                        onOffsetChange = { newOffset ->
                            offset.value = clampOffset(scale.value, newOffset)
                        },
                        onTap = { uiVisible = !uiVisible }
                    )
                }
            }

            // 顶部工具栏
            AnimatedVisibility(
                visible = uiVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = mediaList[currentPage].name,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "关闭",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.6f)
                    )
                )
            }

            if (isDesktop) {
                // 左侧上一张按钮
                if (currentPage > 0) {
                    IconButton(
                        onClick = { goToPage(-1) },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                            .size(48.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "上一张",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // 右侧下一张按钮
                if (currentPage < mediaList.size - 1) {
                    IconButton(
                        onClick = { goToPage(1) },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(48.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "下一张",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // 右下角缩放按钮
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 72.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { handleZoom(scale.value - 0.5f) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "缩小",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { handleZoom(scale.value + 0.5f) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "放大",
                            tint = Color.White
                        )
                    }
                }
            }


            // 底部页码指示器
            if (mediaList.size > 1) {
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${currentPage + 1} / ${mediaList.size}",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    url: String,
    scale: State<Float>,
    offset: State<Offset>,
    isSelect: Boolean,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }

            .then(
                if (getPlatform().type == PlatformType.Desktop) {
                    Modifier.pointerInput(scale, isSelect) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                if (isSelect && scale.value > 1.01f) {
                                    change.consume()
                                    onOffsetChange(offset.value + dragAmount)
                                }
                            }
                        )
                    }
                } else {
                    Modifier.pointerInput(Unit) {
                        awaitTransformGestures({ isSelect && scale.value > 1.01f && getPlatform().type != PlatformType.Desktop }) { _, pan, zoom, _ ->
                            val newScale = (scale.value * zoom).coerceIn(1f, 4f).also(onScaleChange)
                            if (newScale > 1.01f) {
                                val offsetScale = (1 + (newScale - 1) * 0.5f).coerceIn(1f, 1.5f)
                                val newOffset = Offset(
                                    offset.value.x + pan.x * offsetScale,
                                    offset.value.y + pan.y * offsetScale
                                )
                                onOffsetChange(newOffset)
                            }
                        }
                    }
                }
            )
            .then(
                if (getPlatform().type != PlatformType.Desktop) {
                Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (scale.value > 1f) {
                                onScaleChange(1f)
                                onOffsetChange(Offset.Zero)
                            } else {
                                val newScale = 2.5f
                                onScaleChange(newScale)
                                val newOffset = Offset(
                                    newScale * (size.width / 2f - tapOffset.x),
                                    newScale * (size.height / 2f - tapOffset.y)
                                )
                                onOffsetChange(newOffset)
                            }
                        },
                        onTap = { onTap() }
                    )
                }
            } else {
                Modifier
            })
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (!isSelect) {
                        scaleX = 1f
                        scaleY = 1f
                        translationX = 0f
                        translationY = 0f
                    } else {
                        scaleX = scale.value
                        scaleY = scale.value
                        translationX = offset.value.x
                        translationY = offset.value.y
                    }
                },
            contentScale = ContentScale.Fit,
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("图片加载失败", color = Color.White)
                }
            }
        )
    }
}


private suspend fun PointerInputScope.awaitTransformGestures(
    panZoomLock: Boolean = false,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(centroid, panChange, zoomChange, effectiveRotation)
                    }
                    event.changes.forEach {
                        if (it.positionChanged() && true) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}


suspend fun PointerInputScope.awaitTransformGestures(
    onConsume: () -> Boolean = { true },
    panZoomLock: Boolean = false,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(centroid, panChange, zoomChange, effectiveRotation)
                    }
                    event.changes.fastForEach {
                        if (it.positionChanged() && onConsume()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
    }
}