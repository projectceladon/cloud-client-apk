package com.intel.gamepad.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.SimpleArrayMap
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders

import com.intel.gamepad.R
import com.intel.gamepad.activity.MessageActivity
import com.intel.gamepad.activity.SearchActivity
import com.intel.gamepad.model.GameListModel
import kotlinx.android.synthetic.main.fragment_game.*
import kotlinx.android.synthetic.main.include_home_title_search.*

class GameFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance() = GameFragment()
    }

    private val mapType = SimpleArrayMap<String, String>().apply {
        this.put("射击", "fps")
        this.put("赛车", "rac")
        this.put("即时战略", "rts")
        this.put("动作", "act")
        this.put("体育", "spt")
        this.put("安卓", "android")
    }

    private val listTab = listOf("射击", "即时战略", "赛车", "动作", "体育", "安卓")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        initTabLayout()
        initViewPager()
        ibtnMessage.setOnClickListener { MessageActivity.actionStartFragment(this@GameFragment) }
        tvSearch.setOnClickListener { SearchActivity.actionStartFragment(this@GameFragment) }
    }

    private fun initTabLayout() {
        listTab.forEach {
            tabLayout.addTab(tabLayout.newTab().setText(it))
        }
        tabLayout.setupWithViewPager(viewPager, true)
    }

    private fun initViewPager() {
        viewPager.offscreenPageLimit = 1
        viewPager.adapter = object :
            FragmentStatePagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                val type = mapType[listTab[position]]
                return GameListFragment.newInstance(type ?: "fps")
            }

            override fun getCount(): Int {
                return listTab.size
            }

            override fun getPageTitle(position: Int): CharSequence? {
                //return super.getPageTitle(position)
                return listTab[position]
            }
        }
    }
}
