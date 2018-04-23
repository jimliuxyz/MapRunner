package com.jimliuxyz.maprunner.treadmill

import com.google.android.gms.common.api.GoogleApiClient
import com.jimliuxyz.maprunner.utils.direction.DirNode
import java.util.*

interface Contract {

    enum class BluetoothState{
        Off, Waiting, Linked;

        fun toInt(): Int {
            return Arrays.binarySearch(values(), this)
        }

        companion object {
            fun fromInt(i: Int): BluetoothState {
                val values = values()
                if (i in 0.. values.size)
                    return values[i]
                throw IllegalArgumentException("Out of index!")
            }
        }
    }

    interface IRecMap {

        fun isOnAnimate(): Boolean
        fun showDirection(nodes: List<DirNode>)
        fun appendDirection(node: DirNode)
        fun moveMarker(la:Double, lo:Double, done: (()->Unit)?=null)
        fun addTag(la: Double, lo: Double)
        fun clear()
    }

    interface ICPanel {
        fun showBluetoothState(state: BluetoothState)
        fun showSpeed(speed: Double)
        fun showMileage(mileage: Double)
        fun animateSpeedBtn(up:Boolean)
    }

    interface IPresenter {
        fun init(map: IRecMap, cpanel: ICPanel, gapi: GoogleApiClient)
        fun deInit()

        fun onClickBluetooth()
        fun onClickSpeed(speed:Double)
    }
}