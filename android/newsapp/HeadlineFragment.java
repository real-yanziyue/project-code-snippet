package com.example.newsapp;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class HeadlineFragment extends Fragment {
    private TextView textView;
    private  String URL_DATA = "";
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private List<NewsCard> listItems;
    View fragmentView;
    TextView progressMessage;
    private ProgressBar spinner;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    public HeadlineFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("LIFECYCLE", "Create Headline Activity");
        fragmentView =  inflater.inflate(R.layout.headline_fragment, container, false);
        String section = getArguments().getString("message");
        URL_DATA = "http://ec2-54-173-221-8.compute-1.amazonaws.com:4000/androidHeadlines/"+section;
        spinner = fragmentView.findViewById(R.id.headlineprogressBar);
        progressMessage = fragmentView.findViewById(R.id.headlineprogressInfo);
        recyclerView = fragmentView.findViewById(R.id.headlineRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        listItems = new ArrayList<>();
        adapter = new HeadlinesTabAdapter(listItems, getContext());
        recyclerView.setAdapter(adapter);
        loadNewsCardData();


        mSwipeRefreshLayout = fragmentView.findViewById(R.id.headlineSwipeRefresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mSwipeRefreshLayout.isRefreshing()) {

                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                    (Request.Method.GET, URL_DATA, null, new Response.Listener<JSONObject>() {

                                        @Override
                                        public void onResponse(JSONObject response) {
                                            if(listItems!= null){
                                                for(int i = 0; i < listItems.size(); i++){
                                                    listItems.remove(i);
                                                }
                                            }
                                            try {
                                                JSONArray array = response.getJSONObject("response").getJSONArray("results");
                                                for(int i = 0; i < 10 ; i++){
                                                    JSONObject currObj = array.getJSONObject(i);
                                                    NewsCard listItem = new NewsCard(currObj.getString("webTitle"), 1, currObj);
                                                    listItems.add(listItem);
                                                }

                                            } catch (JSONException e) {
                                                System.out.println(e.getMessage());
                                                e.printStackTrace();
                                            }

                                            adapter.notifyDataSetChanged();


                                        }
                                    },
                                            new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    System.out.println(error);

                                                }
                                            }
                                    );
                            RequestQueue requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
                            requestQueue.add(jsonObjectRequest);

                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }
                }, 1000);
            }
        });


        return fragmentView;
    }

    private void loadNewsCardData() {


        spinner.setVisibility(View.VISIBLE);
        progressMessage.setVisibility(View.VISIBLE);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, URL_DATA, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONObject("response").getJSONArray("results");
                            for(int i = 0; i < 10 ; i++){
                                JSONObject currObj = array.getJSONObject(i);
                                NewsCard listItem = new NewsCard(currObj.getString("webTitle"), 1, currObj);
                                listItems.add(listItem);
                            }




                        } catch (JSONException e) {
                            System.out.println(e.getMessage());
                            e.printStackTrace();
                        }
                        spinner.setVisibility(View.GONE);
                        progressMessage.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println(error);

                    }
                }
        );
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        requestQueue.add(jsonObjectRequest);

    }

    public void update(){
        adapter.notifyDataSetChanged();
    }


@Override
    public void onResume(){
        super.onResume();
        Log.d("LIFECYCLE", "Back to Headline Activity");

    }
}
