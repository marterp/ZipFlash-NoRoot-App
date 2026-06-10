package com.zipflash.mrp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.zipflash.mrp.databinding.ActivityModulesBinding
import com.zipflash.mrp.util.CustomTabsHelper
import com.zipflash.mrp.viewmodel.Module
import com.zipflash.mrp.viewmodel.ModulesViewModel
import kotlinx.coroutines.launch

class ModulesFragment : BaseFragment() {

    private var binding: ActivityModulesBinding? = null
    private val viewModel: ModulesViewModel by viewModels()
    private var adapter: ModulesAdapter? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ActivityModulesBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(view, "Modules")

        binding?.progressBar?.indeterminateDrawable?.setColorFilter(
            requireContext().getColor(R.color.accent_primary), android.graphics.PorterDuff.Mode.SRC_IN)

        swipeRefreshLayout = binding!!.swipeRefreshLayout

        swipeRefreshLayout.setOnRefreshListener {
            adapter?.filter("")
            viewModel.loadModules(forceRefresh = true)
        }

        swipeRefreshLayout.setColorSchemeColors(
            requireContext().getColor(R.color.accent_primary),
            requireContext().getColor(R.color.accent_warning),
            requireContext().getColor(R.color.accent_error)
        )
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(requireContext().getColor(R.color.bg_card))

        binding?.btnRetry?.setOnClickListener {
            viewModel.loadModules(forceRefresh = true)
        }

        observeViewModel()
        viewModel.loadModules()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val isEmpty = state.modules.isEmpty()

                    swipeRefreshLayout.isRefreshing = state.isRefreshing

                    if (state.isLoading) {
                        binding?.loadingView?.visibility = View.VISIBLE
                        binding?.recyclerModules?.visibility = View.GONE
                        binding?.errorView?.visibility = View.GONE
                    } else if (state.error != null && isEmpty) {
                        binding?.loadingView?.visibility = View.GONE
                        binding?.recyclerModules?.visibility = View.GONE
                        binding?.errorView?.visibility = View.VISIBLE
                        val errorMessage = binding?.errorView?.findViewById<TextView>(R.id.errorMessage)
                        errorMessage?.text = state.error
                    } else if (state.error != null && !isEmpty) {
                        binding?.loadingView?.visibility = View.GONE
                        binding?.recyclerModules?.visibility = View.VISIBLE
                        binding?.errorView?.visibility = View.GONE
                        swipeRefreshLayout.isRefreshing = false
                    } else if (!isEmpty) {
                        binding?.loadingView?.visibility = View.GONE
                        binding?.recyclerModules?.visibility = View.VISIBLE
                        binding?.errorView?.visibility = View.GONE
                        if (adapter == null) {
                            adapter = ModulesAdapter(requireContext(), state.filteredModules)
                            binding?.recyclerModules?.setHasFixedSize(true)
                            binding?.recyclerModules?.layoutManager = LinearLayoutManager(requireContext())
                            binding?.recyclerModules?.setItemViewCacheSize(20)
                            binding?.recyclerModules?.adapter = adapter
                        } else {
                            adapter!!.updateData(state.filteredModules)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_modules, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Search modules..."
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filter(newText)
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_sort) {
            showSortPopup()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSortPopup() {
        val popup = PopupMenu(requireContext(), binding?.toolbar, android.view.Gravity.END)
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            val which = when (item.itemId) {
                R.id.sort_az -> 0
                R.id.sort_za -> 1
                R.id.sort_latest -> 2
                else -> -1
            }
            if (which != -1) viewModel.sortBy(which)
            true
        }
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private class ModulesAdapter(
        private val context: Context,
        modules: List<Module>
    ) : RecyclerView.Adapter<ModulesAdapter.ModuleViewHolder>() {

        private val modules = mutableListOf<Module>().also { it.addAll(modules) }
        private val allModules = mutableListOf<Module>().also { it.addAll(modules) }
        private var lastPosition = -1
        private var expandedPosition = -1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_modules, parent, false)
            return ModuleViewHolder(view)
        }

        @Suppress("RecyclerView")
        override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
            val adapterPosition = holder.bindingAdapterPosition
            val module = modules[adapterPosition]
            holder.title.text = module.title

            val isExpanded = adapterPosition == expandedPosition
            holder.detailsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.detailsTitle.text = module.title
            holder.detailsDesc.text = if (module.description.isNotEmpty()) module.description else "No description"

            holder.itemView.setOnClickListener {
                if (expandedPosition == adapterPosition) {
                    expandedPosition = -1
                } else {
                    val prev = expandedPosition
                    expandedPosition = adapterPosition
                    notifyItemChanged(prev)
                }
                notifyItemChanged(position)
            }

            holder.linkIcon.setOnClickListener {
                val url = "https://zip-flash-modules.vercel.app/zf/${module.index}"
                CustomTabsHelper.openUrl(context, url)
            }

            if (position > lastPosition) {
                val animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right)
                holder.itemView.startAnimation(animation)
                lastPosition = position
            } else {
                holder.itemView.clearAnimation()
            }
        }

        override fun getItemCount(): Int = modules.size

        fun updateData(newModules: List<Module>) {
            allModules.clear()
            allModules.addAll(newModules)
            modules.clear()
            modules.addAll(newModules)
            notifyDataSetChanged()
        }

        fun filter(text: String?) {
            modules.clear()
            if (text.isNullOrBlank()) {
                modules.addAll(allModules)
            } else {
                val query = text.lowercase().trim()
                for (m in allModules) {
                    if (m.title.lowercase().contains(query) || m.description.lowercase().contains(query)) {
                        modules.add(m)
                    }
                }
            }
            notifyDataSetChanged()
        }

        class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.moduleTitle)
            val linkIcon: ImageView = itemView.findViewById(R.id.moduleDownloadIcon)
            val detailsContainer: LinearLayout = itemView.findViewById(R.id.detailsContainer)
            val detailsTitle: TextView = itemView.findViewById(R.id.detailsTitle)
            val detailsDesc: TextView = itemView.findViewById(R.id.detailsDesc)
        }
    }
}
