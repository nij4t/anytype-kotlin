package com.anytypeio.anytype.presentation.sets.subscription

import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.domain.objects.StoreOfRelations
import com.anytypeio.anytype.domain.search.DataViewSubscriptionContainer
import com.anytypeio.anytype.domain.search.DataViewState
import com.anytypeio.anytype.presentation.relations.ObjectSetConfig
import com.anytypeio.anytype.presentation.search.ObjectSearchConstants
import com.anytypeio.anytype.presentation.sets.filterOutDeletedAndMissingObjects
import com.anytypeio.anytype.presentation.sets.getSetOfValue
import com.anytypeio.anytype.presentation.sets.state.ObjectState
import com.anytypeio.anytype.presentation.sets.updateFormatForSubscription
import com.anytypeio.anytype.presentation.sets.viewerById
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import timber.log.Timber

interface DataViewSubscription {

    suspend fun startObjectSetSubscription(
        context: Id,
        workspaceId: Id,
        state: ObjectState.DataView.Set,
        currentViewerId: Id?,
        offset: Long,
        storeOfRelations: StoreOfRelations
    ): Flow<DataViewState>

    suspend fun startObjectCollectionSubscription(
        context: Id,
        collection: Id,
        workspaceId: Id,
        state: ObjectState.DataView.Collection,
        currentViewerId: Id?,
        offset: Long,
        storeOfRelations: StoreOfRelations
    ): Flow<DataViewState>
}

class DefaultDataViewSubscription(
    private val dataViewSubscriptionContainer: DataViewSubscriptionContainer
) : DataViewSubscription {

    override suspend fun startObjectCollectionSubscription(
        context: Id,
        collection: Id,
        workspaceId: Id,
        state: ObjectState.DataView.Collection,
        currentViewerId: Id?,
        offset: Long,
        storeOfRelations: StoreOfRelations
    ): Flow<DataViewState> {
        if (context.isEmpty() || collection.isEmpty()) {
            Timber.w("Data view collection subscription: context or collection is empty")
            return emptyFlow()
        }
        val activeViewer = state.viewerById(currentViewerId)
        if (activeViewer == null) {
            Timber.w("Data view collection subscription: active viewer is null")
            return emptyFlow()
        }
        val filters =
            activeViewer.filters.updateFormatForSubscription(storeOfRelations) + ObjectSearchConstants.defaultDataViewFilters(
                workspaceId = workspaceId
            )
        val dataViewLinksKeys = state.dataViewContent.relationLinks.map { it.key }
        val keys = ObjectSearchConstants.defaultDataViewKeys + dataViewLinksKeys

        val params = DataViewSubscriptionContainer.Params(
            collection = collection,
            subscription = getSubscriptionId(context),
            sorts = activeViewer.sorts,
            filters = filters,
            sources = listOf(),
            keys = keys,
            limit = ObjectSetConfig.DEFAULT_LIMIT,
            offset = offset
        )
        return dataViewSubscriptionContainer.observe(params)
    }

    override suspend fun startObjectSetSubscription(
        context: Id,
        workspaceId: Id,
        state: ObjectState.DataView.Set,
        currentViewerId: Id?,
        offset: Long,
        storeOfRelations: StoreOfRelations
    ): Flow<DataViewState> {
        if (context.isEmpty()) {
            Timber.w("Data view set subscription: context is empty")
            return emptyFlow()
        }
        val activeViewer = state.viewerById(currentViewerId)
        if (activeViewer == null) {
            Timber.w("Data view set subscription: active viewer is null")
            return emptyFlow()
        }

        val setOfValue = state.getSetOfValue(ctx = context)
        if (setOfValue.isEmpty()) {
            Timber.w("Data view set subscription: setOf value is empty, proceed without subscription")
            return emptyFlow()
        }

        val query = state.filterOutDeletedAndMissingObjects(setOfValue)
        if (query.isEmpty()) {
            Timber.w(
                "Data view set subscription: query has no valid types or relations, " +
                        "proceed without subscription"
            )
            return emptyFlow()
        }

        val filters =
            activeViewer.filters.updateFormatForSubscription(storeOfRelations) + ObjectSearchConstants.defaultDataViewFilters(
                workspaceId = workspaceId
            )
        val dataViewLinksKeys = state.dataViewContent.relationLinks.map { it.key }
        val keys = ObjectSearchConstants.defaultDataViewKeys + dataViewLinksKeys

        val params = DataViewSubscriptionContainer.Params(
            subscription = getSubscriptionId(context),
            sorts = activeViewer.sorts,
            filters = filters,
            sources = query,
            keys = keys,
            limit = ObjectSetConfig.DEFAULT_LIMIT,
            offset = offset
        )
        return dataViewSubscriptionContainer.observe(params)
    }

    companion object {
        const val DATA_VIEW_SUBSCRIPTION_POSTFIX = "-dataview"

        fun getSubscriptionId(context: Id) = "$context$DATA_VIEW_SUBSCRIPTION_POSTFIX"
    }
}