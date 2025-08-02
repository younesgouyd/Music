package dev.younesgouyd.apps.music.common

import dev.younesgouyd.apps.music.common.data.RepoStore

expect class ImportFolderUseCase(
    repoStore: RepoStore,
    saveMp3FileAsTrackUseCase: SaveMp3FileAsTrackUseCase
) {
    val repoStore: RepoStore
    val saveMp3FileAsTrackUseCase: SaveMp3FileAsTrackUseCase
    suspend fun execute(uri: String)
}