package com.agileburo.anytype.presentation.desktop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.agileburo.anytype.domain.auth.interactor.GetCurrentAccount
import com.agileburo.anytype.domain.dashboard.interactor.CloseDashboard
import com.agileburo.anytype.domain.dashboard.interactor.ObserveDashboardBlocks
import com.agileburo.anytype.domain.dashboard.interactor.OpenDashboard
import com.agileburo.anytype.domain.image.LoadImage
import com.agileburo.anytype.domain.page.CreatePage

class HomeDashboardViewModelFactory(
    private val getCurrentAccount: GetCurrentAccount,
    private val loadImage: LoadImage,
    private val openDashboard: OpenDashboard,
    private val observeDashboardBlocks: ObserveDashboardBlocks,
    private val closeDashboard: CloseDashboard,
    private val createPage: CreatePage
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return HomeDashboardViewModel(
            getCurrentAccount = getCurrentAccount,
            loadImage = loadImage,
            openDashboard = openDashboard,
            observeDashboardBlocks = observeDashboardBlocks,
            closeDashboard = closeDashboard,
            createPage = createPage
        ) as T
    }
}