package com.activitytracker.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.activitytracker.app.data.local.AppDatabase
import com.activitytracker.app.data.local.dao.ActivityRecordDao
import com.activitytracker.app.data.local.dao.ActivityTypeDao
import com.activitytracker.app.data.local.dao.DailyNoteDao
import com.activitytracker.app.data.local.dao.SadhanaPlanDao
import com.activitytracker.app.data.repository.ActivityRepositoryImpl
import com.activitytracker.app.data.repository.ActivityTypeRepositoryImpl
import com.activitytracker.app.data.repository.NoteRepositoryImpl
import com.activitytracker.app.data.repository.SadhanaPlanRepositoryImpl
import com.activitytracker.app.data.repository.ScreenTimeRepositoryImpl
import com.activitytracker.app.domain.repository.ActivityRepository
import com.activitytracker.app.domain.repository.ActivityTypeRepository
import com.activitytracker.app.domain.repository.NoteRepository
import com.activitytracker.app.domain.repository.SadhanaPlanRepository
import com.activitytracker.app.domain.repository.ScreenTimeRepository
import com.activitytracker.app.domain.usecase.screentime.ScreenTimeCalculator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_screen_time` (`localDate` TEXT NOT NULL, `totalDurationMinutes` INTEGER NOT NULL, `lastSyncedAt` INTEGER NOT NULL, PRIMARY KEY(`localDate`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_daily_screen_time_localDate`")
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    }

    @Provides
    @Singleton
    fun provideScreenTimeCalculator(@ApplicationContext context: Context): ScreenTimeCalculator {
        return ScreenTimeCalculator(context)
    }

    @Provides fun provideActivityTypeDao(db: AppDatabase): ActivityTypeDao = db.activityTypeDao()
    @Provides fun provideActivityRecordDao(db: AppDatabase): ActivityRecordDao = db.activityRecordDao()
    @Provides fun provideSadhanaPlanDao(db: AppDatabase): SadhanaPlanDao = db.sadhanaPlanDao()
    @Provides fun provideDailyNoteDao(db: AppDatabase): DailyNoteDao = db.dailyNoteDao()
    @Provides fun provideDailyScreenTimeDao(db: AppDatabase) = db.dailyScreenTimeDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository

    @Binds @Singleton
    abstract fun bindActivityTypeRepository(impl: ActivityTypeRepositoryImpl): ActivityTypeRepository

    @Binds @Singleton
    abstract fun bindSadhanaPlanRepository(impl: SadhanaPlanRepositoryImpl): SadhanaPlanRepository

    @Binds @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
    
    @Binds @Singleton
    abstract fun bindScreenTimeRepository(impl: ScreenTimeRepositoryImpl): ScreenTimeRepository
}
