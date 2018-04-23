package com.jimliuxyz.maprunner.handset.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverter
import android.arch.persistence.room.TypeConverters
import com.jimliuxyz.maprunner.utils.UUID62
import java.io.Serializable
import java.util.*

@Entity
@TypeConverters(RunRec.Converter::class)
data class RunRec(
        var title: String,
        var attentionDate: Date = Date(System.currentTimeMillis()),
        @PrimaryKey var id: String = UUID62.randomUUID()
): Serializable {
    class Converter {
        @TypeConverter
        fun date2Long(date: Date): Long {
            return date.time
        }

        @TypeConverter
        fun long2Date(timems: Long): Date {
            return Date(timems)
        }
    }
}