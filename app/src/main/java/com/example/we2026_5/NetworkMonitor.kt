package com.example.we2026_5

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkMonitor(
    private val context: Context,
    private val scope: CoroutineScope // Lifecycle-aware CoroutineScope
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline
    
    private val _isSyncing = MutableLiveData<Boolean>()
    val isSyncing: LiveData<Boolean> = _isSyncing

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.postValue(true)
            // Realtime Database synchronisiert automatisch
            checkSyncStatus()
        }

        override fun onLost(network: Network) {
            _isOnline.postValue(false)
            // Realtime Database arbeitet automatisch offline
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                             networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.postValue(hasInternet)
            
            if (hasInternet) {
                checkSyncStatus()
            }
        }
    }
    
    private fun checkSyncStatus() {
        scope.launch(Dispatchers.IO) {
            _isSyncing.postValue(true)
            // Realtime Database synchronisiert automatisch im Hintergrund
            // Kurze Verzögerung für UI-Feedback, dann Status zurücksetzen
            kotlinx.coroutines.delay(500)
            _isSyncing.postValue(false)
        }
    }

    fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Initialer Status
        checkCurrentStatus()
    }

    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun checkCurrentStatus() {
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                         capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        _isOnline.postValue(isConnected)
        
        // Realtime Database synchronisiert automatisch
        if (isConnected) {
            checkSyncStatus()
        }
    }
}
