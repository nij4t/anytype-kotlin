package com.agileburo.anytype.presentation.page.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.agileburo.anytype.core_utils.ui.ViewStateViewModel
import com.agileburo.anytype.presentation.page.bookmark.CreateBookmarkViewModel.ViewState

class CreateBookmarkViewModel() : ViewStateViewModel<ViewState>() {

    fun onCreateBookmarkClicked(
        url: String
    ) {
        update(ViewState.Success(url = url))
    }

    sealed class ViewState {
        data class Success(val url: String) : ViewState()
        data class Error(val message: String) : ViewState()
        object Exit : ViewState()
    }

    class Factory() : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            CreateBookmarkViewModel() as T
    }
}