package com.example.localbeat.data.library

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity data class Favorite(@PrimaryKey val songId: String)
@Entity data class LocalPlaylist(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)
@Entity(primaryKeys = ["playlistId", "songId"], foreignKeys = [
    ForeignKey(entity = LocalPlaylist::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE)
])
data class PlaylistSong(val playlistId: Long, val songId: String, val addedAt: Long)
@Entity data class PlayStat(@PrimaryKey val songId: String, val lastPlayed: Long, val playCount: Long)

@Dao
abstract class LibraryDao {
    @Query("SELECT * FROM Favorite") abstract fun favorites(): Flow<List<Favorite>>
    @Query("SELECT * FROM LocalPlaylist ORDER BY name COLLATE NOCASE") abstract fun playlists(): Flow<List<LocalPlaylist>>
    @Query("SELECT * FROM PlaylistSong ORDER BY addedAt, songId") abstract fun entries(): Flow<List<PlaylistSong>>
    @Query("SELECT * FROM PlayStat ORDER BY lastPlayed DESC") abstract fun stats(): Flow<List<PlayStat>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun favorite(item: Favorite)
    @Query("DELETE FROM Favorite WHERE songId = :id") abstract suspend fun unfavorite(id: String)
    @Insert abstract suspend fun create(item: LocalPlaylist): Long
    @Query("UPDATE LocalPlaylist SET name = :name WHERE id = :id") abstract suspend fun rename(id: Long, name: String)
    @Query("DELETE FROM LocalPlaylist WHERE id = :id") abstract suspend fun delete(id: Long)
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun add(item: PlaylistSong)
    @Query("DELETE FROM PlaylistSong WHERE playlistId = :playlist AND songId = :song")
    abstract suspend fun remove(playlist: Long, song: String)
    @Query("INSERT OR IGNORE INTO PlayStat(songId, lastPlayed, playCount) VALUES(:id, 0, 0)")
    protected abstract suspend fun ensureStat(id: String)
    @Query("UPDATE PlayStat SET lastPlayed = :time, playCount = playCount + 1 WHERE songId = :id")
    protected abstract suspend fun increment(id: String, time: Long)
    @Transaction
    open suspend fun record(id: String, time: Long) { ensureStat(id); increment(id, time) }
}

@TypeConverters(com.example.localbeat.downloader.DownloadConverters::class)
@Database(entities = [Favorite::class, LocalPlaylist::class, PlaylistSong::class, PlayStat::class, com.example.localbeat.data.model.DownloadItem::class], version = 2, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun library(): LibraryDao
    abstract fun downloads(): com.example.localbeat.downloader.DownloadDao
    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS DownloadItem (id TEXT NOT NULL PRIMARY KEY, url TEXT NOT NULL, title TEXT NOT NULL, thumbnail TEXT, format TEXT NOT NULL, quality INTEGER NOT NULL, status TEXT NOT NULL, progress REAL, downloadedBytes INTEGER, totalBytes INTEGER, stage TEXT NOT NULL, error TEXT, finalUri TEXT, createdAt INTEGER NOT NULL, completedAt INTEGER, artist TEXT, album TEXT, local INTEGER NOT NULL, pendingUri TEXT)")
            }
        }
        @Volatile private var instance: LibraryDatabase? = null
        fun get(context: Context): LibraryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LibraryDatabase::class.java, "localbeat-library.db")
                .addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
