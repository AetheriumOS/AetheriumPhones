package net.aetherium.phones.phone

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class PhonePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DialpadFragment()
            1 -> ContactsFragment()
            2 -> RecentsFragment()
            else -> DialpadFragment()
        }
    }
}
