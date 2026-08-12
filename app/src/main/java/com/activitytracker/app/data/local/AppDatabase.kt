package com.activitytracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.activitytracker.app.data.local.dao.ActivityRecordDao
import com.activitytracker.app.data.local.dao.ActivityTypeDao
import com.activitytracker.app.data.local.dao.DailyNoteDao
import com.activitytracker.app.data.local.dao.DailyScreenTimeDao
import com.activitytracker.app.data.local.dao.SadhanaPlanDao
import com.activitytracker.app.data.local.entity.ActivityRecordEntity
import com.activitytracker.app.data.local.entity.ActivityTypeEntity
import com.activitytracker.app.data.local.entity.DailyNoteEntity
import com.activitytracker.app.data.local.entity.DailyScreenTimeEntity
import com.activitytracker.app.data.local.entity.SadhanaPlanEntity

@Database(
    entities = [
        ActivityTypeEntity::class,
        ActivityRecordEntity::class,
        SadhanaPlanEntity::class,
        DailyNoteEntity::class,
        DailyScreenTimeEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityTypeDao(): ActivityTypeDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun sadhanaPlanDao(): SadhanaPlanDao
    abstract fun dailyNoteDao(): DailyNoteDao
    abstract fun dailyScreenTimeDao(): DailyScreenTimeDao

    companion object {
        const val DATABASE_NAME = "activity_tracker.db"
    }
}
