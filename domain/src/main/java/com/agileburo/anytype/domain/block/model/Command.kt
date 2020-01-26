package com.agileburo.anytype.domain.block.model

import com.agileburo.anytype.domain.common.Id

sealed class Command {

    /**
     * @property contextId context id
     * @property blockId target block id
     * @property text updated text
     * @property marks marks of the updated text
     */
    class UpdateText(
        val contextId: Id,
        val blockId: Id,
        val text: String,
        val marks: List<Block.Content.Text.Mark>
    )

    /**
     * @property context context id
     * @property target id of the target checkbox block
     * @property isChecked new checked/unchecked state for this checkbox block
     */
    class UpdateCheckbox(
        val context: Id,
        val target: Id,
        val isChecked: Boolean
    )

    /**
     * Command for updating style for one textual block.
     * @property context context id
     * @property target id of the target block, whose style we need to update.
     * @property style new style for the target block.
     */
    data class UpdateStyle(
        val context: Id,
        val target: Id,
        val style: Block.Content.Text.Style
    )

    /**
     * Command for creating a block
     * @property contextId id of the context of the block (i.e. page, dashboard or something else)
     * @property targetId id of the block associated with the block we need to create
     * @property position position of the block that we need to create in relation with the target block
     * @property prototype a prototype of the block we would like to create
     */
    class Create(
        val contextId: Id,
        val targetId: Id,
        val position: Position,
        val prototype: Block.Prototype
    )

    class Dnd(
        val contextId: Id,
        val targetId: Id,
        val targetContextId: Id,
        val blockIds: List<String>,
        val position: Position
    )

    /**
     * Command for block duplication
     * @property context context id
     * @property original id of the original block, which we need to duplicate
     */
    class Duplicate(
        val context: Id,
        val original: Id
    )

    /**
     * Command for unlinking a set of blocks from its context (i.e. page)
     * @property context context id
     * @property targets ids of the blocks, which we need to unlink from its [context]
     */
    class Unlink(
        val context: Id,
        val targets: List<Id>
    )
}