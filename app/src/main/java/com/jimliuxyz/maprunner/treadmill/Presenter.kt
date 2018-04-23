package com.jimliuxyz.maprunner.treadmill

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.*
import android.widget.Toast
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.jimliuxyz.maprunner.MyApplication
import com.jimliuxyz.maprunner.utils.direction.DirNode
import com.jimliuxyz.maprunner.utils.direction.MapDirectionHelper
import com.jimliuxyz.maprunner.utils.doMain
import com.jimliuxyz.maprunner.utils.doNetwork
import java.io.IOException

class Presenter : Contract.IPresenter {

    private lateinit var context: Context
    private val DEFAULT_DESTINATION_TEXT = "台中火車站"
    private val DEFAULT_DESTINATION = LatLng(24.137194899999997, 120.68675060000001)
    private val DEFAULT_SORUCE = LatLng(24.1701, 120.64)

    private val SPEED_MIN = 0.0
    private val SPEED_MAX = 60.0
    private val STARTUP_SPEED = 15.0
    private val MOVE_INTEVAL = 300L //ms

    @Volatile
    var mSpeed = 0.0 // kilo/h
        set(value) {
            if (value > SPEED_MAX) field = SPEED_MAX
            else if (value < SPEED_MIN) field = SPEED_MIN
            else field = value
        }

    private var map: Contract.IRecMap? = null
    private var cpanel: Contract.ICPanel? = null
    private var gapi: GoogleApiClient? = null
    private val dirHelper = MapDirectionHelper()
    private lateinit var btServer: BtServer
    private var runnungTask = RunningTask()

    override fun init(map: Contract.IRecMap, cpanel: Contract.ICPanel, gapi: GoogleApiClient) {
        context = MyApplication.instance.applicationContext
        this.map = map
        this.cpanel = cpanel
        this.gapi = gapi

        if (runnungTask.status == AsyncTask.Status.PENDING) {
            runnungTask.execute()
            btServer = BtServer(btMsgHandler)
        }
    }

