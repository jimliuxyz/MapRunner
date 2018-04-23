package com.jimliuxyz.maprunner.utils.direction

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng

data class DirNode(val type: NodeType, val latitude:Double, val longitude:Double, val distance:Double): Parcelable{
    constructor(parcel: Parcel) : this(
            NodeType.fromInt(parcel.readInt()),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readDouble()) {
    }

    fun toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type.toInt())
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeDouble(distance)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DirNode> {
        override fun createFromParcel(parcel: Parcel): DirNode {
            return DirNode(parcel)
        }

        override fun newArray(size: Int): Array<DirNode?> {
            return arrayOfNulls(size)
        }
    }
}