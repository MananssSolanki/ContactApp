package com.example.contactapp.Adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.contactapp.ui.contacts.ContactsFragment
import com.example.contactapp.Fragments.PhoneFragment
import com.example.contactapp.Fragments.RecentFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ContactsFragment()
            1 -> RecentFragment()
            2 -> PhoneFragment()
            else -> ContactsFragment()
        }
    }
}
