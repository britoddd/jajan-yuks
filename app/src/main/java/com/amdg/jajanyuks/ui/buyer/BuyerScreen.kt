package com.amdg.jajanyuks.ui.buyer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amdg.jajanyuks.R
import com.amdg.jajanyuks.domain.NearbyVendor
import com.amdg.jajanyuks.location.locationUpdates
import com.amdg.jajanyuks.ui.components.formatDistance
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng as MapLatLng
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
private const val VENDORS = "vendors"
private const val ME = "me"
private const val VENDOR_DOTS = "vendor-dots"
private val JAKARTA = MapLatLng(-6.2, 106.8)

@Composable
fun BuyerScreen(
    onOpenVendor: (String) -> Unit,
    onOpenOrders: () -> Unit,
    onLogout: () -> Unit,
    viewModel: BuyerViewModel = viewModel(factory = BuyerViewModel.Factory),
) {
    val context = LocalContext.current
    val nearby by viewModel.nearby.collectAsStateWithLifecycle()
    val me by viewModel.myLocation.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermission = result.values.any { it }
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    // While this screen is visible and permission is granted, follow the buyer's location every ~30 s.
    LaunchedEffect(hasPermission) {
        if (hasPermission) context.locationUpdates(30_000).collect { viewModel.updateMyLocation(it.latitude, it.longitude) }
    }

    Column(Modifier.fillMaxSize()) {
        // weight(1f) gives the map all the height the list below doesn't use.
        Box(Modifier.weight(1f)) {
            VendorMap(nearby = nearby, me = me, onOpenVendor = onOpenVendor)
            FilledTonalButton(onClick = onOpenOrders, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                Text(stringResource(R.string.my_orders))
            }
            FilledTonalButton(onClick = onLogout, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                Text(stringResource(R.string.logout))
            }
        }
        NearbyList(nearby, hasPermission, onOpenVendor, Modifier.weight(0.6f))
    }
}

@Composable
private fun NearbyList(nearby: List<NearbyVendor>, hasPermission: Boolean, onOpenVendor: (String) -> Unit, modifier: Modifier) {
    val defaultName = stringResource(R.string.vendor_default_name)
    Column(modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.nearby_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
        when {
            !hasPermission -> Text(stringResource(R.string.location_needed), Modifier.padding(horizontal = 16.dp))
            nearby.isEmpty() -> Text(stringResource(R.string.nearby_empty), Modifier.padding(horizontal = 16.dp))
        }
        LazyColumn {
            items(nearby, key = { it.vendor.id }) { item ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenVendor(item.vendor.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(item.vendor.name ?: defaultName, Modifier.weight(1f))
                    item.distanceMeters?.let { Text(formatDistance(it)) }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun VendorMap(nearby: List<NearbyVendor>, me: LatLng?, onOpenVendor: (String) -> Unit) {
    val context = LocalContext.current
    val defaultName = stringResource(R.string.vendor_default_name)
    val mapView = remember { MapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) } // set once the style has loaded
    var centered by remember { mutableStateOf(false) }
    // The map's click listener is created once; this keeps it calling the latest callback.
    val openVendor by rememberUpdatedState(onOpenVendor)

    MapViewLifecycle(mapView)

    // AndroidView embeds a classic Android View (MapLibre's MapView) inside Compose.
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            mapView.getMapAsync { m ->
                m.cameraPosition = CameraPosition.Builder().target(JAKARTA).zoom(12.0).build()
                m.setStyle(STYLE_URL) { style ->
                    style.addSource(GeoJsonSource(ME))
                    style.addLayer(
                        CircleLayer("me-dot", ME).withProperties(
                            PropertyFactory.circleRadius(7f),
                            PropertyFactory.circleColor("#1E88E5"),
                            PropertyFactory.circleStrokeWidth(3f),
                            PropertyFactory.circleStrokeColor("#FFFFFF"),
                        ),
                    )
                    style.addSource(GeoJsonSource(VENDORS))
                    style.addLayer(
                        CircleLayer(VENDOR_DOTS, VENDORS).withProperties(
                            PropertyFactory.circleRadius(9f),
                            PropertyFactory.circleColor("#E65100"),
                            PropertyFactory.circleStrokeWidth(2f),
                            PropertyFactory.circleStrokeColor("#FFFFFF"),
                        ),
                    )
                    style.addLayer(
                        SymbolLayer("vendor-names", VENDORS).withProperties(
                            PropertyFactory.textField(Expression.get("name")),
                            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                            PropertyFactory.textSize(12f),
                            PropertyFactory.textOffset(arrayOf(0f, 1.5f)),
                        ),
                    )
                    // Tapping a vendor dot opens that vendor's detail screen.
                    m.addOnMapClickListener { point ->
                        val hit = m.queryRenderedFeatures(m.projection.toScreenLocation(point), VENDOR_DOTS).firstOrNull()
                        hit?.getStringProperty("id")?.let { openVendor(it) }
                        hit != null
                    }
                    map = m
                }
            }
            mapView
        },
    )

    // Re-runs whenever the vendors, the buyer's location or the map change: redraw the dots.
    LaunchedEffect(map, nearby, me) {
        val m = map ?: return@LaunchedEffect
        val style = m.style ?: return@LaunchedEffect
        val features = nearby.map { (v, _) ->
            Feature.fromGeometry(Point.fromLngLat(v.lng, v.lat)).apply {
                addStringProperty("id", v.id)
                addStringProperty("name", v.name ?: defaultName)
            }
        }
        style.getSourceAs<GeoJsonSource>(VENDORS)?.setGeoJson(FeatureCollection.fromFeatures(features))
        val meFeatures = listOfNotNull(me?.let { Feature.fromGeometry(Point.fromLngLat(it.lng, it.lat)) })
        style.getSourceAs<GeoJsonSource>(ME)?.setGeoJson(FeatureCollection.fromFeatures(meFeatures))

        // Center once: on the buyer if known, otherwise on the first vendor.
        val target = me?.let { MapLatLng(it.lat, it.lng) } ?: nearby.firstOrNull()?.let { MapLatLng(it.vendor.lat, it.vendor.lng) }
        if (!centered && target != null) {
            m.cameraPosition = CameraPosition.Builder().target(target).zoom(15.0).build()
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
