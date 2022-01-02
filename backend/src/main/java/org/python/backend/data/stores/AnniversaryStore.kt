package org.python.backend.data.stores

import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.python.backend.data.datatype.Anniversary
import org.python.db.CompanionDatabase
import org.python.db.entities.RoomAnniversary

class AnniversaryStore(database: CompanionDatabase) {
    private val anniversaryDao = database.anniversaryDao

    fun getAllAnniversaries(): Flow<PagingData<Anniversary>> = pagingAnniversary { anniversaryDao.getAll() }

    suspend fun getByName(name: String): Anniversary? = anniversaryDao.getByName(name)?.toUI()

    suspend fun add(anniversary: Anniversary): Boolean {
        return try {
            anniversaryDao.add(anniversary.toRoom());
            true;
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            false;
        } catch (e: Exception) {
            false;
        }
    }

    suspend fun upsert(anniversary: Anniversary): Unit = anniversaryDao.upsert(anniversary.toRoom())

    suspend fun update(anniversary: Anniversary) = anniversaryDao.update(anniversary.toRoom())

    suspend fun delete(anniversary: Anniversary) = anniversaryDao.delete(anniversary.toRoom())
}

private fun pagingAnniversary(block: () -> PagingSource<Int, RoomAnniversary>): Flow<PagingData<Anniversary>> =
    Pager(PagingConfig(pageSize = 20)) { block() }.flow.map { page -> page.map(RoomAnniversary::toUI) }


private fun Anniversary.toRoom() = RoomAnniversary(
    name = name,
    duration = duration,
    lastReported = lastReported
)

private fun RoomAnniversary.toUI() = Anniversary(
    name = name,
    duration = duration,
    lastReported = lastReported
)