package com.example.localbeat.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import com.example.localbeat.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

interface MusicRepository {
    fun getSongs(): Flow<List<Song>>
    suspend fun getSongById(id: String): Song?
}

/** Queries Android's indexed music library. No filesystem traversal is performed. */
class MusicRepositoryImpl(private val contentResolver: ContentResolver) : MusicRepository {
    override fun getSongs(): Flow<List<Song>> = flow {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val songs = mutableListOf<Song>()
        val cursor = contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC, ${MediaStore.Audio.Media._ID} ASC",
        ) ?: error("The music library is currently unavailable.")
        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (it.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val id = it.getLong(idColumn)
                songs += Song(
                    id = id.toString(),
                    albumId = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)),
                    artistId = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)),
                    dateAdded = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)),
                    title = it.getString(titleColumn).metadataOr("Unknown title"),
                    artist = it.getString(artistColumn).metadataOr("Unknown artist"),
                    album = it.getString(albumColumn).metadataOr("Unknown album"),
                    durationMs = it.getLong(durationColumn),
                    albumArtUri = ContentUris.withAppendedId(
                        android.net.Uri.parse("content://media/external/audio/albumart"),
                        it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)),
                    ).toString(),
                    contentUri = ContentUris.withAppendedId(collection, id),
                )
            }
        }
        emit(songs.toList())
    }.flowOn(Dispatchers.IO)

    override suspend fun getSongById(id: String): Song? = getSongs().first().find { it.id == id }

    private fun String?.metadataOr(fallback: String): String =
        if (isNullOrBlank() || this == MediaStore.UNKNOWN_STRING) fallback else this
}
