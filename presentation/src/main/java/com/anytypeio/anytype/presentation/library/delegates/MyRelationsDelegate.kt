package com.anytypeio.anytype.presentation.library.delegates

import com.anytypeio.anytype.core_models.DVFilter
import com.anytypeio.anytype.core_models.DVFilterCondition
import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.core_models.Relations
import com.anytypeio.anytype.domain.base.AppCoroutineDispatchers
import com.anytypeio.anytype.domain.library.StoreSearchParams
import com.anytypeio.anytype.domain.library.StorelessSubscriptionContainer
import com.anytypeio.anytype.domain.misc.UrlBuilder
import com.anytypeio.anytype.domain.workspace.WorkspaceManager
import com.anytypeio.anytype.presentation.library.LibraryListDelegate
import com.anytypeio.anytype.presentation.library.LibraryScreenState
import com.anytypeio.anytype.presentation.library.LibraryView
import com.anytypeio.anytype.presentation.library.QueryListenerMyRelations
import com.anytypeio.anytype.presentation.library.filterByQuery
import com.anytypeio.anytype.presentation.objects.toLibraryViews
import com.anytypeio.anytype.presentation.search.ObjectSearchConstants
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow

class MyRelationsDelegate @Inject constructor(
    private val container: StorelessSubscriptionContainer,
    private val workspaceManager: WorkspaceManager,
    private val urlBuilder: UrlBuilder,
    private val dispatchers: AppCoroutineDispatchers
) : LibraryListDelegate, QueryListenerMyRelations {

    override val queryFlow: MutableStateFlow<String> = MutableStateFlow("")

    override fun onQueryMyRelations(string: String) {
        queryFlow.value = string
    }

    @FlowPreview
    override val itemsFlow: Flow<LibraryScreenState.Tabs.TabData> = combine(
        itemsFlow(),
        queryFlow()
    ) { items, query ->
        LibraryScreenState.Tabs.TabData(
            items
                .toLibraryViews(urlBuilder)
                .filterByQuery(query)
                .optAddCreateRelationView(query)
        )
    }

    @FlowPreview
    override fun itemsFlow() = flow {
        emit(workspaceManager.getCurrentWorkspace())
    }.flatMapMerge {
        val searchParams = buildSearchParams(it)
        container.subscribe(searchParams)
    }

    private fun buildSearchParams(workspaceId: Id): StoreSearchParams {
        return StoreSearchParams(
            subscription = SUB_LIBRARY_MY_RELATIONS,
            keys = ObjectSearchConstants.defaultKeys + listOf(
                Relations.SOURCE_OBJECT,
                Relations.RELATION_FORMAT,
                Relations.RESTRICTIONS
            ),
            filters = buildList {
                addAll(ObjectSearchConstants.filterMyRelations())
                add(
                    DVFilter(
                        relation = Relations.WORKSPACE_ID,
                        condition = DVFilterCondition.EQUAL,
                        value = workspaceId
                    )
                )
                add(
                    DVFilter(
                        relation = Relations.RELATION_KEY,
                        condition = DVFilterCondition.NOT_IN,
                        value = Relations.systemRelationKeys.filter { key ->
                            key != Relations.DESCRIPTION
                        }
                    )
                )
            }
        )
    }

    override suspend fun unsubscribe() = with(dispatchers.io) {
        container.unsubscribe(listOf(SUB_LIBRARY_MY_RELATIONS))
    }

}

private fun List<LibraryView>.optAddCreateRelationView(query: String): List<LibraryView> {
    val q = query.trim()
    val result = this
    return if (q.isNotEmpty() && result.none { it.name.lowercase() == q.lowercase() }) {
        buildList {
            add(LibraryView.CreateNewRelationView(name = q))
            addAll(result)
        }
    } else {
        result
    }
}

private const val SUB_LIBRARY_MY_RELATIONS = "subscription.library_my_relations"