package com.example.myfitnessapp.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng

class LocationTrackingService(context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    
    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation

    private val _routePoints = MutableLiveData<List<LatLng>>(emptyList())
    val routePoints: LiveData<List<LatLng>> = _routePoints

    private val _totalDistance = MutableLiveData<Double>(0.0) // 单位：公里
    val totalDistance: LiveData<Double> = _totalDistance

    private val _currentSpeed = MutableLiveData<Float>(0f) // 单位：m/s
    val currentSpeed: LiveData<Float> = _currentSpeed

    private var isTracking = false
    private var lastLocation: Location? = null
    private val pointsList = mutableListOf<LatLng>()
    private var distanceAccumulator = 0.0

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
        .setMinUpdateIntervalMillis(1000)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (!isTracking) return

            for (location in locationResult.locations) {
                _currentLocation.value = location
                _currentSpeed.value = location.speed

                // 过滤精度较差的点
                if (location.accuracy > 20) continue

                val latLng = LatLng(location.latitude, location.longitude)
                pointsList.add(latLng)
                _routePoints.value = pointsList.toList()

                // 计算距离
                lastLocation?.let { last ->
                    val distanceMeters = last.distanceTo(location)
                    distanceAccumulator += (distanceMeters / 1000.0) // 转换为公里
                    _totalDistance.value = distanceAccumulator
                }
                lastLocation = location
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        isTracking = true
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun pauseTracking() {
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun stopTracking() {
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        lastLocation = null
        pointsList.clear()
        distanceAccumulator = 0.0
        _routePoints.value = emptyList()
        _totalDistance.value = 0.0
        _currentSpeed.value = 0f
    }
}
