package com.anytypeio.anytype.di.feature

import com.anytypeio.anytype.analytics.base.Analytics
import com.anytypeio.anytype.core_utils.di.scope.PerScreen
import com.anytypeio.anytype.core_utils.tools.FeatureToggles
import com.anytypeio.anytype.domain.`object`.ObjectTypesProvider
import com.anytypeio.anytype.domain.auth.interactor.GetProfile
import com.anytypeio.anytype.domain.auth.repo.AuthRepository
import com.anytypeio.anytype.domain.base.AppCoroutineDispatchers
import com.anytypeio.anytype.domain.block.interactor.Move
import com.anytypeio.anytype.domain.block.repo.BlockRepository
import com.anytypeio.anytype.domain.config.ConfigStorage
import com.anytypeio.anytype.domain.config.GetConfig
import com.anytypeio.anytype.domain.config.GetDebugSettings
import com.anytypeio.anytype.domain.config.InfrastructureRepository
import com.anytypeio.anytype.domain.config.UserSettingsRepository
import com.anytypeio.anytype.domain.dashboard.interactor.CloseDashboard
import com.anytypeio.anytype.domain.dashboard.interactor.OpenDashboard
import com.anytypeio.anytype.domain.event.interactor.EventChannel
import com.anytypeio.anytype.domain.event.interactor.InterceptEvents
import com.anytypeio.anytype.domain.launch.GetDefaultEditorType
import com.anytypeio.anytype.domain.misc.UrlBuilder
import com.anytypeio.anytype.domain.objects.DeleteObjects
import com.anytypeio.anytype.domain.objects.ObjectStore
import com.anytypeio.anytype.domain.objects.SetObjectListIsArchived
import com.anytypeio.anytype.domain.objects.StoreOfObjectTypes
import com.anytypeio.anytype.domain.page.CreateObject
import com.anytypeio.anytype.domain.search.CancelSearchSubscription
import com.anytypeio.anytype.domain.search.ObjectSearchSubscriptionContainer
import com.anytypeio.anytype.domain.search.SearchObjects
import com.anytypeio.anytype.domain.search.SubscriptionEventChannel
import com.anytypeio.anytype.domain.templates.GetTemplates
import com.anytypeio.anytype.domain.workspace.WorkspaceManager
import com.anytypeio.anytype.presentation.dashboard.HomeDashboardEventConverter
import com.anytypeio.anytype.presentation.dashboard.HomeDashboardViewModelFactory
import com.anytypeio.anytype.ui.dashboard.DashboardFragment
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.Dispatchers


@Subcomponent(
    modules = [HomeDashboardModule::class]
)
@PerScreen
interface HomeDashboardSubComponent {

    @Subcomponent.Builder
    interface Builder {
        fun homeDashboardModule(module: HomeDashboardModule): Builder
        fun build(): HomeDashboardSubComponent
    }

    fun inject(fragment: DashboardFragment)
}

@Module
object HomeDashboardModule {

