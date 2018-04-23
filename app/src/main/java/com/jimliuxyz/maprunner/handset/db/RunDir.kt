package com.jimliuxyz.maprunner.handset.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.ForeignKey.CASCADE
import android.arch.persistence.room.PrimaryKey

@Entity(foreignKeys = [ForeignKey(entity = RunRec::class,
        parentColumns = ["id"],
        childColumns = ["recId"],
        onDelete = CASCADE)])
data class RunDir(
        var recId: String,
        var latitude: Double,
        var longitude: Double,
        @PrimaryKey(autoGenerate = true) var id: Long = 0
) {

}


