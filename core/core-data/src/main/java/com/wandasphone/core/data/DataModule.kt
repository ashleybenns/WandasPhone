package com.wandasphone.core.data

import android.content.Context
import androidx.room.Room
import com.wandasphone.core.data.local.LocalCallLogRepository
import com.wandasphone.core.data.local.LocalContactRepository
import com.wandasphone.core.data.local.WandasDatabase
import com.wandasphone.core.data.repository.CallLogRepository
import com.wandasphone.core.data.repository.ContactRepository
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
                "wandas_phone_db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
        
        @Provides
        fun provideContactDao(database: WandasDatabase) = database.contactDao()
        
        @Provides
        fun provideCallLogDao(database: WandasDatabase) = database.callLogDao()
    }
}

