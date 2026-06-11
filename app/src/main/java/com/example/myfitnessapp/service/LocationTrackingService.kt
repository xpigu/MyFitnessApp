package com.example.myfitnessapp.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class LocationTrackingService(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation

    private val _routePoints = MutableLiveData<List<Location>>(emptyList())
    val routePoints: LiveData<List<Location>> = _routePoints

    private val _totalDistance = MutableLiveData<Double>(0.0) // 单位：公里
    val totalDistance: LiveData<Double> = _totalDistance

    private val _currentSpeed = MutableLiveData<Float>(0f) // 单位：m/s
    val currentSpeed: LiveData<Float> = _currentSpeed

    private var isTracking = false
    private var lastLocation: Location? = null
    private val pointsList = mutableListOf<Location>()
    private var distanceAccumulator = 0.0
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocationChangedInternal(location)
        }
    }

    private fun onLocationChangedInternal(location: Location) {
        if (!isTracking) return

        val currentPoint = Location(location)
        _currentLocation.value = currentPoint

        // 过滤精度较差的点，避免把基站/WiFi 粗定位误认为轨迹点
        if (currentPoint.hasAccuracy() && currentPoint.accuracy > 30f) return

        val derivedSpeed = lastLocation?.let { last ->
            calculateDerivedSpeedMetersPerSecond(last, currentPoint)
        } ?: 0f
        _currentSpeed.value = when {
            currentPoint.hasSpeed() && currentPoint.speed > MIN_VALID_SPEED_MPS -> currentPoint.speed
            derivedSpeed > MIN_VALID_SPEED_MPS -> derivedSpeed
            else -> 0f
        }

        pointsList.add(currentPoint)
        _routePoints.value = pointsList.toList()

        // 计算距离
        lastLocation?.let { last ->
            val distanceMeters = last.distanceTo(currentPoint)
            distanceAccumulator += (distanceMeters / 1000.0) // 转换为公里
            _totalDistance.value = distanceAccumulator
        }
        lastLocation = currentPoint
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        isTracking = true
        requestProvider(LocationManager.GPS_PROVIDER)
        requestProvider(LocationManager.NETWORK_PROVIDER)
        getFreshLastKnownLocation()?.let { onLocationChangedInternal(it) }
    }

    fun pauseTracking() {
        isTracking = false
        locationManager.removeUpdates(locationListener)
    }

    fun stopTracking() {
        isTracking = false
        locationManager.removeUpdates(locationListener)
        lastLocation = null
        pointsList.clear()
        distanceAccumulator = 0.0
        _routePoints.value = emptyList()
        _totalDistance.value = 0.0
        _currentSpeed.value = 0f
    }

    fun destroy() {
        locationManager.removeUpdates(locationListener)
    }

    @SuppressLint("MissingPermission")
    private fun requestProvider(provider: String) {
        if (!locationManager.isProviderEnabled(provider)) return
        locationManager.requestLocationUpdates(
            provider,
            1000L,
            0f,
            locationListener,
            Looper.getMainLooper()
        )
    }

    private fun getFreshLastKnownLocation(): Location? {
        val now = System.currentTimeMillis()
        val candidates = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).mapNotNull { provider ->
            runCatching {
                if (!locationManager.isProviderEnabled(provider)) return@runCatching null
                locationManager.getLastKnownLocation(provider)
            }.getOrNull()
        }.filter { location ->
            now - location.time <= 2 * 60 * 1000L
        }

        return candidates.minByOrNull { location ->
            if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE
        }
    }

    private fun calculateDerivedSpeedMetersPerSecond(previous: Location, current: Location): Float {
        val timeDeltaMillis = current.time - previous.time
        if (timeDeltaMillis <= 0L) return 0f

        val distanceMeters = previous.distanceTo(current)
        val derivedSpeed = distanceMeters / (timeDeltaMillis / 1000f)
        return if (derivedSpeed.isFinite() && derivedSpeed <= MAX_REASONABLE_SPEED_MPS) {
            derivedSpeed
        } else {
            0f
        }
    }

    companion object {
        private const val MIN_VALID_SPEED_MPS = 0.3f
        private const val MAX_REASONABLE_SPEED_MPS = 20f
    }
}
