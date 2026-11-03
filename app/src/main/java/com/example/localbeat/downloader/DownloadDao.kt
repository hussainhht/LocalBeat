package com.example.localbeat.downloader

import androidx.room.*
import com.example.localbeat.data.model.*
import kotlinx.coroutines.flow.Flow

class DownloadConverters {
    @TypeConverter fun status(value: String) = DownloadStatus.valueOf(value)
    @TypeConverter fun status(value: DownloadStatus) = value.name
}
@Dao
interface DownloadDao {
    @Query("SELECT * FROM DownloadItem ORDER BY createdAt DESC") fun observe(): Flow<List<DownloadItem>>
    @Query("SELECT * FROM DownloadItem WHERE id = :id") suspend fun get(id: String): DownloadItem?
    @Query("SELECT * FROM DownloadItem WHERE status = 'QUEUED' ORDER BY createdAt LIMIT 1") suspend fun next(): DownloadItem?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(item: DownloadItem)
    @Query("SELECT * FROM DownloadItem WHERE status NOT IN ('COMPLETED','FAILED','CANCELLED')") suspend fun interrupted(): List<DownloadItem>
    @Query("DELETE FROM DownloadItem WHERE status = 'COMPLETED'") suspend fun clear()
}
