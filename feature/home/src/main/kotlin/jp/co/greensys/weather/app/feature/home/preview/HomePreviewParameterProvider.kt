package jp.co.greensys.weather.app.feature.home.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import jp.co.greensys.weather.app.core.ui.mock.PreviewForecastData
import jp.co.greensys.weather.app.feature.home.HomeUiState

internal class HomePreviewParameterProvider : PreviewParameterProvider<HomeUiState> {
    override val values: Sequence<HomeUiState> = sequenceOf(
        HomeUiState.Init,
        HomeUiState.Loading,
        HomeUiState.Success(forecastData = PreviewForecastData.default),
        HomeUiState.NetworkError(error = Exception()),
        HomeUiState.LocationError(error = Exception()),
    )
}