    private fun vibrate() {
        val myVibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            myVibrator?.vibrate(VibrationEffect.createOneShot(100, 100))
        } else
            myVibrator.vibrate(100)
    }

    val handlerThread = HandlerThread("BtServer").apply { start() }
    var btMsgHandler = Handler(handlerThread.looper, {

        when (it.what) {
        //for bluetooth state changed
            0 -> {
                val state = it.obj as Contract.BluetoothState
                cpanel?.showBluetoothState(state)
                if (state == Contract.BluetoothState.Linked) {
                    changeSpeed(STARTUP_SPEED)
                    updateCurrentLocaltion()
                } else {
                    map?.clear()
                    changeSpeed(0.0)
                    stopRunning()
                }
                vibrate()
            }
        //for receive any msg from client
            else -> {
                runnungTask?.let {
                    it.getCurrentLalo()?.let {
                        map?.addTag(it.latitude, it.longitude)
                        btServer.sendData(it.latitude, it.longitude, true)
                    }
                }
            }
        }
        true
    })

    @SuppressLint("MissingPermission")
    private fun updateCurrentLocaltion() {
        doNetwork {
            println("try updateCurrentLocaltion...")
            stopRunning()
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                    .addOnSuccessListener {
                        println("update YOUR location..." + (it?.let { "okay" } ?: "failure"))
                        if (it == null)
                            toast("無法取得您的真實位置!")

                        val la = it?.latitude ?: DEFAULT_SORUCE.latitude
                        val lo = it?.longitude ?: DEFAULT_SORUCE.longitude

                        map?.clear()
                        map?.moveMarker(la, lo) {
                            doNetwork {
                                val here = LatLng(la, lo)
                                val there = getDestination()

                                requestDirection(here, there)
                            }
                        }
                    }
                    .addOnFailureListener {
                        println("exp ${it}")
//                    Thread.sleep(5000)
//                    updateCurrentLocaltion()
                    }
        }
    }

    private fun requestDirection(src: LatLng, dest: LatLng) {
        dirHelper.requestDirection(src, dest) {
            println("requestDirection..." + (it?.let { "okay" } ?: "failure"))
            if (it == null) {
                toast("無法取得行進路線!")

                return@requestDirection
            }

            val dirlist = it!!

            map?.showDirection(dirlist)
            startRunning(dirlist)
        }
    }

    private fun stopRunning() {
        runnungTask.setDirList(null)
    }

    private fun startRunning(dirlist: List<DirNode>) {
        runnungTask.setDirList(dirlist)
    }

    inner class RunningTask : AsyncTask<Void, Void, Boolean>() {
        private var pos: LatLng? = null
        private var exit = false
        private var mileage = 0.0
        private var move = 0.0
        private var idx = 0

        private var newDirList: List<DirNode>? = null
        private var curDirList: List<DirNode>? = null

        fun getCurrentLalo(): LatLng? {
            synchronized(this) {
                return pos
            }
        }

        fun setDirList(dirlist: List<DirNode>?) {
            synchronized(this) {
                newDirList = dirlist
            }
        }

        fun exit() {
            exit = true
        }

        override fun doInBackground(vararg params: Void): Boolean {

            while (!exit) {
                if (map?.isOnAnimate() ?: false) {
                    Thread.sleep(200)
                    continue
                }

                //got new direction list
                if (curDirList !== newDirList) {
                    synchronized(this) {
                        //reset all
                        curDirList = newDirList
                        pos = null
                        mileage = 0.0
                        move = 0.0
                        idx = 0
                    }
                }

                if (curDirList != null && idx < curDirList!!.size - 1) {
                    doRnu(curDirList!!)
                }

                Thread.sleep(MOVE_INTEVAL)
            }
            return true
        }

        private fun doRnu(list: List<DirNode>) {
            var steps = (mSpeed * 1000.0 / (60.0 * 60.0 * 1000.0)) * MOVE_INTEVAL.toDouble()
            move += steps
            mileage += steps / 1000.0

            while (!exit && idx < list.size-1) {
                var node = list[idx]!!
                var next = list[idx + 1]!!

                if (move >= next.distance) {
                    move = move - next.distance
                    idx++

                    if (idx >= list.size-1) {
                        exit = true
                    } else
                        continue
                }

                val f = move / next.distance
                var la = node.latitude + (next.latitude - node.latitude) * f
                var lo = node.longitude + (next.longitude - node.longitude) * f

                if (pos == null || la != pos?.latitude || lo != pos?.longitude) {
                    pos = LatLng(la, lo)
                    publishProgress()
                }
                break
            }
        }

        var first = true
        override fun onProgressUpdate(vararg values: Void?) {
            val pos = pos!!
            cpanel?.showMileage(mileage)
            if (!first) {
                //first location(from hard code or google api) may not equal first direction(from xml or google api), so skip first
                btServer.sendData(pos.latitude, pos.longitude)
            }
            first = false
            map?.moveMarker(pos.latitude, pos.longitude) {
            }
        }
    }

    private fun getDestination(): LatLng {
        try {
            val geocoder = Geocoder(context)
            val addrs = geocoder.getFromLocationName(DEFAULT_DESTINATION_TEXT, 1)
            if (!addrs.isEmpty())
                return LatLng(addrs[0].latitude, addrs[0].longitude)
        } catch (e: IOException) {
            toast("無法取得'${DEFAULT_DESTINATION_TEXT}'位址!")
            e.printStackTrace()
        }

        return DEFAULT_DESTINATION
    }

    private fun toast(text: String) {
        doMain {
            //toast must show after looper.prepare
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    override fun deInit() {
        println("!!!!!!! deInit ~~~~~~~~~~~~~")
        this.map = null
        this.cpanel = null
        this.gapi = null

        btServer?.shutdown()
        runnungTask?.exit()
    }

    var simLinked = false
    override fun onClickBluetooth() {

        if (btServer.getState() == Contract.BluetoothState.Linked) {
            btServer.disconnect()
        }
        //simulate a link when server did not linked
        else {
            if (!simLinked) {
                toast("自體模擬")
                btMsgHandler.obtainMessage(0, Contract.BluetoothState.Linked).sendToTarget()
            } else
                btMsgHandler.obtainMessage(0, btServer.getState()).sendToTarget()
            simLinked = !simLinked
        }
    }

    override fun onClickSpeed(move: Double) {
        var newspeed = mSpeed + move
        changeSpeed(newspeed)
        cpanel?.animateSpeedBtn(move > 0)
    }

    private fun changeSpeed(newspeed: Double) {
        mSpeed = newspeed
        cpanel?.showSpeed(mSpeed)
    }
}
