package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room3.Dao
import androidx.room3.Query
import dev.younesgouyd.apps.music.common.models.HomeItemType
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.datamodels.home.HomeFeedStub

@Dao
abstract class HomeDao {
    @Query(
        """
        SELECT * FROM (
            -- 1. TRACKS (supports name search + tag filter)
            SELECT 
                'TRACK' AS itemType,
                t.id AS itemId,
                t.creationDatetime AS creationDatetime
            FROM track t
            JOIN importsessionitem i ON i.id = t.importSessionItemId
            LEFT JOIN spotifytrack sp ON sp.id = t.spotifyTrackId
            WHERE (:filterByTypes = 0 OR 'TRACK' IN (:allowedTypes))
              AND (
                  :query = '' 
                  OR (sp.name IS NOT NULL AND sp.name LIKE '%' || :query || '%')
                  OR (sp.name IS NULL AND i.title IS NOT NULL AND i.title LIKE '%' || :query || '%')
              )
              AND (
                  :tagCount = 0 
                  OR t.id IN (
                      SELECT tt.trackId 
                      FROM tagtrackcrossref tt 
                      WHERE tt.tagId IN (:tagIds)
                      -- Optional: use GROUP BY tt.trackId HAVING COUNT(DISTINCT tt.tagId) = :tagCount for AND semantics
                  )
              )

            UNION ALL

            -- 2. PLAYLISTS (excluded automatically when :tagCount > 0)
            SELECT 
                'PLAYLIST' AS itemType,
                p.id AS itemId,
                p.creationDatetime AS creationDatetime
            FROM playlist p
            WHERE :tagCount = 0
              AND (:filterByTypes = 0 OR 'PLAYLIST' IN (:allowedTypes))
              AND (:query = '' OR p.name LIKE '%' || :query || '%')

            UNION ALL

            -- 3. ALBUMS
            SELECT 
                'ALBUM' AS itemType,
                a.id AS itemId,
                a.creationDatetime AS creationDatetime
            FROM spotifyalbum a
            WHERE :tagCount = 0
              AND (:filterByTypes = 0 OR 'ALBUM' IN (:allowedTypes))
              AND (:query = '' OR a.name LIKE '%' || :query || '%')

            UNION ALL

            -- 4. ARTISTS
            SELECT 
                'ARTIST' AS itemType,
                ar.id AS itemId,
                ar.creationDatetime AS creationDatetime
            FROM spotifyartist ar
            WHERE :tagCount = 0
              AND (:filterByTypes = 0 OR 'ARTIST' IN (:allowedTypes))
              AND (:query = '' OR ar.name LIKE '%' || :query || '%')

            UNION ALL

            -- 5. TAGS
            SELECT 
                'TAG' AS itemType,
                tg.id AS itemId,
                tg.creationDatetime AS creationDatetime
            FROM tag tg
            WHERE :tagCount = 0
              AND (:filterByTypes = 0 OR 'TAG' IN (:allowedTypes))
              AND (:query = '' OR tg.name LIKE '%' || :query || '%')
        ) feed
        WHERE (
            :cursorDatetime IS NULL
            OR feed.creationDatetime < :cursorDatetime
            OR (feed.creationDatetime = :cursorDatetime AND feed.itemType > :cursorType)
            OR (feed.creationDatetime = :cursorDatetime AND feed.itemType = :cursorType AND feed.itemId < :cursorId)
        )
        ORDER BY feed.creationDatetime DESC, feed.itemType ASC, feed.itemId DESC
        LIMIT :limit
        """
    )
    abstract suspend fun getFeedPage(
        query: String,
        filterByTypes: Boolean, // false when user selected no type chips (show all)
        allowedTypes: Set<HomeItemType>,
        tagCount: Int,          // tagIds.size (0 = no tag filter)
        tagIds: Set<TagId>,
        cursorDatetime: Long?,  // null on first page
        cursorType: HomeItemType?,
        cursorId: Long?,
        limit: Int
    ): List<HomeFeedStub>
}