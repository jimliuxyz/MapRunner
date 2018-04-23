package com.jimliuxyz.maprunner.common

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

data class BtMessage(val action: Int, val latitude:Double, val longitude:Double, val tag:Boolean=false, val msg:String?=null): Parcelable, Serializable {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readByte() != 0.toByte(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(action)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeByte(if (tag) 1 else 0)
        parcel.writeString(msg)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BtMessage> {
        override fun createFromParcel(parcel: Parcel): BtMessage {
            return BtMessage(parcel)
        }

        override fun newArray(size: Int): Array<BtMessage?> {
            return arrayOfNulls(size)
        }
    }

}
