package com.jimliuxyz.maprunner.handset

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import com.jimliuxyz.maprunner.MyApplication
import com.jimliuxyz.maprunner.common.BtMessage
import com.jimliuxyz.maprunner.handset.db.RecDatabase
import com.jimliuxyz.maprunner.handset.db.RunDir
import com.jimliuxyz.maprunner.handset.db.RunRec
import com.jimliuxyz.maprunner.treadmill.BtServer
import com.jimliuxyz.maprunner.treadmill.Contract
import com.jimliuxyz.maprunner.utils.doNetwork
import java.io.IOException
import java.io.ObjectInputStream
import java.text.SimpleDateFormat
import java.util.*

typealias BtMessageListener = (state: Contract.BluetoothState, msg: BtMessage?)->Unit
typealias BtStateListener = (state: Contract.BluetoothState)->Unit

class BtClient private constructor() {

    private var mContext = MyApplication.instance
    private var mState = Contract.BluetoothState.Off
    private var mSocket: BluetoothSocket? = null
    private var mRec: RunRec? = null

    private var mMsgListener: BtMessageListener? = null
    private var mStateListener: BtStateListener? = null

    companion object {
        private var instance = BtClient()

        fun getClient(): BtClient{
            return instance
        }
    }

    fun setStateListener(listener: BtStateListener){
        mStateListener = listener
        mStateListener?.invoke(mState)
    }

    fun setMsgListener(rec: RunRec, listener: BtMessageListener){
        if (isRecConnected(rec))
            mMsgListener = listener
    }

    fun isRecConnected(rec: RunRec): Boolean{
        return mRec != null && rec != null && mRec?.id == rec.id
    }

    private fun setState(state: Contract.BluetoothState) {
        synchronized(BtClient) {
            mState = state
            mStateListener?.invoke(state)
            mMsgListener?.invoke(state, null)

            if (state == Contract.BluetoothState.Off)
                reset()
        }
    }

    fun getState(): Contract.BluetoothState{
        synchronized(BtClient) {
            return mState
        }
    }

    private fun reset(){
        mSocket = null
        mRec = null
        mMsgListener = null
        mStateListener = null
    }

    fun connect(addr:String, block: (ready:Boolean, rec: RunRec?)->Unit) {
        synchronized(BtClient) {
            if (mState != Contract.BluetoothState.Off){
                block(false, null)
                return
            }
            mState = Contract.BluetoothState.Waiting
        }

        doNetwork {
            try {
                var mBtAdapter = BluetoothAdapter.getDefaultAdapter()!!

                if (!mBtAdapter.isEnabled) {
                    mBtAdapter.enable()
                    Thread.sleep(1000)
                }

                // try connect addr
                val device = mBtAdapter!!.getRemoteDevice(addr)
                var socket: BluetoothSocket? = null

                socket = device.createInsecureRfcommSocketToServiceRecord(BtServer.BT_UUID)
                socket!!.connect()
                this.mSocket = socket

                // create a new database record
                var dao = RecDatabase.getInstance(mContext).getDao()

                val title = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                mRec = RunRec(title)
                dao.newRunRec(mRec!!)

                // change mState to link and invoke connection mState
                setState(Contract.BluetoothState.Linked)
                block(true, mRec!!)

                // try to receive data
                var ins = socket!!.inputStream
                var ois = ObjectInputStream(ins)

                while (socket.isConnected) {
                    val buffer = ByteArray(1024)
                    val bytes: Int

                    try {
                        var obj = ois?.readObject()
                        var msg = obj as BtMessage

                        var dir = RunDir(mRec!!.id, msg.latitude, msg.longitude)
                        dao.newRunDir(dir)
                        mMsgListener?.invoke(mState, msg)
                    } catch (e: IOException) {
                        break
                    }
                }
            } catch (e: IOException) {
            }

            if (mState == Contract.BluetoothState.Waiting){
                setState(Contract.BluetoothState.Off)
                block(false, null)
            }
            else
                setState(Contract.BluetoothState.Off)
        }
    }

    fun close(){
        mMsgListener = null

        synchronized(BtClient) {
            if (mState == Contract.BluetoothState.Off){
                return
            }
        }

        try {
            mSocket?.close()
        } catch (e: IOException) {
        }
    }
}
