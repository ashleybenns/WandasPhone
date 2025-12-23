package com.wandasphone.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wandasphone.core.data.local.dao.CallLogDao
import com.wandasphone.core.data.local.dao.ContactDao
import com.wandasphone.core.data.local.entity.CallLogEntity
import com.wandasphone.core.data.local.entity.ContactEntity

@Database(
    entities = [
        ContactEntity::class,
        CallLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WandasDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun callLogDao(): CallLogDao
}

