package com.tomsphone.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tomsphone.core.data.local.LocalCallLogRepository
import com.tomsphone.core.data.local.LocalContactRepository
import com.tomsphone.core.data.local.WandasDatabase
import com.tomsphone.core.config.ButtonColor
import com.tomsphone.core.data.repository.CallLogRepository
import com.tomsphone.core.data.repository.ContactRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contacts ADD COLUMN notifyBatteryAlerts INTEGER NOT NULL DEFAULT 0")
    }
}

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
                "toms_phone_db_v5"
            )
                .addMigrations(MIGRATION_3_4)
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
 * Seeds the database with demo contacts on first DB creation (debug only).
 * Release builds start with an empty contact table; carers add real contacts in settings.
 */
private class SeedDatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        if (!com.tomsphone.core.data.BuildConfig.DEBUG) {
            return
        }
        
        val now = System.currentTimeMillis()
        val orange = ButtonColor.ORANGE.argb
        val purple = ButtonColor.PURPLE.argb
        
        db.execSQL(
            "INSERT INTO contacts (name, phoneNumber, photoUri, priority, contactType, createdAt, updatedAt, buttonColor, autoAnswerEnabled, notifyBatteryAlerts, buttonPosition, isHalfWidth) " +
            "VALUES ('Ashley', '07597086211', NULL, 1, 'CARER', $now, $now, $orange, 0, 1, 0, 0)"
        )
        
        db.execSQL(
            "INSERT INTO contacts (name, phoneNumber, photoUri, priority, contactType, createdAt, updatedAt, buttonColor, autoAnswerEnabled, notifyBatteryAlerts, buttonPosition, isHalfWidth) " +
            "VALUES ('Jane', '07510940646', NULL, 2, 'CARER', $now, $now, $purple, 0, 1, 1, 0)"
        )
    }
}
