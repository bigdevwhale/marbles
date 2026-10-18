package app.marbles.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** A child, and the jar of their Saturdays. */
@Entity(tableName = "children")
data class Child(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** [LocalDate.toEpochDay] of the birthday. */
    val born: Long,
    /** "GIRL", "BOY" or empty: only for the grammar of a few sentences. */
    val gender: String = "",
    /** Which glass is theirs, an index into the palette. */
    val glass: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val birthday: LocalDate get() = LocalDate.ofEpochDay(born)
    val girl: Boolean get() = gender == "GIRL"
    val boy: Boolean get() = gender == "BOY"
}

/** What the time together was; each has its own marble colour. */
enum class Kind { PLAY, OUTSIDE, TALK, BOOK, KITCHEN, MAKE, SPORT, ROUTINE, TRIP }

/**
 * Time spent with one child or a few. [kids] is the ids as ",1,3,". A timed one ([timed]) was
 * measured with the phone put away, and [phoneMinutes] is how long the phone was picked up meanwhile.
 */
@Entity(tableName = "together", indices = [Index("start")])
data class Together(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kids: String,
    val start: Long,
    /** Minutes that count: for a timed one, without the minutes on the phone. */
    val minutes: Int,
    val timed: Boolean = false,
    val phoneMinutes: Int = 0,
    /** [Kind] name. */
    val kind: String = Kind.PLAY.name,
    val note: String = "",
    /** Something the child said. */
    val quote: String = "",
    /** A picture, a file in the vault, or empty. */
    val photo: String = "",
    /** The idea from the deck it came from ([app.marbles.content.Ideas] key), or empty. */
    val idea: String = "",
    /** The bedtime question it answers ([app.marbles.content.Questions] key), or empty. */
    val question: String = "",
    /** Time with just this one child, no brothers or sisters. */
    val alone: Boolean = false,
    /** Something done for the first time. */
    val first: Boolean = false,
) {
    val kindOf: Kind get() = runCatching { Kind.valueOf(kind) }.getOrDefault(Kind.PLAY)
    val kidIds: List<Long> get() = kids.split(',').mapNotNull { it.toLongOrNull() }
    val end: Long get() = start + minutes * 60_000L + phoneMinutes * 60_000L
    fun of(child: Long) = ",$child," in kids

    companion object {
        fun kidsOf(ids: Collection<Long>) = ids.distinct().sorted().joinToString(",", ",", ",")
    }
}

/** What a child is into right now: dinosaurs, a friend called Petya, the dark. */
@Entity(tableName = "interests", indices = [Index("childId")])
data class Interest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val text: String,
    val since: Long,
    /** When it passed, or 0 while it's still going. */
    val until: Long = 0,
) {
    val current: Boolean get() = until == 0L
}

/** Something planned: a Saturday out, an evening with just one of them. */
@Entity(tableName = "plans", indices = [Index("day")])
data class Plan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kids: String,
    /** [LocalDate.toEpochDay]. */
    val day: Long,
    val text: String,
    val idea: String = "",
    val alone: Boolean = false,
    /** When it was done, or 0. */
    val done: Long = 0,
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(day)
    val kidIds: List<Long> get() = kids.split(',').mapNotNull { it.toLongOrNull() }
}

/**
 * One evening in the mirror: how long the phone was in use within the family hours, and which apps
 * took it ("pkg=min;pkg=min", biggest first). Usage history on the phone lasts only days, so each
 * evening is kept once it's over.
 */
@Entity(tableName = "evenings")
data class Evening(
    /** [LocalDate.toEpochDay]. */
    @PrimaryKey val day: Long,
    /** The family hours that day, minutes after midnight. */
    val from: Int,
    val to: Int,
    val phoneMinutes: Int,
    val apps: String = "",
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(day)
    val appMinutes: List<Pair<String, Int>>
        get() = apps.split(';').mapNotNull { e -> e.substringAfter('=').toIntOrNull()?.let { e.substringBefore('=') to it } }
}

