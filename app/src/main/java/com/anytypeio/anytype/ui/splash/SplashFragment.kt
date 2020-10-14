package com.anytypeio.anytype.ui.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.anytypeio.anytype.BuildConfig
import com.anytypeio.anytype.R
import com.anytypeio.anytype.core_utils.ext.toast
import com.anytypeio.anytype.core_utils.ext.visible
import com.anytypeio.anytype.core_utils.ui.ViewState
import com.anytypeio.anytype.di.common.componentManager
import com.anytypeio.anytype.presentation.splash.SplashViewModel
import com.anytypeio.anytype.presentation.splash.SplashViewModelFactory
import com.anytypeio.anytype.ui.base.NavigationFragment
import kotlinx.android.synthetic.main.fragment_splash.*
import javax.inject.Inject

/**
 * Created by Konstantin Ivanov
 * email :  ki@agileburo.com
 * on 2019-10-21.
 */
class SplashFragment : NavigationFragment(R.layout.fragment_splash), Observer<ViewState<Nothing>> {

    @Inject
    lateinit var factory: SplashViewModelFactory
    private val vm by viewModels<SplashViewModel> { factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.navigation.observe(viewLifecycleOwner, navObserver)
        vm.state.observe(viewLifecycleOwner, this)
        showVersion()
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    private fun showVersion() {
        version.text = "${BuildConfig.VERSION_NAME}-alpha"
    }

    override fun onChanged(state: ViewState<Nothing>) {
        if (state is ViewState.Error) {
            toast(state.error)
            error.visible()
        }
    }

    override fun injectDependencies() {
        componentManager().splashLoginComponent.get().inject(this)
    }

    override fun releaseDependencies() {
        componentManager().splashLoginComponent.release()
    }
}