package com.jimliuxyz.maprunner.treadmill

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.util.Log
import com.jimliuxyz.maprunner.MyApplication
import com.jimliuxyz.maprunner.common.BtMessage
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.*

class BtServer(val msgHandler: Handler) {

    companion object {
        val TAG = "BtServer"
//    val BT_NAME = "Treadmill"
//    val BT_UUID = UUID.fromString("0AB71EE6-3C14-42EA-AF76-E748EAB79BFF")

        val BT_NAME = "BluetoothChatSecure"
        val BT_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    }

    private var context: Context
    private var mAdapter: BluetoothAdapter? = null
    private var server: AcceptThread? = null

    private var mState = Contract.BluetoothState.Off
        set(value) {
            if (field == value) return
            synchronized(this) {
                field = value
                msgHandler.obtainMessage(0, value).sendToTarget()
            }
        }

    fun getState():Contract.BluetoothState{
        return mState
    }


    init {
        context = MyApplication.instance

        BluetoothAdapter.getDefaultAdapter()?.let {
            mAdapter = it
            server = AcceptThread().apply {
                start()
            }
        }
    }

    private inner class AcceptThread() : Thread() {
        var mServerSocket: BluetoothServerSocket? = null
        var mBtSocket: BluetoothSocket?= null
        var oos: ObjectOutputStream? = null
        var exit = false

        override fun run() {
            val adapter = mAdapter?:return

            while (!exit) {

                try{
                    mServerSocket?.close()
                    mBtSocket?.close()
                }
                catch (e: Exception){
                }

                try {
                    if (!adapter.isEnabled) {
                        mState = Contract.BluetoothState.Off
                        adapter.enable()
                        Thread.sleep(1000)
                        if (!adapter.isEnabled)
                            continue
                    }
                    mState = Contract.BluetoothState.Waiting

                    println("waiting bt connection....")
                    mServerSocket = adapter.listenUsingRfcommWithServiceRecord(BT_NAME, BT_UUID)
                } catch (e: IOException) {
                    Log.e(TAG, "listen() failed", e)
                    Thread.sleep(3000)
                    continue
                }

                try {
                    println("waiting bt accept....")
                    mBtSocket = mServerSocket!!.accept()
                    mState = Contract.BluetoothState.Linked
                    println("bt connected!")
                    oos = ObjectOutputStream(mBtSocket!!.outputStream)
                } catch (e: IOException) {
                    Log.e(TAG, "accept() failed", e)
                    continue
                }

                while (true) {
                    val buffer = ByteArray(1024)
                    val bytes: Int

                    try {
                        var ins = mBtSocket!!.inputStream

                        bytes = ins.read(buffer)
//                        println("read : ${String(buffer, 0, bytes)}")

                        msgHandler.obtainMessage(99, String(buffer, 0, bytes)).sendToTarget()

                        // Send the obtained bytes to the UI Activity
//                        mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
//                                .sendToTarget()
                    } catch (e: IOException) {
                        Log.e(TAG, "disconnected", e)
                        break
                    }
                }
            }
        }
    }

    fun disconnect(){
        server?.apply {
            try {
                mBtSocket?.close()
                mServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of server failed", e)
            }
        }
    }

    fun shutdown(){
        server?.apply {
            exit = true
        }
        disconnect()
    }

    fun sendData(la: Double, lo:Double, tag:Boolean=false) {
        try {
            server?.oos?.apply {
                writeObject(BtMessage(0, la, lo, tag))
                flush()
            }
        } catch (e: IOException) {
        }
    }

}
