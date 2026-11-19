package com.amdg.jajanyuks.ui.buyer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amdg.jajanyuks.R
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

// Free OpenStreetMap-based tiles: no API key, no billing. Swap for Google Maps later.
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val SOURCE_ID = "vendors"
private val JAKARTA = LatLng(-6.2, 106.8)

@Composable
fun BuyerScreen(viewModel: BuyerViewModel = viewModel(factory = BuyerViewModel.Factory)) {
    val vendors by viewModel.vendors.collectAsStateWithLifecycle()
    val defaultName = stringResource(R.string.vendor_default_name)
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) } // set once the style has loaded
    var centered by remember { mutableStateOf(false) }

    MapViewLifecycle(mapView)

    // AndroidView embeds a classic Android View (MapLibre's MapView) inside Compose.
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            mapView.getMapAsync { m ->
                m.cameraPosition = CameraPosition.Builder().target(JAKARTA).zoom(12.0).build()
                m.setStyle(STYLE_URL) { style ->
                    style.addSource(GeoJsonSource(SOURCE_ID))
                    style.addLayer(
                        CircleLayer("vendor-dots", SOURCE_ID).withProperties(
                            PropertyFactory.circleRadius(9f),
                            PropertyFactory.circleColor("#E65100"),
                            PropertyFactory.circleStrokeWidth(2f),
                            PropertyFactory.circleStrokeColor("#FFFFFF"),
                        ),
                    )
                    style.addLayer(
                        SymbolLayer("vendor-names", SOURCE_ID).withProperties(
                            PropertyFactory.textField(Expression.get("name")),
                            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                            PropertyFactory.textSize(12f),
                            PropertyFactory.textOffset(arrayOf(0f, 1.5f)),
                        ),
                    )
                    map = m
                }
            }
            mapView
        },
    )

    // Re-runs whenever the vendor list (or the map) changes: redraw all pins.
    LaunchedEffect(map, vendors) {
        val m = map ?: return@LaunchedEffect
        val features = vendors.map { v ->
            Feature.fromGeometry(Point.fromLngLat(v.lng, v.lat)).apply {
                addStringProperty("name", v.name ?: defaultName)
            }
        }
        m.style?.getSourceAs<GeoJsonSource>(SOURCE_ID)?.setGeoJson(FeatureCollection.fromFeatures(features))
        if (!centered && vendors.isNotEmpty()) {
            m.cameraPosition = CameraPosition.Builder().target(LatLng(vendors[0].lat, vendors[0].lng)).zoom(15.0).build()
            centered = true
        }
    }
}

// MapView needs to hear the screen's lifecycle (start, pause, ...) to load tiles and free memory.
@Composable
private fun MapViewLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            // Leaving the screen: wind the map down unless the activity already did.
            val state = lifecycle.currentState
            if (state.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (state.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            if (state.isAtLeast(Lifecycle.State.CREATED)) mapView.onDestroy()
        }
    }
}
