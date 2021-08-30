package com.anytypeio.anytype.presentation.moving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anytypeio.anytype.analytics.base.Analytics
import com.anytypeio.anytype.core_models.*
import com.anytypeio.anytype.domain.block.interactor.sets.GetObjectTypes
import com.anytypeio.anytype.domain.config.GetFlavourConfig
import com.anytypeio.anytype.domain.dataview.interactor.SearchObjects
import com.anytypeio.anytype.domain.misc.UrlBuilder
import com.anytypeio.anytype.presentation.navigation.DefaultObjectView
import com.anytypeio.anytype.presentation.objects.ObjectIcon
import com.anytypeio.anytype.presentation.objects.SupportedLayouts
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

class MoveToViewModel(
    urlBuilder: UrlBuilder,
    private val searchObjects: SearchObjects,
    private val getObjectTypes: GetObjectTypes,
    private val analytics: Analytics,
    private val getFlavourConfig: GetFlavourConfig
) : ViewModel() {

    private val _viewState: MutableStateFlow<MoveToView> = MutableStateFlow(MoveToView.Init)
    val viewState: StateFlow<MoveToView> get() = _viewState

    val commands = MutableSharedFlow<Command>(replay = 0)
    private val userInput = MutableStateFlow(EMPTY_QUERY)
    private val searchQuery = userInput.take(1).onCompletion {
        emitAll(userInput.debounce(DEBOUNCE_DURATION).distinctUntilChanged())
    }

    val types = MutableStateFlow(emptyList<ObjectType>())
    val objects = MutableStateFlow(emptyList<ObjectWrapper.Basic>())

    init {
        viewModelScope.launch {
            combine(objects, types) { listOfObjects, listOfTypes ->
                listOfObjects.map { obj ->
                    val targetType = listOfTypes.find { type ->
                        obj.type.contains(type.url)
                    }
                    DefaultObjectView(
                        id = obj.id,
                        name = obj.name.orEmpty(),
                        typeName = targetType?.name.orEmpty(),
                        typeLayout = obj.layout,
                        icon = ObjectIcon.from(
                            obj = obj,
                            layout = obj.layout,
                            builder = urlBuilder
                        )
                    )
                }
            }.collectLatest { views ->
                if (views.isNotEmpty())
                    _viewState.value = MoveToView.Success(views)
                else
                    _viewState.value = MoveToView.NoResults(userInput.value)
            }
        }
    }

    fun onStart() {
        getObjectTypes()
    }

    private fun startProcessingSearchQuery() {
        viewModelScope.launch {
            searchQuery.collectLatest { query ->
                val params = getSearchObjectsParams().copy(fulltext = query)
                searchObjects(params = params).process(
                    success = { raw -> setObjects(raw.map { ObjectWrapper.Basic(it) }) },
                    failure = { Timber.e(it, "Error while searching for objects") }
                )
            }
        }
    }

    private fun getObjectTypes() {
        viewModelScope.launch {
            val params = GetObjectTypes.Params(filterArchivedObjects = true)
            getObjectTypes.invoke(params).process(
                failure = { Timber.e(it, "Error while getting object types") },
                success = {
                    types.value = it
                    startProcessingSearchQuery()
                }
            )
        }
    }

    private fun getSearchObjectsParams(): SearchObjects.Params {

        val filteredTypes = if (getFlavourConfig.isDataViewEnabled()) {
            types.value
                .filter { objectType -> objectType.smartBlockTypes.contains(SmartBlockType.PAGE) }
                .map { objectType -> objectType.url }
        } else {
            listOf(ObjectTypeConst.PAGE)
        }

        val filters = listOf(
            DVFilter(
                condition = DVFilterCondition.EQUAL,
                value = false,
                relationKey = Relations.IS_ARCHIVED,
                operator = DVFilterOperator.AND
            ),
            DVFilter(
                relationKey = Relations.IS_HIDDEN,
                condition = DVFilterCondition.NOT_EQUAL,
                value = true
            ),
            DVFilter(
                relationKey = Relations.IS_READ_ONLY,
                condition = DVFilterCondition.NOT_EQUAL,
                value = true
            )
        )

        val sorts = listOf(
            DVSort(
                relationKey = Relations.NAME,
                type = DVSortType.ASC
            )
        )

        return SearchObjects.Params(
            limit = SEARCH_LIMIT,
            objectTypeFilter = filteredTypes,
            filters = filters,
            sorts = sorts,
            fulltext = EMPTY_QUERY
        )
    }

    fun onObjectClicked(target: Id, layout: ObjectType.Layout?) {
        viewModelScope.launch {
            commands.emit(Command.Move(target = target))
        }
    }

    fun onDialogCancelled() {
        viewModelScope.launch {
            commands.emit(Command.Exit)
        }
    }

    fun onSearchTextChanged(searchText: String) {
        userInput.value = searchText
    }

    fun setObjects(data: List<ObjectWrapper.Basic>) {
        objects.value = data.filter {
            SupportedLayouts.layouts.contains(it.layout)
        }
    }

    sealed class Command {
        object Exit : Command()
        data class Move(val target: Id) : Command()
    }

    companion object {
        const val EMPTY_QUERY = ""
        const val DEBOUNCE_DURATION = 300L
        const val SEARCH_LIMIT = 200
    }
}