package com.example.baseproject.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.baseproject.data.database.entity.ToolUsageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Giao tiếp cơ sở dữ liệu cho các thao tác yêu thích và lịch sử công cụ.
 */
@Dao
interface ToolDao {

    // Lấy toàn bộ danh sách trạng thái của các công cụ.
    @Query("SELECT * FROM tool_usage")
    fun getAllUsage(): Flow<List<ToolUsageEntity>>

    // Lấy danh sách các công cụ được đánh dấu yêu thích.
    @Query("SELECT * FROM tool_usage WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<ToolUsageEntity>>

    // Lấy danh sách các công cụ sử dụng gần đây có thời gian lớn hơn 0.
    @Query("SELECT * FROM tool_usage WHERE lastUsedTimestamp > 0 ORDER BY lastUsedTimestamp DESC LIMIT :limit")
    fun getRecents(limit: Int = 10): Flow<List<ToolUsageEntity>>

    // Lấy bản ghi theo mã công cụ.
    @Query("SELECT * FROM tool_usage WHERE toolId = :toolId LIMIT 1")
    suspend fun getById(toolId: String): ToolUsageEntity?

    // Chèn hoặc thay thế bản ghi công cụ.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ToolUsageEntity)

    // Cập nhật trạng thái yêu thích của công cụ.
    @Transaction
    suspend fun toggleFavorite(toolId: String, isFav: Boolean) {
        val existing = getById(toolId)
        if (existing != null) {
            upsert(existing.copy(isFavorite = isFav))
        } else {
            upsert(ToolUsageEntity(toolId = toolId, isFavorite = isFav, lastUsedTimestamp = 0L))
        }
    }

    // Ghi nhận thời điểm sử dụng công cụ gần nhất.
    @Transaction
    suspend fun recordUsage(toolId: String, timestamp: Long) {
        val existing = getById(toolId)
        if (existing != null) {
            upsert(existing.copy(lastUsedTimestamp = timestamp))
        } else {
            upsert(ToolUsageEntity(toolId = toolId, isFavorite = false, lastUsedTimestamp = timestamp))
        }
    }
}
