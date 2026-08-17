package com.ohuang.kmp.filemanager.kmp_filemanager.ui.utils

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp


enum class ScreenType {
    PHONE, TABLET, DESKTOP
}


@Composable
fun rememberScreenType(): ScreenType {
    val widthDp = LocalWindowInfo.current.containerDpSize.width
    return remember(widthDp) {
        when {
            widthDp >= 1280.dp -> ScreenType.DESKTOP
            widthDp >= 720.dp -> ScreenType.TABLET
            else -> ScreenType.PHONE
        }

    }
}

data class FragmentBoxSize(
    val minWidth: Float,

    val maxWidth: Float,

    val minHeight: Float,

    val maxHeight: Float
)

val LocalFragmentBoxSize = compositionLocalOf<FragmentBoxSize> {
    FragmentBoxSize(0f, 0f, 0f, 0f)
}


@Composable
fun FragmentBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    isChange: Boolean = true,
    content: @Composable @UiComposable BoxWithConstraintsScope.() -> Unit
) {


    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints
    ) {
        val fragmentBoxSize = remember {
            mutableStateOf(
                FragmentBoxSize(
                    minWidth.value, maxWidth.value,
                    minHeight.value, maxHeight.value
                )
            )
        }
        val lastBoxSize = remember {
            mutableStateOf(
                FragmentBoxSize(
                    minWidth.value, maxWidth.value,
                    minHeight.value, maxHeight.value
                )
            )
        }


        LaunchedEffect(
            minWidth.value, maxWidth.value,
            minHeight.value, maxHeight.value
        ) {
            val data = FragmentBoxSize(
                minWidth.value, maxWidth.value,
                minHeight.value, maxHeight.value
            )
            fragmentBoxSize.value = data
            if (isChange) {
                lastBoxSize.value = data
            }

        }

        CompositionLocalProvider(
            LocalFragmentBoxSize.provides(
                if (isChange) {
                    fragmentBoxSize.value
                } else {
                    lastBoxSize.value
                }
            )
        ) {
                this@BoxWithConstraints.content()
        }
    }
}