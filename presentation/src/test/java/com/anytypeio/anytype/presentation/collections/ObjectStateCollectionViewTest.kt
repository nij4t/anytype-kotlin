package com.anytypeio.anytype.presentation.collections

import app.cash.turbine.testIn
import com.anytypeio.anytype.core_models.DV
import com.anytypeio.anytype.core_models.ObjectType
import com.anytypeio.anytype.core_models.ObjectTypeIds
import com.anytypeio.anytype.core_models.Relations
import com.anytypeio.anytype.core_models.StubObject
import com.anytypeio.anytype.presentation.objects.SupportedLayouts
import com.anytypeio.anytype.presentation.sets.DataViewViewState
import com.anytypeio.anytype.presentation.sets.ObjectSetViewModel
import com.anytypeio.anytype.presentation.sets.SetOrCollectionHeaderState
import com.anytypeio.anytype.presentation.sets.main.ObjectSetViewModelTestSetup
import com.anytypeio.anytype.presentation.sets.model.Viewer
import com.anytypeio.anytype.presentation.sets.state.ObjectState
import com.anytypeio.anytype.presentation.sets.state.ObjectState.Companion.VIEW_DEFAULT_OBJECT_TYPE
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.bytebuddy.utility.RandomString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class ObjectStateCollectionViewTest : ObjectSetViewModelTestSetup() {

    private lateinit var viewModel: ObjectSetViewModel
    private lateinit var mockObjectCollection: MockCollection

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = givenViewModel()
        mockObjectCollection = MockCollection(context = root)
        stubGetDefaultPageType()
    }

    @After
    fun after() {
        rule.advanceTime()
    }

    @Test
    fun `should display init state when opening object without DataView block`() = runTest {
        // SETUP
        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubOpenObject(
            doc = listOf(mockObjectCollection.header, mockObjectCollection.title),
            details = mockObjectCollection.details
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val headerFlow = viewModel.header.testIn(backgroundScope)
        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        assertIs<SetOrCollectionHeaderState.None>(headerFlow.awaitItem())
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())

        assertIs<ObjectState.DataView.Collection>(stateFlow.awaitItem())

        assertEquals(
            expected = mockObjectCollection.details.details[mockObjectCollection.root]?.name,
            actual = (headerFlow.awaitItem() as SetOrCollectionHeaderState.Default).title.text
        )
        viewerFlow.expectNoEvents()

        // ASSERT NO SUBSCRIPTION TO COLLECTION RECORDS
        advanceUntilIdle()
        verifyNoInteractions(repo)
    }

    @Test
    fun `should display collection no view state when opening object without view`() = runTest {
        // SETUP
        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                mockObjectCollection.dataViewEmpty
            ),
            details = mockObjectCollection.details
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())

        assertIs<ObjectState.DataView.Collection>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Collection.NoView>(viewerFlow.awaitItem())

        // ASSERT NO SUBSCRIPTION TO COLLECTION RECORDS
        advanceUntilIdle()
        verifyNoInteractions(repo)
    }


    @Test
    fun `should display collection no items state when opening object without records`() = runTest {
        // SETUP
        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                mockObjectCollection.dataView
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            dvSorts = mockObjectCollection.sorts
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())

        assertIs<ObjectState.DataView.Collection>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Collection.NoItems>(viewerFlow.awaitItem())
    }

    @Test
    fun `should display collection default state when opening object with records`() = runTest {
        // SETUP
        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                mockObjectCollection.dataView
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            objects = listOf(mockObjectCollection.obj1, mockObjectCollection.obj2),
            dvSorts = mockObjectCollection.sorts
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())

        assertIs<ObjectState.DataView.Collection>(stateFlow.awaitItem())
        //assertIs<DataViewViewState.Collection.NoItems>(viewerFlow.awaitItem())
        assertIs<DataViewViewState.Collection.Default>(viewerFlow.awaitItem())
    }

    @Test
    fun `should sort LIST collection objects by data view object order`() = runTest {
        // SETUP
        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                mockObjectCollection.dataViewWithObjectOrder
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            objects = listOf(
                mockObjectCollection.obj1,
                mockObjectCollection.obj2,
                mockObjectCollection.obj3,
                mockObjectCollection.obj4,
                mockObjectCollection.obj5
            ),
            dvSorts = mockObjectCollection.sorts
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())

        assertIs<DataViewViewState.Collection.Default>(viewerFlow.awaitItem()).also {dataViewState ->
            val rows = (dataViewState.viewer as Viewer.ListView).items
            assertEquals(5, rows.size)

            // SHOULD BE 3, 2, 4, 5, 1
            assertEquals(mockObjectCollection.obj3.id, rows[0].objectId)
            assertEquals(mockObjectCollection.obj2.id, rows[1].objectId)
            assertEquals(mockObjectCollection.obj4.id, rows[2].objectId)
            assertEquals(mockObjectCollection.obj5.id, rows[3].objectId)
            assertEquals(mockObjectCollection.obj1.id, rows[4].objectId)
        }
    }

    @Test
    fun `should not sort LIST collection objects by empty data view object order`() = runTest {
        // SETUP
        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                mockObjectCollection.dataView
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            objects = listOf(
                mockObjectCollection.obj5,
                mockObjectCollection.obj2,
                mockObjectCollection.obj3,
                mockObjectCollection.obj4,
                mockObjectCollection.obj1
            ),
            dvSorts = mockObjectCollection.sorts
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())

        assertIs<DataViewViewState.Collection.Default>(viewerFlow.awaitItem()).also {dataViewState ->
            val rows = (dataViewState.viewer as Viewer.ListView).items
            assertEquals(5, rows.size)

            // SHOULD BE NOT SORTED 5, 2, 3, 4, 1
            assertEquals(mockObjectCollection.obj5.id, rows[0].objectId)
            assertEquals(mockObjectCollection.obj2.id, rows[1].objectId)
            assertEquals(mockObjectCollection.obj3.id, rows[2].objectId)
            assertEquals(mockObjectCollection.obj4.id, rows[3].objectId)
            assertEquals(mockObjectCollection.obj1.id, rows[4].objectId)
        }
    }

    @Test
    fun `should sort GRID collection objects by data view object order`() = runTest {
        // SETUP
        session.currentViewerId.value = mockObjectCollection.viewerGrid.id
        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                mockObjectCollection.dataViewWithObjectOrder
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            objects = listOf(
                mockObjectCollection.obj1,
                mockObjectCollection.obj2,
                mockObjectCollection.obj3,
                mockObjectCollection.obj4,
                mockObjectCollection.obj5
            ),
            dvSorts = listOf(mockObjectCollection.sortGrid)
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())

        //assertIs<DataViewViewState.Collection.NoItems>(viewerFlow.awaitItem())
        assertIs<DataViewViewState.Collection.Default>(viewerFlow.awaitItem()).also {dataViewState ->
            val rows = (dataViewState.viewer as Viewer.GridView).rows
            assertEquals(5, rows.size)

            // SHOULD BE SORTED 1, 2, 4, 3, 5
            assertEquals(mockObjectCollection.obj1.id, rows[0].id)
            assertEquals(mockObjectCollection.obj2.id, rows[1].id)
            assertEquals(mockObjectCollection.obj4.id, rows[2].id)
            assertEquals(mockObjectCollection.obj3.id, rows[3].id)
            assertEquals(mockObjectCollection.obj5.id, rows[4].id)
        }
    }

    @Test
    fun `should NOT sort GRID collection objects by empty data view object order`() = runTest {
        // SETUP
        session.currentViewerId.value = mockObjectCollection.viewerGrid.id
        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                mockObjectCollection.dataViewGrid
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            objects = listOf(
                mockObjectCollection.obj1,
                mockObjectCollection.obj2,
                mockObjectCollection.obj3,
                mockObjectCollection.obj4,
                mockObjectCollection.obj5
            ),
            dvSorts = listOf(mockObjectCollection.sortGrid)
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())

        assertIs<DataViewViewState.Collection.Default>(viewerFlow.awaitItem()).also {dataViewState ->
            val rows = (dataViewState.viewer as Viewer.GridView).rows
            assertEquals(5, rows.size)

            // SHOULD BE SORTED 1, 2, 4, 3, 5
            assertEquals(mockObjectCollection.obj1.id, rows[0].id)
            assertEquals(mockObjectCollection.obj2.id, rows[1].id)
            assertEquals(mockObjectCollection.obj3.id, rows[2].id)
            assertEquals(mockObjectCollection.obj4.id, rows[3].id)
            assertEquals(mockObjectCollection.obj5.id, rows[4].id)
        }
    }

    @Test
    fun `should sort GALLERY collection objects by data view object order`() = runTest {
        // SETUP
        session.currentViewerId.value = mockObjectCollection.viewerGallery.id
        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                mockObjectCollection.dataViewWithObjectOrder
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            objects = listOf(
                mockObjectCollection.obj1,
                mockObjectCollection.obj2,
                mockObjectCollection.obj3,
                mockObjectCollection.obj4,
                mockObjectCollection.obj5
            ),
            dvSorts = listOf(mockObjectCollection.sortGallery)
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())

        assertIs<DataViewViewState.Collection.Default>(viewerFlow.awaitItem()).also {dataViewState ->
            val rows = (dataViewState.viewer as Viewer.GalleryView).items
            assertEquals(5, rows.size)

            // SHOULD BE SORTED 5, 4, 3, 2, 1
            assertEquals(mockObjectCollection.obj5.id, rows[0].objectId)
            assertEquals(mockObjectCollection.obj4.id, rows[1].objectId)
            assertEquals(mockObjectCollection.obj3.id, rows[2].objectId)
            assertEquals(mockObjectCollection.obj2.id, rows[3].objectId)
            assertEquals(mockObjectCollection.obj1.id, rows[4].objectId)
        }
    }

    @Test
    fun `should NOT sort GALLERY collection objects by empty data view object order`() = runTest {
        // SETUP
        session.currentViewerId.value = mockObjectCollection.viewerGallery.id
        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                mockObjectCollection.dataViewGallery
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            objects = listOf(
                mockObjectCollection.obj1,
                mockObjectCollection.obj2,
                mockObjectCollection.obj3,
                mockObjectCollection.obj4,
                mockObjectCollection.obj5
            ),
            dvSorts = listOf(mockObjectCollection.sortGallery)
        )
        stubTemplatesContainer()

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())

        assertIs<DataViewViewState.Collection.Default>(viewerFlow.awaitItem()).also {dataViewState ->
            val rows = (dataViewState.viewer as Viewer.GalleryView).items
            assertEquals(5, rows.size)

            // SHOULD BE SORTED 5, 4, 3, 2, 1
            assertEquals(mockObjectCollection.obj1.id, rows[0].objectId)
            assertEquals(mockObjectCollection.obj2.id, rows[1].objectId)
            assertEquals(mockObjectCollection.obj3.id, rows[2].objectId)
            assertEquals(mockObjectCollection.obj4.id, rows[3].objectId)
            assertEquals(mockObjectCollection.obj5.id, rows[4].objectId)
        }
    }

    @Test
    fun `should be collection with templates present when active view hasn't defaultTemplateId`() = runTest {
        // SETUP

        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubTemplatesContainer(
            type = VIEW_DEFAULT_OBJECT_TYPE,
            templates = listOf(StubObject(objectType = VIEW_DEFAULT_OBJECT_TYPE))
        )

        session.currentViewerId.value = mockObjectCollection.viewerList.id
        val dataview = mockObjectCollection.dataView.copy(
            content = (mockObjectCollection.dataView.content as DV).copy(
                viewers = listOf(
                    mockObjectCollection.viewerGrid.copy(defaultObjectType = ObjectTypeIds.PROFILE),
                    mockObjectCollection.viewerList.copy(defaultObjectType = ObjectTypeIds.PAGE)
                )
            )
        )

        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                dataview
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfObjectTypes(
            VIEW_DEFAULT_OBJECT_TYPE,
            mapOf(
                Relations.ID to VIEW_DEFAULT_OBJECT_TYPE,
                Relations.RECOMMENDED_LAYOUT to ObjectType.Layout.BASIC.code.toDouble(),
                Relations.NAME to "VIEW_DEFAULT_OBJECT_TYPE"
            )
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            dvSorts = mockObjectCollection.sorts
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())
        assertIs<ObjectState.DataView.Collection>(stateFlow.awaitItem())

        val item = viewerFlow.awaitItem()
        assertIs<DataViewViewState.Collection.NoItems>(item)
        assertTrue(item.hasTemplates)
    }

    @Test
    fun `should be collection without templates allowed when active viewer default type is NOTE`() = runTest {
        // SETUP

        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()
        stubTemplatesContainer()

        session.currentViewerId.value = mockObjectCollection.viewerList.id
        val dataview = mockObjectCollection.dataView.copy(
            content = (mockObjectCollection.dataView.content as DV).copy(
                viewers = listOf(
                    mockObjectCollection.viewerGrid.copy(defaultObjectType = ObjectTypeIds.PROFILE),
                    mockObjectCollection.viewerList.copy(defaultObjectType = ObjectTypeIds.NOTE)
                )
            )
        )

        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                dataview
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfObjectTypes(
            ObjectTypeIds.NOTE,
            mapOf(
                Relations.ID to ObjectTypeIds.NOTE,
                Relations.RECOMMENDED_LAYOUT to ObjectType.Layout.NOTE.code.toDouble(),
                Relations.NAME to "NOTE"
            )
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            dvSorts = mockObjectCollection.sorts
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())
        assertIs<ObjectState.DataView.Collection>(stateFlow.awaitItem())

        val item = viewerFlow.awaitItem()
        assertIs<DataViewViewState.Collection.NoItems>(item)
        assertFalse(item.hasTemplates)
    }

    @Test
    fun `should be collection without templates allowed when active viewer default type is custom type without recommended layouts`() = runTest {
        // SETUP

        val defaultObjectType = RandomString.make()
        val defaultObjectTypeName = RandomString.make()

        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()

        session.currentViewerId.value = mockObjectCollection.viewerList.id
        val dataview = mockObjectCollection.dataView.copy(
            content = (mockObjectCollection.dataView.content as DV).copy(
                viewers = listOf(
                    mockObjectCollection.viewerGrid.copy(defaultObjectType = ObjectTypeIds.PROFILE),
                    mockObjectCollection.viewerList.copy(defaultObjectType = defaultObjectType)
                )
            )
        )

        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                dataview
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfObjectTypes(
            defaultObjectType,
            mapOf(
                Relations.ID to defaultObjectType,
                Relations.RECOMMENDED_LAYOUT to ObjectType.Layout.COLLECTION.code.toDouble(),
                Relations.NAME to defaultObjectTypeName
            )
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            dvSorts = mockObjectCollection.sorts
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())
        assertIs<ObjectState.DataView.Collection>(stateFlow.awaitItem())

        val item = viewerFlow.awaitItem()
        assertIs<DataViewViewState.Collection.NoItems>(item)
        assertFalse(item.hasTemplates)
    }

    @Test
    fun `should be collection with templates allowed when active viewer default type is custom type with recommended layouts`() = runTest {
        // SETUP

        val defaultObjectType = RandomString.make()
        val defaultObjectTypeName = RandomString.make()

        stubWorkspaceManager(mockObjectCollection.workspaceId)
        stubInterceptEvents()
        stubInterceptThreadStatus()

        session.currentViewerId.value = mockObjectCollection.viewerList.id
        val dataview = mockObjectCollection.dataView.copy(
            content = (mockObjectCollection.dataView.content as DV).copy(
                viewers = listOf(
                    mockObjectCollection.viewerGrid.copy(defaultObjectType = ObjectTypeIds.PROFILE),
                    mockObjectCollection.viewerList.copy(defaultObjectType = defaultObjectType)
                )
            )
        )

        stubOpenObject(
            doc = listOf(
                mockObjectCollection.header,
                mockObjectCollection.title,
                dataview
            ),
            details = mockObjectCollection.details
        )
        stubStoreOfObjectTypes(
            defaultObjectType,
            mapOf(
                Relations.ID to defaultObjectType,
                Relations.RECOMMENDED_LAYOUT to SupportedLayouts.editorLayouts.random().code.toDouble(),
                Relations.NAME to defaultObjectTypeName
            )
        )
        stubStoreOfRelations(mockObjectCollection)
        stubSubscriptionResults(
            subscription = mockObjectCollection.subscriptionId,
            workspace = mockObjectCollection.workspaceId,
            collection = root,
            storeOfRelations = storeOfRelations,
            keys = mockObjectCollection.dvKeys,
            dvSorts = mockObjectCollection.sorts
        )

        // TESTING
        viewModel.onStart(ctx = root)

        val viewerFlow = viewModel.currentViewer.testIn(backgroundScope)
        val stateFlow = stateReducer.state.testIn(backgroundScope)

        // ASSERT STATES
        assertIs<ObjectState.Init>(stateFlow.awaitItem())
        assertIs<DataViewViewState.Init>(viewerFlow.awaitItem())
        assertIs<ObjectState.DataView.Collection>(stateFlow.awaitItem())

        val item = viewerFlow.awaitItem()
        assertIs<DataViewViewState.Collection.NoItems>(item)
        assertTrue(item.hasTemplates)
    }
}