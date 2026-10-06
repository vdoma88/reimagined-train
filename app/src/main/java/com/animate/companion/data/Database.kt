package com.animate.companion.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.animate.companion.model.Appearance
import com.animate.companion.model.Gender
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gender: String,
    val appearanceJson: String,
    val archetypeId: String,
    val professionId: String,
    val directionId: String,
    /** Free-form extra details the user typed in the creator. */
    val extraNote: String = "",
    /** Rolling summary of older parts of the conversation. */
    val memory: String = "",
    /** Messages with id <= this value are already folded into [memory]. */
    val summarizedUntilId: Long = 0,
    /** 0..100, grows as you talk. Purely cosmetic, but also hinted to the model. */
    val affection: Int = 10,
    val voiceSeed: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis(),
    val lastMessagePreview: String = "",
    val lastEmotion: String = "neutral",
) {
    val genderEnum: Gender get() = runCatching { Gender.valueOf(gender) }.getOrDefault(Gender.FEMALE)
        .let { if (it == Gender.NEUTRAL) Gender.FEMALE else it }
    val appearance: Appearance get() = Appearance.fromJson(appearanceJson)
}

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("characterId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long,
    /** "user" or "assistant". */
    val role: String,
    val text: String,
    val emotion: String = "neutral",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isUser get() = role == ROLE_USER

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY lastMessageAt DESC")
    fun observeAll(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    fun observe(id: Long): Flow<CharacterEntity?>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun get(id: Long): CharacterEntity?

    @Insert
    suspend fun insert(c: CharacterEntity): Long

    @Update
    suspend fun update(c: CharacterEntity)

    @Query("UPDATE characters SET appearanceJson = :appearanceJson WHERE id = :id")
    suspend fun updateAppearance(id: Long, appearanceJson: String)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE characterId = :characterId ORDER BY id ASC")
    fun observe(characterId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE characterId = :characterId ORDER BY id ASC")
    suspend fun all(characterId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE characterId = :characterId AND id > :afterId ORDER BY id ASC")
    suspend fun after(characterId: Long, afterId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE characterId = :characterId ORDER BY id DESC LIMIT 1")
    suspend fun last(characterId: Long): MessageEntity?

    @Insert
    suspend fun insert(m: MessageEntity): Long

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM messages WHERE characterId = :characterId")
    suspend fun clear(characterId: Long)
}

@Database(entities = [CharacterEntity::class, MessageEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characters(): CharacterDao
    abstract fun messages(): MessageDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "animate.db").build()
    }
}
