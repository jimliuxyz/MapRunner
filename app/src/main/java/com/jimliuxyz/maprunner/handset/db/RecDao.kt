package com.jimliuxyz.maprunner.handset.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

/**
 * Created by jimliu on 2018/3/1.
 */
@Dao
abstract class RecDao {

    //select
    @Query("SELECT * FROM RunRec ORDER BY attentionDate DESC")
    abstract fun getRunRecList(): List<RunRec>

    @Query("SELECT * FROM RunRec where id=:id")
    abstract fun getRunRec(id: String): RunRec

    @Query("SELECT * FROM RunDir where recId=:id")
    abstract fun getRunDirs(id: String): List<RunDir>


    //insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun newRunRec(rec: RunRec)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun newRunDir(dir: RunDir)


//    //delete
//    @Query("DELETE FROM BookInfo where id=:id")
//    abstract fun delBookInfo(id: String)
//
//    @Query("DELETE FROM BookInfo")
//    abstract fun clear()

//    @Query("SELECT COUNT(*)>0 FROM BookInfo WHERE value = :arg0")
//    fun hasText(story: String): Boolean


}
