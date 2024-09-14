package jp.co.greensys.weather.app.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import coil.compose.AsyncImagePainter.State.Error
import coil.compose.rememberAsyncImagePainter

@Composable
fun DynamicAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Painter = ColorPainter(color = Color.Transparent),
) {
    val isError = remember { mutableStateOf(value = false) }
    val imageLoader = rememberAsyncImagePainter(
        model = imageUrl,
        onState = { state ->
            isError.value = state is Error
        },
    )
    val isLocalInspection = LocalInspectionMode.current

    Image(
        contentScale = ContentScale.Crop,
        painter = if (isError.value.not() && !isLocalInspection) imageLoader else placeholder,
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
