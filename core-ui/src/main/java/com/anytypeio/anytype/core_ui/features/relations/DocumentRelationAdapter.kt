package com.anytypeio.anytype.core_ui.features.relations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.anytypeio.anytype.core_ui.R
import com.anytypeio.anytype.core_utils.diff.DefaultDiffUtil
import com.anytypeio.anytype.presentation.relations.DocumentRelationView
import com.anytypeio.anytype.presentation.relations.RelationListViewModel
import kotlinx.android.synthetic.main.item_relation_list_section.view.*
import timber.log.Timber
import com.anytypeio.anytype.core_ui.features.editor.holders.relations.RelationViewHolder as ViewHolder

class DocumentRelationAdapter(
    private var items: List<RelationListViewModel.Model>,
    private val onRelationClicked: (RelationListViewModel.Model.Item) -> Unit,
    private val onCheckboxClicked: (RelationListViewModel.Model.Item) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_relation_list_relation_default -> {
                ViewHolder.Default(view = inflater.inflate(viewType, parent, false)).apply {
                    itemView.setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onRelationClicked(view)
                    }
                    itemView.findViewById<View>(R.id.featuredRelationCheckbox).setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onCheckboxClicked(view)
                    }
                }
            }
            R.layout.item_relation_list_relation_checkbox -> {
                ViewHolder.Checkbox(view = inflater.inflate(viewType, parent, false)).apply {
                    itemView.setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onRelationClicked(view)
                    }
                    itemView.findViewById<View>(R.id.featuredRelationCheckbox).setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onCheckboxClicked(view)
                    }
                }
            }
            R.layout.item_relation_list_relation_object -> {
                ViewHolder.Object(view = inflater.inflate(viewType, parent, false)).apply {
                    itemView.setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onRelationClicked(view)
                    }
                    itemView.findViewById<View>(R.id.featuredRelationCheckbox).setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onCheckboxClicked(view)
                    }
                }
            }
            R.layout.item_relation_list_relation_status -> {
                ViewHolder.Status(view = inflater.inflate(viewType, parent, false)).apply {
                    itemView.setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onRelationClicked(view)
                    }
                    itemView.findViewById<View>(R.id.featuredRelationCheckbox).setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onCheckboxClicked(view)
                    }
                }
            }
            R.layout.item_relation_list_relation_tag -> {
                ViewHolder.Tags(view = inflater.inflate(viewType, parent, false)).apply {
                    itemView.setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onRelationClicked(view)
                    }
                    itemView.findViewById<View>(R.id.featuredRelationCheckbox).setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onCheckboxClicked(view)
                    }
                }
            }
            R.layout.item_relation_list_relation_file -> {
                ViewHolder.File(view = inflater.inflate(viewType, parent, false)).apply {
                    itemView.setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onRelationClicked(view)
                    }
                    itemView.findViewById<View>(R.id.featuredRelationCheckbox).setOnClickListener {
                        val view = items[bindingAdapterPosition]
                        check(view is RelationListViewModel.Model.Item)
                        onCheckboxClicked(view)
                    }
                }
            }
            R.layout.item_relation_list_section -> {
                SectionViewHolder(view = inflater.inflate(viewType, parent, false))
            }
            else -> throw IllegalStateException("Unexpected view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ViewHolder.Status -> {
                check(item is RelationListViewModel.Model.Item)
                val view = item.view
                check(view is DocumentRelationView.Status)
                holder.bind(view)
            }
            is ViewHolder.Checkbox -> {
                check(item is RelationListViewModel.Model.Item)
                val view = item.view
                check(view is DocumentRelationView.Checkbox)
                holder.bind(view)
            }
            is ViewHolder.Tags -> {
                check(item is RelationListViewModel.Model.Item)
                val view = item.view
                check(view is DocumentRelationView.Tags)
                holder.bind(view)
            }
            is ViewHolder.Object -> {
                check(item is RelationListViewModel.Model.Item)
                val view = item.view
                check(view is DocumentRelationView.Object)
                holder.bind(view)
            }
            is ViewHolder.File -> {
                check(item is RelationListViewModel.Model.Item)
                val view = item.view
                check(view is DocumentRelationView.File)
                holder.bind(view)
            }
            is ViewHolder.Default -> {
                check(item is RelationListViewModel.Model.Item)
                val view = item.view
                check(view is DocumentRelationView.Default)
                holder.bind(view)
            }
            is SectionViewHolder -> {
                check(item is RelationListViewModel.Model.Section)
                holder.bind(item)
            }
            else -> { Timber.d("Skipping binding for: $holder") }
        }
        if (holder is ViewHolder) {
            check(item is RelationListViewModel.Model.Item)
            holder.setIsFeatured(item.view.isFeatured)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = when (val item = items[position]) {
        is RelationListViewModel.Model.Item -> {
            when(item.view) {
                is DocumentRelationView.Checkbox -> R.layout.item_relation_list_relation_checkbox
                is DocumentRelationView.Object -> R.layout.item_relation_list_relation_object
                is DocumentRelationView.Status -> R.layout.item_relation_list_relation_status
                is DocumentRelationView.Tags -> R.layout.item_relation_list_relation_tag
                is DocumentRelationView.File -> R.layout.item_relation_list_relation_file
                else -> R.layout.item_relation_list_relation_default
            }
        }
        is RelationListViewModel.Model.Section.Featured -> R.layout.item_relation_list_section
        is RelationListViewModel.Model.Section.Other -> R.layout.item_relation_list_section
    }

    fun update(update: List<RelationListViewModel.Model>) {
        Timber.d("Updating adapter: $update")
        val differ = DefaultDiffUtil(old = items, new = update)
        val result = DiffUtil.calculateDiff(differ, false)
        items = update
        result.dispatchUpdatesTo(this)
    }

    class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(section: RelationListViewModel.Model.Section) {
            when(section) {
                RelationListViewModel.Model.Section.Featured -> {
                    itemView.tvSectionName.setText(R.string.featured_relations)
                }
                RelationListViewModel.Model.Section.Other -> {
                    itemView.tvSectionName.setText(R.string.other_relations)
                }
            }
        }
    }
}