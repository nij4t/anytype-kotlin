package com.agileburo.anytype.middleware.block

import anytype.Events
import anytype.model.Models
import anytype.model.Models.Block.Content.Dashboard
import anytype.model.Models.Block.Content.Page
import com.agileburo.anytype.data.auth.model.BlockEntity
import com.agileburo.anytype.data.auth.model.ConfigEntity
import com.agileburo.anytype.data.auth.repo.block.BlockRemote
import com.agileburo.anytype.middleware.EventProxy
import com.agileburo.anytype.middleware.interactor.Middleware
import com.google.protobuf.Value
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

class BlockMiddleware(
    private val middleware: Middleware,
    private val events: EventProxy
) : BlockRemote {

    private val supportedTextStyles = listOf(
        Models.Block.Content.Text.Style.Paragraph,
        Models.Block.Content.Text.Style.Header1,
        Models.Block.Content.Text.Style.Header2,
        Models.Block.Content.Text.Style.Header3,
        Models.Block.Content.Text.Style.Title
    )

    private val supportedContent = listOf(
        Models.Block.ContentCase.DASHBOARD,
        Models.Block.ContentCase.PAGE
    )

    override suspend fun getConfig(): ConfigEntity {
        return ConfigEntity(
            homeId = middleware.provideHomeDashboardId()
        )
    }

    override suspend fun observeBlocks() = events
        .flow()
        .filter { event ->
            event.messagesList.any { message ->
                message.valueCase == Events.Event.Message.ValueCase.BLOCKSHOW
            }
        }
        .map { event ->
            event.messagesList.filter { message ->
                message.valueCase == Events.Event.Message.ValueCase.BLOCKSHOW
            }
        }
        .flatMapConcat { event -> event.asFlow() }
        .map { event ->
            event.blockShow.blocksList
                .filter { block -> supportedContent.contains(block.contentCase) }
                .map { block ->
                    when (block.contentCase) {
                        Models.Block.ContentCase.DASHBOARD -> {
                            BlockEntity(
                                id = block.id,
                                children = block.childrenIdsList.toList(),
                                fields = extractFields(block),
                                content = extractDashboard(block)
                            )
                        }
                        Models.Block.ContentCase.PAGE -> {
                            BlockEntity(
                                id = block.id,
                                children = block.childrenIdsList.toList(),
                                fields = extractFields(block),
                                content = extractPage(block)
                            )
                        }
                        else -> {
                            throw IllegalStateException("Unexpected content: ${block.contentCase}")
                        }
                    }

                }
        }

    override suspend fun observePages() = events
        .flow()
        .filter { event ->
            event.messagesList.any { message ->
                message.valueCase == Events.Event.Message.ValueCase.BLOCKSHOW
            }
        }
        .map { event ->
            event.messagesList.filter { message ->
                message.valueCase == Events.Event.Message.ValueCase.BLOCKSHOW
            }
        }
        .flatMapConcat { event -> event.asFlow() }
        .map { event ->
            event.blockShow.blocksList
                .filter { block -> block.contentCase == Models.Block.ContentCase.TEXT }
                .filter { block -> supportedTextStyles.contains(block.text.style) }
                .map { block ->
                    BlockEntity(
                        id = block.id,
                        children = block.childrenIdsList.toList(),
                        fields = extractFields(block),
                        content = extractText(block)
                    )
                }
        }

    private fun extractDashboard(block: Models.Block): BlockEntity.Content.Dashboard {
        return BlockEntity.Content.Dashboard(
            type = when {
                block.dashboard.style == Dashboard.Style.Archive -> {
                    BlockEntity.Content.Dashboard.Type.ARCHIVE
                }
                block.dashboard.style == Dashboard.Style.MainScreen -> {
                    BlockEntity.Content.Dashboard.Type.MAIN_SCREEN
                }
                else -> throw IllegalStateException("Unexpected dashboard style: ${block.dashboard.style}")
            }
        )
    }

    private fun extractPage(block: Models.Block): BlockEntity.Content.Page {
        return BlockEntity.Content.Page(
            style = when {
                block.page.style == Page.Style.Empty -> {
                    BlockEntity.Content.Page.Style.EMPTY
                }
                block.page.style == Page.Style.Task -> {
                    BlockEntity.Content.Page.Style.TASK
                }
                block.page.style == Page.Style.Set -> {
                    BlockEntity.Content.Page.Style.SET
                }
                else -> throw IllegalStateException("Unexpected page style: ${block.page.style}")
            }
        )
    }

    private fun extractText(block: Models.Block): BlockEntity.Content.Text {
        return BlockEntity.Content.Text(
            text = block.text.text,
            marks = block.text.marks.marksList.map { mark ->
                BlockEntity.Content.Text.Mark(
                    range = IntRange(mark.range.from, mark.range.to),
                    // TODO parse parameter
                    param = null,
                    type = when (mark.type) {
                        Models.Block.Content.Text.Mark.Type.Bold -> {
                            BlockEntity.Content.Text.Mark.Type.BOLD
                        }
                        Models.Block.Content.Text.Mark.Type.Italic -> {
                            BlockEntity.Content.Text.Mark.Type.ITALIC
                        }
                        Models.Block.Content.Text.Mark.Type.Strikethrough -> {
                            BlockEntity.Content.Text.Mark.Type.STRIKETHROUGH
                        }
                        Models.Block.Content.Text.Mark.Type.Underscored -> {
                            BlockEntity.Content.Text.Mark.Type.UNDERSCORED
                        }
                        Models.Block.Content.Text.Mark.Type.Keyboard -> {
                            BlockEntity.Content.Text.Mark.Type.KEYBOARD
                        }
                        Models.Block.Content.Text.Mark.Type.TextColor -> {
                            BlockEntity.Content.Text.Mark.Type.TEXT_COLOR
                        }
                        Models.Block.Content.Text.Mark.Type.BackgroundColor -> {
                            BlockEntity.Content.Text.Mark.Type.BACKGROUND_COLOR
                        }
                        else -> throw IllegalStateException("Unexpected mark type: ${mark.type.name}")
                    }
                )
            },
            style = when (block.text.style) {
                Models.Block.Content.Text.Style.Paragraph -> BlockEntity.Content.Text.Style.P
                Models.Block.Content.Text.Style.Header1 -> BlockEntity.Content.Text.Style.H1
                Models.Block.Content.Text.Style.Header2 -> BlockEntity.Content.Text.Style.H2
                Models.Block.Content.Text.Style.Header3 -> BlockEntity.Content.Text.Style.H3
                Models.Block.Content.Text.Style.Title -> BlockEntity.Content.Text.Style.TITLE
                Models.Block.Content.Text.Style.Quote -> BlockEntity.Content.Text.Style.QUOTE
                else -> TODO()
            }
        )
    }

    override suspend fun openDashboard(contextId: String, id: String) {
        middleware.openDashboard(contextId, id)
    }

    override suspend fun closeDashboard(id: String) {
        middleware.closeDashboard(id)
    }

    override suspend fun createPage(parentId: String) = middleware.createPage(parentId)

    override suspend fun openPage(id: String) {
        middleware.openBlock(id)
    }

    override suspend fun closePage(id: String) {
        middleware.closePage(id)
    }

    private fun extractFields(block: Models.Block): BlockEntity.Fields {
        return BlockEntity.Fields().also { fields ->
            block.fields.fieldsMap.mapValues { (key, value) ->
                fields.map[key] = when (val case = value.kindCase) {
                    Value.KindCase.NUMBER_VALUE -> value.numberValue
                    Value.KindCase.STRING_VALUE -> value.stringValue
                    else -> throw IllegalStateException("$case is not supported.")
                }
            }
        }
    }
}