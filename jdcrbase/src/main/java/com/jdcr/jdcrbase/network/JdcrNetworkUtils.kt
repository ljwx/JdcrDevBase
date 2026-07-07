package com.jdcr.jdcrbase.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import com.jdcr.jdcrbase.app.JdcrAppUtils
import com.jdcr.jdcrbase.log.JdcrDevBaseLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object JdcrNetworkUtils {

    enum class NetworkType {
        WIFI, //wifi
        CELLULAR, //蜂窝网络
        ETHERNET, //有线网络
        VPN, //VPN
        BLUETOOTH, //蓝牙网络共享
        OTHER, //没明确归类的网络类型
        NONE //没有可用网络
    }

    data class NetworkState(
        val hasInternet: Boolean, //理论上能访问互联网
        val isValidated: Boolean,//是否真的能访问互联网
        val type: NetworkType,
        val isMetered: Boolean, //是否可能消耗流量
        val isVpn: Boolean //是否走 VPN
    ) {
        companion object {
            val None = NetworkState(
                hasInternet = false,
                isValidated = false,
                type = NetworkType.NONE,
                isMetered = false,
                isVpn = false
            )
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun getCurrentState(): NetworkState {
        val cm = JdcrAppUtils.getAppContext()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val caps = cm.currentCapabilities() ?: return NetworkState.None

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

        return NetworkState(
            hasInternet = hasInternet,
            isValidated = isValidated,
            type = caps.networkType(),
            isMetered = isMetered,
            isVpn = isVpn
        )
    }

    @SuppressLint("MissingPermission")
    fun observe(): Flow<NetworkState> {
        val appContext = JdcrAppUtils.getAppContext()
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return callbackFlow {
            trySend(getCurrentState())

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(getCurrentState())
                }

                override fun onLost(network: Network) {
                    trySend(getCurrentState())
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    trySend(getCurrentState())
                }

                override fun onUnavailable() {
                    trySend(getCurrentState())
                }
            }

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            JdcrDevBaseLog.i("注册网络变化监听")
            cm.registerNetworkCallback(request, callback)

            awaitClose {
                runCatching {
                    JdcrDevBaseLog.i("注销网络变化监听")
                    cm.unregisterNetworkCallback(callback)
                }
            }
        }.distinctUntilChanged()
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun ConnectivityManager.currentCapabilities(): NetworkCapabilities? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activeNetwork?.let { getNetworkCapabilities(it) }
        } else {
            allNetworks
                .mapNotNull { getNetworkCapabilities(it) }
                .firstOrNull {
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
        }
    }

    private fun NetworkCapabilities.networkType(): NetworkType {
        return when {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkType.BLUETOOTH
            else -> NetworkType.OTHER
        }
    }

}