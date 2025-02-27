package com.anytypeio.anytype.domain.icon

import com.anytypeio.anytype.core_models.Block
import com.anytypeio.anytype.core_models.Command
import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.domain.block.repo.BlockRepository

class SetDocumentImageIcon(
    private val repo: BlockRepository
) : SetImageIcon<Id>() {

    override suspend fun run(params: Params<Id>) = safe {
        val hash = repo.uploadFile(
            command = Command.UploadFile(
                path = params.path,
                type = Block.Content.File.Type.IMAGE
            )
        )
        val payload = repo.setDocumentImageIcon(
            command = Command.SetDocumentImageIcon(
                hash = hash,
                context = params.target
            )
        )
        Pair(payload, hash)
    }
}