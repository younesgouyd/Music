package dev.younesgouyd.apps.music.client.multiplatform.data

import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.ResultType
import dev.younesgouyd.apps.music.common.models.Rpc
import kotlinx.coroutines.flow.Flow
import java.io.File

abstract class Server {
    abstract suspend fun <TRpc> call(rpc: TRpc) where TRpc: Rpc.ToServer, TRpc: ResultType.Action

    abstract suspend fun <TRpc, TResult> get(rpc: TRpc): TResult where TRpc: Rpc.ToServer, TRpc: ResultType.Data.Static<TResult>

    abstract fun <TRpc, TResult> observe(rpc: TRpc): Flow<TResult> where TRpc: Rpc.ToServer, TRpc: ResultType.Data.Dynamic<TResult>

    abstract suspend fun getFile(id: MediaFileId): File
}