package com.agileburo.anytype.domain.block.interactor

import com.agileburo.anytype.domain.base.BaseUseCase
import com.agileburo.anytype.domain.base.Either
import com.agileburo.anytype.domain.block.model.Block.Content.Text
import com.agileburo.anytype.domain.block.model.Command
import com.agileburo.anytype.domain.block.repo.BlockRepository
import com.agileburo.anytype.domain.common.Id

/**
 * Use-case for udpating a block's text style
 */
class UpdateTextStyle(
    private val repo: BlockRepository
) : BaseUseCase<Unit, UpdateTextStyle.Params>() {

    override suspend fun run(params: Params) = try {
        repo.updateTextStyle(
            command = Command.UpdateStyle(
                style = params.style,
                context = params.context,
                target = params.target
            )
        ).let {
            Either.Right(it)
        }
    } catch (t: Throwable) {
        Either.Left(t)
    }

    /**
     * @property context context id
     * @property target id of the target block, whose style we need to update.
     * @property style new style for the target block.
     */
    data class Params(
        val context: Id,
        val target: Id,
        val style: Text.Style
    )
}