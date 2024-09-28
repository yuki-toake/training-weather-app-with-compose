package jp.co.greensys.weather.app.feature.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import jp.co.greensys.weather.app.core.designsystem.component.ActionIconSet
import jp.co.greensys.weather.app.core.designsystem.component.MenuSet
import jp.co.greensys.weather.app.core.designsystem.component.WeatherButton
import jp.co.greensys.weather.app.core.designsystem.component.WeatherLoading
import jp.co.greensys.weather.app.core.designsystem.component.WeatherSnackbar
import jp.co.greensys.weather.app.core.designsystem.component.WeatherTopAppBar
import jp.co.greensys.weather.app.core.designsystem.icon.WeatherIcons
import jp.co.greensys.weather.app.core.designsystem.theme.WeatherTheme
import jp.co.greensys.weather.app.core.model.Coord
import jp.co.greensys.weather.app.core.ui.component.DetailContent
import jp.co.greensys.weather.app.core.ui.component.ErrorDialog
import jp.co.greensys.weather.app.feature.home.preview.HomePreviewParameterProvider
import kotlinx.coroutines.launch
import jp.co.greensys.weather.app.core.ui.R as UiR

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun HomeScreen(
    navigateToSelect: () -> Unit,
    navigateToOss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ),
    )
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    HomeScreen(
        uiState = uiState,
        locationPermissionsState = locationPermissionsState,
        onOssClick = navigateToOss,
        onSelectClick = navigateToSelect,
        onLocationClick = { snackbarHostState ->
            val anyPermissionsGranted =
                locationPermissionsState.revokedPermissions.size < locationPermissionsState.permissions.size

            if (anyPermissionsGranted) {
                getLastLocation(
                    fusedLocationProviderClient = fusedLocationProviderClient,
                    onSuccess = viewModel::getForecastData,
                    onFailure = { it?.let(viewModel::getLocationError) },
                )
            } else {
                if (!locationPermissionsState.shouldShowRationale) {
                    locationPermissionsState.launchMultiplePermissionRequest()
                }
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.home_snackbar_message),
                        actionLabel = context.getString(R.string.home_snackbar_action_label),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                val uri = Uri.fromParts("package", context.packageName, null)
                                setData(uri)
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            }
        },
        onBottomSheetDismissRequest = viewModel::clearForecastData,
        onErrorDialogOkClick = viewModel::clearForecastData,
        modifier = modifier,
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    locationPermissionsState: MultiplePermissionsState,
    onOssClick: () -> Unit,
    onSelectClick: () -> Unit,
    onLocationClick: (snackbarHostState: SnackbarHostState) -> Unit,
    onBottomSheetDismissRequest: () -> Unit,
    onErrorDialogOkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // status バーを考慮したボトムシート用の padding
    val bottomSheetPaddingValues = WindowInsets.safeDrawing.only(sides = WindowInsetsSides.Top).asPaddingValues()
    val snackbarHostState = remember { SnackbarHostState() }

    LocationPermissionEffect(locationPermissionsState = locationPermissionsState)

    Scaffold(
        topBar = {
            WeatherTopAppBar(
                titleRes = R.string.home_title,
                actionIcon = ActionIconSet(
                    icon = WeatherIcons.More,
                    contentDescription = stringResource(id = UiR.string.ui_content_description_menu),
                    menu = listOf(
                        MenuSet(textRes = UiR.string.ui_oss, onClick = onOssClick),
                    ),
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { WeatherSnackbar(snackbarData = it) },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
        ) {
            Column(modifier = Modifier.width(intrinsicSize = IntrinsicSize.Max)) {
                WeatherButton(
                    onClick = onSelectClick,
                    text = { Text(text = stringResource(id = R.string.home_to_select)) },
                    leadingIcon = { Icon(imageVector = WeatherIcons.List, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                WeatherButton(
                    onClick = { onLocationClick(snackbarHostState) },
                    text = { Text(text = stringResource(id = R.string.home_to_location)) },
                    leadingIcon = { Icon(imageVector = WeatherIcons.Gps, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    when (uiState) {
        is HomeUiState.Init -> Unit // no-np
        is HomeUiState.Loading -> WeatherLoading(
            hasScrim = true,
            modifier = Modifier.fillMaxSize(),
        )

        is HomeUiState.Success -> ModalBottomSheet(
            onDismissRequest = onBottomSheetDismissRequest,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(paddingValues = bottomSheetPaddingValues),
        ) {
            DetailContent(
                forecastData = uiState.forecastData,
                modifier = Modifier.fillMaxSize(),
            )
        }

        is HomeUiState.NetworkError -> ErrorDialog(
            onDismissRequest = onErrorDialogOkClick,
            title = stringResource(id = R.string.home_error_loading_title),
            text = stringResource(id = R.string.home_error_loading_message),
        )

        is HomeUiState.LocationError -> ErrorDialog(
            onDismissRequest = onErrorDialogOkClick,
            title = stringResource(id = R.string.home_location_error_title),
            text = stringResource(id = R.string.home_location_error_message),
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationPermissionEffect(
    locationPermissionsState: MultiplePermissionsState,
) {
    if (LocalInspectionMode.current) return

    // 全て許可されていない場合のみ
    val allPermissionsRevoked =
        locationPermissionsState.permissions.size == locationPermissionsState.revokedPermissions.size
    LaunchedEffect(locationPermissionsState) {
        if (allPermissionsRevoked) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }
}

private fun getLastLocation(
    fusedLocationProviderClient: FusedLocationProviderClient,
    onSuccess: (Coord) -> Unit,
    onFailure: (Exception?) -> Unit,
) {
    fusedLocationProviderClient.lastLocation
        .addOnSuccessListener { location ->
            try {
                // location が null の場合があるため try catch で囲む
                onSuccess(Coord(lat = location.latitude, lon = location.longitude))
            } catch (e: Exception) {
                onFailure(e)
            }
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
        .addOnCanceledListener {
            onFailure(null)
        }
}

@OptIn(ExperimentalPermissionsApi::class)
@PreviewLightDark
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(HomePreviewParameterProvider::class)
    uiState: HomeUiState,
) {
    WeatherTheme {
        HomeScreen(
            uiState = uiState,
            locationPermissionsState = DummyLocationPermissionsState,
            onOssClick = {},
            onSelectClick = {},
            onLocationClick = {},
            onBottomSheetDismissRequest = {},
            onErrorDialogOkClick = {},
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private val DummyLocationPermissionsState = object : MultiplePermissionsState {
    override val allPermissionsGranted: Boolean = true
    override val permissions: List<PermissionState> = emptyList()
    override val revokedPermissions: List<PermissionState> = emptyList()
    override val shouldShowRationale: Boolean = false
    override fun launchMultiplePermissionRequest() = Unit
}
