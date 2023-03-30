package com.anytypeio.anytype.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.anytypeio.anytype.R
import com.anytypeio.anytype.analytics.base.EventsDictionary
import com.anytypeio.anytype.core_ui.common.ComposeDialogView
import com.anytypeio.anytype.core_utils.ext.GetImageContract
import com.anytypeio.anytype.core_utils.ext.parseImagePath
import com.anytypeio.anytype.core_utils.ext.shareFile
import com.anytypeio.anytype.core_utils.ext.subscribe
import com.anytypeio.anytype.core_utils.ext.toast
import com.anytypeio.anytype.core_utils.tools.FeatureToggles
import com.anytypeio.anytype.core_utils.ui.BaseBottomSheetComposeFragment
import com.anytypeio.anytype.di.common.componentManager
import com.anytypeio.anytype.ui.auth.account.DeleteAccountWarning
import com.anytypeio.anytype.ui.dashboard.ClearCacheAlertFragment
import com.anytypeio.anytype.ui.editor.modals.IconPickerFragmentBase
import com.anytypeio.anytype.ui.profile.KeychainPhraseDialog
import com.anytypeio.anytype.ui.sets.ARG_SHOW_REMOVE_BUTTON
import com.anytypeio.anytype.ui_settings.account.AccountAndDataScreen
import com.anytypeio.anytype.ui_settings.account.AccountAndDataViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import javax.inject.Inject
import timber.log.Timber

class AccountAndDataFragment : BaseBottomSheetComposeFragment() {

    @Inject
    lateinit var factory: AccountAndDataViewModel.Factory

    @Inject
    lateinit var toggles: FeatureToggles

    private val vm by viewModels<AccountAndDataViewModel> { factory }

    private val onKeychainPhraseClicked = {
        val bundle =
            bundleOf(KeychainPhraseDialog.ARG_SCREEN_TYPE to EventsDictionary.Type.screenSettings)
        safeNavigate(R.id.keychainDialog, bundle)
    }

    private val onLogoutClicked = {
        safeNavigate(R.id.logoutWarningScreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        jobs += lifecycleScope.subscribe(vm.debugSyncReportUri) { uri ->
            if (uri != null) {
                shareFile(uri)
            }
        }
        return ComposeDialogView(
            context = requireContext(),
            dialog = requireDialog()
        ).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(typography = typography) {
                    AccountAndDataScreen(
                        onKeychainPhraseClicked = onKeychainPhraseClicked,
                        onClearFileCachedClicked = { throttle { proceedWithClearFileCacheWarning() } },
                        onDeleteAccountClicked = { throttle { proceedWithAccountDeletion() } },
                        onLogoutClicked = onLogoutClicked,
                        onDebugSyncReportClicked = { throttle { vm.onDebugSyncReportClicked() } },
                        isLogoutInProgress = vm.isLoggingOut.collectAsState().value,
                        isClearCacheInProgress = vm.isClearFileCacheInProgress.collectAsState().value,
                        isDebugSyncReportInProgress = vm.isDebugSyncReportInProgress.collectAsState().value,
                        isShowDebug = toggles.isTroubleshootingMode,
                        onNameChange = { vm.onNameChange(it) },
                        onProfileIconClick = { proceedWithIconClick() },
                        vm.accountData.collectAsStateWithLifecycle().value
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val offsetFromTop = PADDING_TOP
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            isFitToContents = false
            expandedOffset = offsetFromTop
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun proceedWithClearFileCacheWarning() {
        vm.onClearCacheButtonClicked()
        val dialog = ClearCacheAlertFragment.new()
        dialog.onClearAccepted = { vm.onClearFileCacheAccepted() }
        dialog.show(childFragmentManager, null)
    }

    private fun proceedWithAccountDeletion() {
        val dialog = DeleteAccountWarning()
        dialog.onDeletionAccepted = {
            dialog.dismiss()
            vm.onDeleteAccountClicked()
        }
        dialog.show(childFragmentManager, null)
    }

    private fun proceedWithIconClick() {
        if (!hasExternalStoragePermission()) {
            permissionReadStorage.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        } else {
            openGallery()
        }
    }

    private fun hasExternalStoragePermission() = ContextCompat.checkSelfPermission(
        requireActivity(),
        Manifest.permission.READ_EXTERNAL_STORAGE
    ).let { result -> result == PackageManager.PERMISSION_GRANTED }

    private val permissionReadStorage =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
            val readResult = grantResults[Manifest.permission.READ_EXTERNAL_STORAGE]
            if (readResult == true) {
                openGallery()
            } else {
                toast(R.string.permission_read_denied)
            }
        }

    private fun openGallery() {
        getContent.launch(SELECT_IMAGE_CODE)
    }

    private val getContent = registerForActivityResult(GetImageContract()) { uri: Uri? ->
        if (uri != null) {
            try {
                val path = uri.parseImagePath(requireContext())
                vm.onPickedImageFromDevice(path = path)
            } catch (e: Exception) {
                toast("Error while parsing path for cover image")
                Timber.d(e, "Error while parsing path for cover image")
            }
        } else {
            toast("Error while upload cover image, URI is null")
            Timber.e("Error while upload cover image, URI is null")
        }
    }

    override fun injectDependencies() {
        componentManager().accountAndDataComponent.get().inject(this)
    }

    override fun releaseDependencies() {
        componentManager().accountAndDataComponent.release()
    }
}

private const val PADDING_TOP = 54

private const val SELECT_IMAGE_CODE = 1
