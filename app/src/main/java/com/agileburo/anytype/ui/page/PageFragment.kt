package com.agileburo.anytype.ui.page

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agileburo.anytype.R
import com.agileburo.anytype.core_ui.features.page.BlockAdapter
import com.agileburo.anytype.core_utils.ext.invisible
import com.agileburo.anytype.core_utils.ext.visible
import com.agileburo.anytype.di.common.componentManager
import com.agileburo.anytype.ext.extractMarks
import com.agileburo.anytype.presentation.page.PageViewModel
import com.agileburo.anytype.presentation.page.PageViewModelFactory
import com.agileburo.anytype.ui.base.NavigationFragment
import kotlinx.android.synthetic.main.fragment_page.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class PageFragment : NavigationFragment(R.layout.fragment_page) {

    private val pageAdapter by lazy {
        BlockAdapter(
            blocks = mutableListOf(),
            onTextChanged = { id, editable ->
                vm.onTextChanged(id, editable.toString(), editable.extractMarks())
            },
            onSelectionChanged = { id, selection ->
                vm.onSelectionChanged(
                    id = id,
                    selection = selection
                )
                if (selection.first == selection.last)
                    markupToolbar.invisible()
                else
                    markupToolbar.visible()
            }
        )
    }

    private val vm by lazy {
        ViewModelProviders
            .of(this, factory)
            .get(PageViewModel::class.java)
    }

    @Inject
    lateinit var factory: PageViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm.open(requireArguments().getString(ID_KEY, ID_EMPTY_VALUE))

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(this) {
                vm.onSystemBackPressed()
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = pageAdapter
        }

        toolbar.addButton().setOnClickListener { vm.onAddTextBlockClicked() }

        markupToolbar
            .markupClicks()
            .onEach { vm.onMarkupActionClicked(it) }
            .launchIn(lifecycleScope)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        vm.state.observe(viewLifecycleOwner, Observer { render(it) })
        vm.navigation.observe(this, navObserver)
    }

    private fun render(state: PageViewModel.ViewState) {
        when (state) {
            is PageViewModel.ViewState.Success -> {
                pageAdapter.updateWithDiffUtil(state.blocks)
            }
        }
    }

    override fun injectDependencies() {
        componentManager().pageComponent.get().inject(this)
    }

    override fun releaseDependencies() {
        componentManager().pageComponent.release()
    }

    companion object {
        const val ID_KEY = "id"
        const val ID_EMPTY_VALUE = ""
    }
}