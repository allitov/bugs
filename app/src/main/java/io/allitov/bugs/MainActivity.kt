package io.allitov.bugs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.allitov.bugs.fragments.AuthorsFragment
import io.allitov.bugs.fragments.GameRulesFragment
import io.allitov.bugs.fragments.GameSettingsFragment
import io.allitov.bugs.fragments.PlayerRegistrationFragment
import io.allitov.bugs.fragments.RecordsFragment

class MainActivity : AppCompatActivity() {

    private val tabs = listOf("Игрок", "Рекорды", "Правила", "Авторы", "Настройки")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.vpPages)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.isUserInputEnabled = true
        viewPager.offscreenPageLimit = tabs.size - 1
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabs.size

            override fun createFragment(position: Int): Fragment = when (position) {
                PAGE_PLAYER -> PlayerRegistrationFragment()
                PAGE_RECORDS -> RecordsFragment()
                PAGE_RULES -> GameRulesFragment()
                PAGE_AUTHORS -> AuthorsFragment()
                else -> GameSettingsFragment()
            }
        }
        viewPager.setCurrentItem(intent.getIntExtra(EXTRA_SELECTED_PAGE, PAGE_PLAYER), false)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()
    }

    companion object {
        const val EXTRA_SELECTED_PAGE = "io.allitov.bugs.extra.SELECTED_PAGE"
        const val PAGE_PLAYER = 0
        const val PAGE_RECORDS = 1
        const val PAGE_RULES = 2
        const val PAGE_AUTHORS = 3
        const val PAGE_SETTINGS = 4
    }
}