    @JvmStatic
    @Provides
    @PerScreen
    fun provideDesktopViewModelFactory(
        getProfile: GetProfile,
        openDashboard: OpenDashboard,
        closeDashboard: CloseDashboard,
        getConfig: GetConfig,
        move: Move,
        interceptEvents: InterceptEvents,
        eventConverter: HomeDashboardEventConverter,
        getDebugSettings: GetDebugSettings,
        analytics: Analytics,
        searchObjects: SearchObjects,
        urlBuilder: UrlBuilder,
        setObjectListIsArchived: SetObjectListIsArchived,
        deleteObjects: DeleteObjects,
        objectSearchSubscriptionContainer: ObjectSearchSubscriptionContainer,
        cancelSearchSubscription: CancelSearchSubscription,
        objectStore: ObjectStore,
        featureToggles: FeatureToggles,
        createObject: CreateObject,
        storeOfObjectTypes: StoreOfObjectTypes,
        workspaceManager: WorkspaceManager
    ): HomeDashboardViewModelFactory = HomeDashboardViewModelFactory(
        getProfile = getProfile,
        openDashboard = openDashboard,
        closeDashboard = closeDashboard,
        getConfig = getConfig,
        move = move,
        interceptEvents = interceptEvents,
        eventConverter = eventConverter,
        getDebugSettings = getDebugSettings,
        searchObjects = searchObjects,
        analytics = analytics,
        urlBuilder = urlBuilder,
        setObjectListIsArchived = setObjectListIsArchived,
        deleteObjects = deleteObjects,
        objectSearchSubscriptionContainer = objectSearchSubscriptionContainer,
        cancelSearchSubscription = cancelSearchSubscription,
        objectStore = objectStore,
        createObject = createObject,
        featureToggles = featureToggles,
        storeOfObjectTypes = storeOfObjectTypes,
        workspaceManager = workspaceManager
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun provideGetProfileUseCase(
        repository: BlockRepository,
        subscriptionEventChannel: SubscriptionEventChannel,
        provider: ConfigStorage
    ): GetProfile = GetProfile(
        repo = repository,
        channel = subscriptionEventChannel,
        provider = provider
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun provideOpenDashboardUseCase(
        repo: BlockRepository,
        auth: AuthRepository,
        provider: ConfigStorage
    ): OpenDashboard = OpenDashboard(
        repo = repo,
        auth = auth,
        provider = provider
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun provideCloseDashboardUseCase(
        repo: BlockRepository,
        provider: ConfigStorage
    ): CloseDashboard = CloseDashboard(
        repo = repo,
        provider = provider
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun getConfigUseCase(
        provider: ConfigStorage
    ): GetConfig = GetConfig(
        provider = provider
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun provideMoveUseCase(
        repo: BlockRepository
    ): Move = Move(
        repo = repo
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun provideInterceptEvents(
        channel: EventChannel
    ): InterceptEvents = InterceptEvents(
        context = Dispatchers.IO,
        channel = channel
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun provideEventConverter(
        builder: UrlBuilder,
        storeOfObjectTypes: StoreOfObjectTypes
    ): HomeDashboardEventConverter {
        return HomeDashboardEventConverter.DefaultConverter(
            builder = builder,
            storeOfObjectTypes = storeOfObjectTypes
        )
    }

    @JvmStatic
    @Provides
    @PerScreen
    fun provideGetDebugSettings(
        repo: InfrastructureRepository
    ): GetDebugSettings = GetDebugSettings(
        repo = repo
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun provideSearchObjects(
        repo: BlockRepository
    ): SearchObjects = SearchObjects(
        repo = repo
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun provideGetDefaultPageType(repo: UserSettingsRepository): GetDefaultEditorType =
        GetDefaultEditorType(repo)

    @JvmStatic
    @Provides
    @PerScreen
    fun deleteObjects(
        repo: BlockRepository
    ): DeleteObjects = DeleteObjects(
        repo = repo
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun setObjectListIsArchived(
        repo: BlockRepository
    ): SetObjectListIsArchived = SetObjectListIsArchived(
        repo = repo
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun objectSearchSubscriptionContainer(
        repo: BlockRepository,
        channel: SubscriptionEventChannel,
        store: ObjectStore
    ): ObjectSearchSubscriptionContainer = ObjectSearchSubscriptionContainer(
        repo = repo,
        channel = channel,
        store = store,
        dispatchers = AppCoroutineDispatchers(
            io = Dispatchers.IO,
            computation = Dispatchers.Default,
            main = Dispatchers.Main
        )
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun cancelSearchSubscription(
        repo: BlockRepository,
        store: ObjectStore
    ): CancelSearchSubscription = CancelSearchSubscription(
        repo = repo,
        store = store
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun getTemplates(
        repo: BlockRepository
    ) : GetTemplates = GetTemplates(
        repo = repo,
        dispatchers = AppCoroutineDispatchers(
            io = Dispatchers.IO,
            computation = Dispatchers.Default,
            main = Dispatchers.Main
        )
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun getCreateObject(
        repo: BlockRepository,
        getTemplates: GetTemplates,
        getDefaultEditorType: GetDefaultEditorType
    ): CreateObject = CreateObject(
        repo = repo,
        getTemplates = getTemplates,
        getDefaultEditorType = getDefaultEditorType
    )
}