@Dao
abstract class MarblesDao {
    @Query("SELECT * FROM children ORDER BY born ASC") abstract fun observeChildren(): Flow<List<Child>>
    @Query("SELECT * FROM together ORDER BY start DESC") abstract fun observeTogether(): Flow<List<Together>>
    @Query("SELECT * FROM interests ORDER BY since DESC") abstract fun observeInterests(): Flow<List<Interest>>
    @Query("SELECT * FROM plans ORDER BY day ASC") abstract fun observePlans(): Flow<List<Plan>>
    @Query("SELECT * FROM evenings ORDER BY day DESC") abstract fun observeEvenings(): Flow<List<Evening>>

    @Query("SELECT * FROM children ORDER BY born ASC") abstract suspend fun children(): List<Child>
    @Query("SELECT * FROM together ORDER BY start DESC") abstract suspend fun together(): List<Together>
    @Query("SELECT * FROM interests ORDER BY since DESC") abstract suspend fun interests(): List<Interest>
    @Query("SELECT * FROM plans ORDER BY day ASC") abstract suspend fun plans(): List<Plan>
    @Query("SELECT * FROM evenings ORDER BY day DESC") abstract suspend fun evenings(): List<Evening>
    @Query("SELECT day FROM evenings") abstract suspend fun eveningDays(): List<Long>

    @Query("SELECT * FROM together WHERE id = :id") abstract suspend fun together(id: Long): Together?

    @Insert abstract suspend fun insert(c: Child): Long
    @Update abstract suspend fun update(c: Child)
    @Delete abstract suspend fun delete(c: Child)

    @Insert abstract suspend fun insert(t: Together): Long
    @Update abstract suspend fun update(t: Together)
    @Delete abstract suspend fun delete(t: Together)

    @Insert abstract suspend fun insert(i: Interest): Long
    @Update abstract suspend fun update(i: Interest)
    @Delete abstract suspend fun delete(i: Interest)

    @Insert abstract suspend fun insert(p: Plan): Long
    @Update abstract suspend fun update(p: Plan)
    @Delete abstract suspend fun delete(p: Plan)

    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun put(e: Evening)

    @Query("DELETE FROM interests WHERE childId = :id") abstract suspend fun deleteInterestsOf(id: Long)

    @Query("DELETE FROM children") protected abstract suspend fun clearChildren()
    @Query("DELETE FROM together") protected abstract suspend fun clearTogether()
    @Query("DELETE FROM interests") protected abstract suspend fun clearInterests()
    @Query("DELETE FROM plans") protected abstract suspend fun clearPlans()
    @Query("DELETE FROM evenings") protected abstract suspend fun clearEvenings()
    @Insert protected abstract suspend fun insertChildren(l: List<Child>)
    @Insert protected abstract suspend fun insertTogether(l: List<Together>)
    @Insert protected abstract suspend fun insertInterests(l: List<Interest>)
    @Insert protected abstract suspend fun insertPlans(l: List<Plan>)
    @Insert protected abstract suspend fun insertEvenings(l: List<Evening>)

    @Transaction
    open suspend fun replaceAll(all: Everything) {
        clearChildren(); clearTogether(); clearInterests(); clearPlans(); clearEvenings()
        insertChildren(all.children); insertTogether(all.together); insertInterests(all.interests); insertPlans(all.plans); insertEvenings(all.evenings)
    }
}

/** The whole house, for backups. */
class Everything(
    val children: List<Child>,
    val together: List<Together>,
    val interests: List<Interest>,
    val plans: List<Plan>,
    val evenings: List<Evening>,
)

@Database(entities = [Child::class, Together::class, Interest::class, Plan::class, Evening::class], version = 1, exportSchema = true)
abstract class MarblesDatabase : RoomDatabase() {
    abstract fun dao(): MarblesDao

    companion object {
        fun create(context: Context): MarblesDatabase =
            Room.databaseBuilder(context.applicationContext, MarblesDatabase::class.java, "marbles.db").build()
    }
}
