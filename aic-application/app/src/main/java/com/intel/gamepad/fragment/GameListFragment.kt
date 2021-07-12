package com.intel.gamepad.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager

import com.intel.gamepad.R
import com.intel.gamepad.activity.GameDetailActivity
import com.intel.gamepad.app.*
import com.lzy.okgo.OkGo
import com.mcxtzhang.commonadapter.rv.CommonAdapter
import com.mcxtzhang.commonadapter.rv.ViewHolder
import com.intel.gamepad.bean.GameListBean
import com.intel.gamepad.model.GameListModel
import kotlinx.android.synthetic.main.fragment_game_list.*
import kotlinx.android.synthetic.main.item_game_adapter.view.ivCover
import org.jetbrains.anko.support.v4.longToast

class GameListFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance(type: String) = GameListFragment().apply {
            arguments = Bundle().apply {
                putString("type", type)
            }
        }
    }

    private lateinit var typeName: String
    private val listGame = mutableListOf<GameListBean>()
    private lateinit var model: GameListModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.typeName = it.getString("type") ?: ""
        }
        initViewModel()
    }

    private fun initViewModel() {
        model = ViewModelProviders.of(this).get(GameListModel::class.java)
        model.showLoading.observe(this, Observer {
            if (it == false) {
                refreshLayout.finishLoadMore()
                refreshLayout.finishRefresh()
            }
        })
        model.listData.observe(this, Observer { updateListData(it) })
        model.errorMessage.observe(this, Observer { longToast(it) })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        initRefreshLayout()
        initRvGameAdapter()
    }

    private fun initRefreshLayout() {
        refreshLayout.setOnRefreshListener {
            BufferHelper.load(AppConst.QUERY_LIST)?.let { updateListData(it) }
            model.requestGameList()
        }
        refreshLayout.setOnLoadMoreListener {
            BufferHelper.load(AppConst.QUERY_LIST)?.let { updateListData(it) }
            model.requestGameList()
        }
    }

    private fun initRvGameAdapter() {
        rvGameList.layoutManager = LinearLayoutManager(context)
        rvGameList.adapter =
            object : CommonAdapter<GameListBean>(context, listGame, R.layout.item_game_adapter) {
                override fun convert(vh: ViewHolder, p1: GameListBean) {
                    vh.itemView.setOnClickListener {
                       // GameDetailActivity.actionFragment(this@GameListFragment, p1)
                    }
                    vh.setText(R.id.tvTitle, p1.title)
                    vh.setText(R.id.tvDesc, p1.intro)
                    loadImage(vh.itemView.ivCover, p1.imageUrl)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        refreshLayout.autoRefresh()
    }

    private fun updateListData(json: String) {
        if (json.isEmpty()) return
        val result = GameListBean.arrayGameListBeanFromData(json)
        updateListData(result)
    }

    private fun updateListData(list: List<GameListBean>) {
        listGame.clear()
        listGame += list.filter { it.addurl == typeName }

        attachLocalImage(listGame)
        rvGameList?.adapter?.notifyDataSetChanged()

        if (listGame.size == 0) {
            tvNoData.setText(R.string.no_game)
            tvNoData.visibility = View.VISIBLE
        } else {
            tvNoData.visibility = View.GONE
        }
    }
}
