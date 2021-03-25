package com.intel.gamepad.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.intel.gamepad.R
import com.intel.gamepad.activity.GameDetailActivity
import com.intel.gamepad.activity.MessageActivity
import com.intel.gamepad.activity.SearchActivity
import com.intel.gamepad.app.*
import com.intel.gamepad.bean.GameListBean
import com.intel.gamepad.model.GameListModel
import com.mcxtzhang.commonadapter.rv.CommonAdapter
import com.mcxtzhang.commonadapter.rv.ViewHolder
import com.mycommonlibrary.utils.DensityUtils
import com.mycommonlibrary.utils.LogEx
import com.mycommonlibrary.utils.ScreenUtils
import com.mycommonlibrary.view.decoration.GridSpacingItemDecoration
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.include_device_info.*
import kotlinx.android.synthetic.main.include_home_title_search.*
import kotlinx.android.synthetic.main.item_home_rank.view.*
import org.jetbrains.anko.support.v4.longToast

class HomeFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    private val listGame = mutableListOf<GameListBean>()
    private lateinit var model: GameListModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initViewModel() {
        model = ViewModelProviders.of(this).get(GameListModel::class.java)
        model.showLoading.observe(this, Observer {
            refreshLayout.isRefreshing = it
        })
        model.listData.observe(this, Observer {
            val json = GameListBean.toJson(it)
            BufferHelper.save(AppConst.QUERY_LIST, json)
            updateListData(it)
        })
        model.errorMessage.observe(this, Observer { longToast(it) })
    }

    override fun onStart() {
        super.onStart()
        BufferHelper.load(AppConst.QUERY_LIST)?.let { updateListData(it) }
        model.requestGameList()
    }

    private fun initView() {
        initRefreshLayout()
        initViewPager()
        initRvGrid()
        ibtnMessage.setOnClickListener { MessageActivity.actionStartFragment(this@HomeFragment) }
        tvSearch.setOnClickListener { SearchActivity.actionStartFragment(this@HomeFragment) }
    }

    private fun initViewPager() {
        viewPager.isFocusable = false
        viewPager.adapter = object : FragmentStatePagerAdapter(
            childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> MemoryFragment()
                    1 -> BatteryFragment()
                    else -> Fragment()
                }
            }

            override fun getCount(): Int {
                return 2
            }

        }
    }

    private fun initRefreshLayout() {
        refreshLayout.setOnRefreshListener {
            model.requestGameList()
        }
    }

    private fun initRvGrid() {
        rvRank.layoutManager = if (ScreenUtils.isScreenOriatationPortrait(context))
            LinearLayoutManager(context)
        else
            GridLayoutManager(context, 2)
        if (!ScreenUtils.isScreenOriatationPortrait(context)) {
            rvRank.addItemDecoration(GridSpacingItemDecoration(2, DensityUtils.dp2px(12f), false))
        } else
            rvRank.verItemDecoration(Color.TRANSPARENT, 6f)
        rvRank.adapter =
            object : CommonAdapter<GameListBean>(context, listGame, R.layout.item_home_rank) {
                override fun convert(vh: ViewHolder, p1: GameListBean) {
                    vh.setText(R.id.tvTitle, p1.title)
                    // 点击后跳详情界面
                    vh.itemView.setOnClickListener {
                        GameDetailActivity.actionFragment(this@HomeFragment, p1)
                    }
                    // 游戏封面
                    p1.imageUrl?.let { loadImage(vh.itemView.ivGame, it) }
                    // 根据排名显示序号和背景色
                    val resBgIcon = when (vh.adapterPosition) {
                        0 -> R.drawable.bg_icon_rank_1
                        1 -> R.drawable.bg_icon_rank_2
                        2 -> R.drawable.bg_icon_rank_3
                        else -> R.drawable.bg_icon_rank
                    }
                    vh.setBackgroundRes(R.id.tvRank, resBgIcon)
                    val resTextColor = when (vh.adapterPosition) {
                        in (0..2) -> R.color.white
                        else -> R.color.colorPrimary
                    }
                    vh.setTextColorRes(R.id.tvRank, resTextColor)
                    vh.setText(R.id.tvRank, (vh.adapterPosition + 1).toString())
                    vh.itemView.setOnFocusChangeListener { v, hasFocus ->
                        // 游戏名称
                        val title = p1.title + (if (hasFocus) "<" else "")
                        vh.setText(R.id.tvTitle, title)
                    }
                }
            }
    }

    private fun updateListData(json: String) {
        if (json.isNullOrEmpty()) return
        val result = GameListBean.arrayGameListBeanFromData(json)
        listGame.clear()
        listGame.addAll(if (result.size > 10) result.subList(0, 9) else result)// 取集合的前10条记录

        attachLocalImage(listGame)
        rvRank?.adapter?.notifyDataSetChanged()
    }

    private fun updateListData(list: List<GameListBean>) {
        listGame.clear()
        listGame += list

        attachLocalImage(listGame)
        rvRank?.adapter?.notifyDataSetChanged()
        if (refreshLayout.isRefreshing)
            refreshLayout.isRefreshing = false
    }
}
