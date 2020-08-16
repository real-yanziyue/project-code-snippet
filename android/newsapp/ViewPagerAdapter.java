package com.example.newsapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ViewPagerAdapter extends FragmentPagerAdapter {
    HeadlineFragment headlineFragment;


    public ViewPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        headlineFragment = new HeadlineFragment();
        String section = "";
        switch(position){
            case 0:
                section = "world";
                break;
            case 1:
                section =  "business";
                break;
            case 2:
                section = "politics";
                break;
            case 3:
                section = "sport";
                break;
            case 4:
                section =  "technology";
                break;
            case 5:
                section =  "science";
                break;
            default:
                section =  "world";

        }

        Bundle bundle = new Bundle();
        bundle.putString("message",section);
        headlineFragment.setArguments(bundle);
        return headlineFragment;
    }

    @Override
    public int getCount() {
        return 6;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch(position){
            case 0:
                return "WORLD";
            case 1:
                return "BUSINESS";
            case 2:
                return "POLITICS";
            case 3:
                return "SPORTS";
            case 4:
                return "TECHNOLOGY";
            case 5:
                return "SCIENCE";
            default:
                return "WORLD";

        }
    }



    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }
}
