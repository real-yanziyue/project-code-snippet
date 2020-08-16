package com.example.newsapp;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.google.android.material.tabs.TabLayout;


/**
 * A simple {@link Fragment} subclass.
 */
public class HeadlinesFragment extends Fragment {
    private Toolbar toolbar;
    private androidx.viewpager.widget.ViewPager viewPager;
    private ViewPagerAdapter adapter;
    private TabLayout tabLayout;



    public HeadlinesFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("LIFECYCLE", "Create Headlines Activity");
        View v = inflater.inflate(R.layout.fragment_headlines, container, false);
        viewPager =  v.findViewById(R.id.viewpager);
        adapter = new ViewPagerAdapter(getChildFragmentManager());
        Log.d("LIFECYCLE",adapter.toString());
        viewPager.setAdapter(adapter);
        tabLayout = v.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        adapter.notifyDataSetChanged();
        return v;
    }

    @Override
    public void onResume() {
        Log.d("LIFECYCLE", "Back to Headlines Activity");
        super.onResume();
    }



}
