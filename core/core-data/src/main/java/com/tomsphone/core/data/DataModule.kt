package com.tomsphone.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tomsphone.core.data.local.LocalCallLogRepository
import com.tomsphone.core.data.local.LocalContactRepository
import com.tomsphone.core.data.local.WandasDatabase
import com.tomsphone.core.data.local.entity.ContactEntity
import com.tomsphone.core.data.repository.CallLogRepository
import com.tomsphone.core.data.repository.ContactRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    
    @Binds
    @Singleton
    abstract fun bindContactRepository(
        impl: LocalContactRepository
    ): ContactRepository
    
    @Binds
    @Singleton
    abstract fun bindCallLogRepository(
        impl: LocalCallLogRepository
    ): CallLogRepository
    
    companion object {
        @Provides
        @Singleton
        fun provideWandasDatabase(
            @ApplicationContext context: Context
        ): WandasDatabase {
            return Room.databaseBuilder(
                context,
                WandasDatabase::class.java,
                "toms_phone_db"  // Changed from wandas to avoid conflicts
            )
                .fallbackToDestructiveMigration()
                .addCallback(SeedDatabaseCallback())
                .build()
        }
        
        @Provides
        fun provideContactDao(database: WandasDatabase) = database.contactDao()
        
        @Provides
        fun provideCallLogDao(database: WandasDatabase) = database.callLogDao()
    }
}

/**
 * Seeds the database with test contacts on first creation.
 * TODO: Remove this before production - contacts should be added via carer setup.
 */
private class SeedDatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        val now = System.currentTimeMillis()
        
        // Test contacts - REMOVE BEFORE PRODUCTION
        // 1. Ashley - 07597086211 - CARER - Primary (main test contact)
        // 2. Dev - 07510940646 - GREY_LIST (can receive calls, no nag)
        
        db.execSQL(
            "INSERT INTO contacts (name, phoneNumber, photoUri, priority, isPrimary, contactType, createdAt, updatedAt) " +
            "VALUES ('Ashley', '07597086211', NULL, 1, 1, 'CARER', $now, $now)"
        )
        
        db.execSQL(
            "INSERT INTO contacts (name, phoneNumber, photoUri, priority, isPrimary, contactType, createdAt, updatedAt) " +
            "VALUES ('Dev', '07510940646', NULL, 2, 0, 'GREY_LIST', $now, $now)"
        )
    }
}
