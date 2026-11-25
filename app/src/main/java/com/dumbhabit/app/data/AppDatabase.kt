package com.dumbhabit.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val reminderMinutes: Int? = null,
    val daysMask: Int = EVERY_DAY,
    val position: Int = 0,
)

@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("date")],
)
data class HabitLog(val habitId: Long, val date: String)

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val reminderMinutes: Int? = null,
    val daysMask: Int = EVERY_DAY,
    val position: Int = 0,
)

@Entity(
    tableName = "routine_steps",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineId")],
)
data class RoutineStep(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val position: Int,
    val name: String,
    /** null = pas de minuteur, simple chronomètre qui compte. */
    val durationSeconds: Int? = null,
)

@Entity(
    tableName = "routine_logs",
    primaryKeys = ["routineId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("date")],
)
data class RoutineLog(val routineId: Long, val date: String)

@Dao
abstract class AppDao {

    // ---- Habitudes -------------------------------------------------------

    @Query("SELECT * FROM habits ORDER BY position, id")
    abstract fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits")
    abstract suspend fun getHabits(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    abstract suspend fun getHabit(id: Long): Habit?

    @Upsert
    abstract suspend fun upsertHabit(habit: Habit): Long

    @Query("DELETE FROM habits WHERE id = :id")
    abstract suspend fun deleteHabit(id: Long)

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    abstract fun observeHabitLogs(date: String): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertHabitLog(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    abstract suspend fun deleteHabitLog(habitId: Long, date: String)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date")
    abstract suspend fun getHabitLog(habitId: Long, date: String): HabitLog?

    // ---- Routines --------------------------------------------------------

    @Query("SELECT * FROM routines ORDER BY position, id")
    abstract fun observeRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines")
    abstract suspend fun getRoutines(): List<Routine>

    @Query("SELECT * FROM routines WHERE id = :id")
    abstract suspend fun getRoutine(id: Long): Routine?

    @Query("DELETE FROM routines WHERE id = :id")
    abstract suspend fun deleteRoutine(id: Long)

    @Upsert
    abstract suspend fun upsertRoutine(routine: Routine): Long

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY position")
    abstract fun observeSteps(routineId: Long): Flow<List<RoutineStep>>

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY position")
    abstract suspend fun getSteps(routineId: Long): List<RoutineStep>

    @Query("DELETE FROM routine_steps WHERE routineId = :routineId")
    abstract suspend fun deleteSteps(routineId: Long)

    @Insert
    abstract suspend fun insertSteps(steps: List<RoutineStep>)

    @Query("SELECT * FROM routine_logs WHERE date = :date")
    abstract fun observeRoutineLogs(date: String): Flow<List<RoutineLog>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertRoutineLog(log: RoutineLog)

    /** Enregistre une routine et remplace toutes ses étapes d'un coup. Renvoie son id. */
    @Transaction
    open suspend fun saveRoutine(routine: Routine, steps: List<RoutineStep>): Long {
        val id = if (routine.id == 0L) {
            upsertRoutine(routine)
        } else {
            upsertRoutine(routine)
            routine.id
        }
        deleteSteps(id)
        insertSteps(steps.mapIndexed { i, s -> s.copy(id = 0, routineId = id, position = i) })
        return id
    }
}

@Database(
    entities = [Habit::class, HabitLog::class, Routine::class, RoutineStep::class, RoutineLog::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "dumbhabit.db").build()
    }
}
