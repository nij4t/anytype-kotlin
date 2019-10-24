package com.agileburo.anytype.presentation.auth.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agileburo.anytype.core_utils.common.Event
import com.agileburo.anytype.core_utils.ui.ViewState
import com.agileburo.anytype.domain.auth.interactor.CreateAccount
import com.agileburo.anytype.presentation.auth.model.Session
import com.agileburo.anytype.presentation.navigation.AppNavigation
import com.agileburo.anytype.presentation.navigation.SupportNavigation
import timber.log.Timber

class SetupNewAccountViewModel(
    private val session: Session,
    private val createAccount: CreateAccount
) : ViewModel(), SupportNavigation<Event<AppNavigation.Command>> {

    override val navigation: MutableLiveData<Event<AppNavigation.Command>> = MutableLiveData()

    private val _state = MutableLiveData<ViewState<Any>>()
    val state: LiveData<ViewState<Any>>
        get() = _state

    init {
        _state.postValue(ViewState.Loading)
        proceedWithCreatingAccount()
    }

    private fun proceedWithCreatingAccount() {
        createAccount.invoke(
            scope = viewModelScope,
            params = CreateAccount.Params(
                name = session.name ?: throw IllegalStateException("Name not set")
            )
        ) { result ->
            result.either(
                fnL = {
                    _state.postValue(ViewState.Error("Error while creating account"))
                    Timber.e(it, "Error while creating account")
                },
                fnR = {
                    _state.postValue(ViewState.Success(Any()))
                    navigation.postValue(Event(AppNavigation.Command.CongratulationScreen))
                }
            )
        }
    }
}