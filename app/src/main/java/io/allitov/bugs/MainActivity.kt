package io.allitov.bugs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import io.allitov.bugs.fragments.AuthorsFragment
import io.allitov.bugs.fragments.GameRulesFragment
import io.allitov.bugs.fragments.GameSettingsFragment
import io.allitov.bugs.fragments.PlayerRegistrationFragment

class MainActivity : AppCompatActivity() {

    private val tabs = listOf("Игрок", "Правила", "Авторы", "Настройки")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.vpPages)
        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)

        viewPager.isUserInputEnabled = true
        viewPager.offscreenPageLimit = tabs.size - 1
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabs.size

            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> PlayerRegistrationFragment()
                1 -> GameRulesFragment()
                2 -> AuthorsFragment()
                else -> GameSettingsFragment()
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()
    }
}
