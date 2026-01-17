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
                "toms_phone_db_v5"  // v5: Added button config fields to contacts
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
 * 
 * SECURITY NOTE:
 * - Only seeds test contacts in DEBUG builds
 * - Production builds start with empty database
 * - Carer adds contacts via settings
 */
private class SeedDatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        // Only seed test data in debug builds
        // Production builds start completely empty for security
        if (!com.tomsphone.core.data.BuildConfig.DEBUG) {
            return
        }
        
        val now = System.currentTimeMillis()
        
        // DEBUG ONLY: Test contacts
        // 1. Ashley - 07597086211 - CARER - Primary (main test contact)
        // 2. Dev - 07510940646 - CARER (second test contact)
        
        // Seed contacts with all columns including new button config fields
        // Columns: name, phoneNumber, photoUri, priority, isPrimary, contactType, createdAt, updatedAt,
        //          buttonColor, autoAnswerEnabled, buttonPosition, isHalfWidth
        
        db.execSQL(
            "INSERT INTO contacts (name, phoneNumber, photoUri, priority, isPrimary, contactType, createdAt, updatedAt, buttonColor, autoAnswerEnabled, buttonPosition, isHalfWidth) " +
            "VALUES ('Ashley', '07597086211', NULL, 1, 1, 'CARER', $now, $now, NULL, 0, 0, 0)"
        )
        
        db.execSQL(
            "INSERT INTO contacts (name, phoneNumber, photoUri, priority, isPrimary, contactType, createdAt, updatedAt, buttonColor, autoAnswerEnabled, buttonPosition, isHalfWidth) " +
            "VALUES ('Dev', '07510940646', NULL, 2, 0, 'CARER', $now, $now, NULL, 0, 1, 0)"
        )
    }
}
