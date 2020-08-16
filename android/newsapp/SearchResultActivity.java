package com.example.newsapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class SearchResultActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private TextView text;
    private RecyclerView recyclerView;
    private List<NewsCard>listItems;
    private HeadlinesTabAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    String URL_DATA ;
    private ProgressBar spinner;
    private TextView progressMessage;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        Log.d("LIFECYCLE","Create Search Activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_result_activity);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String query = extras.getString("query");

        toolbar =  findViewById(R.id.searchResultActivityToolbar);
        toolbar.setTitle("Search results for "+ query);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        URL_DATA ="http://ec2-54-173-221-8.compute-1.amazonaws.com:4000/androidSearch/"+ query;

        spinner = findViewById(R.id.searchPageprogressBar);
        progressMessage = findViewById(R.id.searchPageprogressInfo);
        recyclerView = findViewById(R.id.searchResultRecyclerview);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listItems = new ArrayList<>();
        adapter = new HeadlinesTabAdapter(listItems, this);
        recyclerView.setAdapter(adapter);
        loadNewsCardData();
        mSwipeRefreshLayout = findViewById(R.id.searchSwipeRefresh);
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
                            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
                            requestQueue.add(jsonObjectRequest);

                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }
                }, 1000);
            }
        });

    }


    private void loadNewsCardData() {


        spinner.setVisibility(View.VISIBLE);
        progressMessage.setVisibility(View.VISIBLE);
        System.out.println("Inside loadNewsCardData");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, URL_DATA, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            JSONArray array = response.getJSONObject("response").getJSONArray("results");
                            for(int i = 0; i<array.length() && i < 10 ; i++){
                                JSONObject currObj = array.getJSONObject(i);
                                NewsCard listItem = new NewsCard(currObj.getString("webTitle"), 1, currObj);
                                listItems.add(listItem);
                            }
                            adapter.notifyDataSetChanged();



                        } catch (JSONException e) {
                            System.out.println(e.getMessage());
                            e.printStackTrace();
                        }
                        spinner.setVisibility(View.GONE);
                        progressMessage.setVisibility(View.GONE);

                    }
                },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                System.out.println(error);

                            }
                        }
                );
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(jsonObjectRequest);

    }
}
