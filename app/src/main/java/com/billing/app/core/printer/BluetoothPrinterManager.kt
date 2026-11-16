package com.billing.app.core.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.billing.app.domain.model.BluetoothPrinterDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    val isConnected: Boolean get() = socket?.isConnected == true

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val CONNECT_TIMEOUT_MS = 4500L
    }

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    suspend fun getBondedPrinters(): List<BluetoothPrinterDevice> = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission()) {
            return@withContext emptyList()
        }
        val adapter = bluetoothAdapter ?: return@withContext emptyList()
        if (!adapter.isEnabled) return@withContext emptyList()
        try {
            adapter.bondedDevices.map { device ->
                BluetoothPrinterDevice(
                    name = device.name ?: "Unknown Device",
                    macAddress = device.address
                )
            }
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun connect(macAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        disconnect()
        if (!hasBluetoothPermission()) {
            return@withContext Result.failure(SecurityException("BLUETOOTH_CONNECT permission not granted"))
        }

        val adapter = bluetoothAdapter ?: return@withContext Result.failure(Exception("Bluetooth unavailable on device"))
        if (!adapter.isEnabled) return@withContext Result.failure(Exception("Bluetooth is turned off"))

        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(macAddress)
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Invalid MAC address: $macAddress"))
        }

        try {
            adapter.cancelDiscovery()
        } catch (_: Exception) {}

        val connectSuccess = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            try {
                val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
                sock.connect()
                socket = sock
                outputStream = sock.outputStream
                true
            } catch (_: Exception) {
                // Reflection fallback for channel 1
                try {
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    @Suppress("UNCHECKED_CAST")
                    val fallbackSock = method.invoke(device, 1) as BluetoothSocket
                    fallbackSock.connect()
                    socket = fallbackSock
                    outputStream = fallbackSock.outputStream
                    true
                } catch (_: Exception) {
                    disconnect()
                    false
                }
            }
        } ?: false

        if (connectSuccess) {
            Result.success(Unit)
        } else {
            disconnect()
            Result.failure(Exception("Connection to $macAddress timed out or failed"))
        }
    }

    suspend fun printBytes(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val stream = outputStream ?: return@withContext Result.failure(Exception("Printer is not connected"))
        try {
            stream.write(bytes)
            stream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    fun disconnect() {
        try { outputStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        outputStream = null
        socket = null
    }
}
