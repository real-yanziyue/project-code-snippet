package com.example.newsapp;


import android.Manifest;
import android.app.ProgressDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;




import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */

public class HomeFragment extends Fragment implements LocationListener {

private static final String URL_DATA = "http://ec2-54-173-221-8.compute-1.amazonaws.com:4000/androidHome";
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private List<NewsCard> listItems;
    LocationManager locationManager;
    String provider;
    Context context;
    View fragmentView;
    TextView progressMessage;
    Location location;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private String cityName ="Los Angeles";
    private String stateName ="California";

    private ProgressBar spinner;


    public HomeFragment() {
    }


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        Log.d("checkLocationpermission", "check location");
        if (ContextCompat.checkSelfPermission(this.getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(this.getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                        requestPermissions(
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
               requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        }
        else {
            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            provider = locationManager.getBestProvider(new Criteria(), false);
            location = locationManager.getLastKnownLocation(provider);
            if(location != null) {
                onLocationChanged(location);
                Double longitute = location.getLongitude();
                Double latitute = location.getLatitude();
                Geocoder geocoder = new Geocoder(this.getActivity(), Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(latitute, longitute, 1);
                    cityName = addresses.get(0).getLocality();
                    stateName = addresses.get(0).getAdminArea();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                populateFragment();
            }
            else{
                locationManager.requestLocationUpdates(provider, 400, 1, (android.location.LocationListener) this);
            }
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.

                    if (ContextCompat.checkSelfPermission(this.getActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        Log.d("onRequestPermission","allow");

                        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                        provider = locationManager.getBestProvider(new Criteria(), false);

                           location = locationManager.getLastKnownLocation(provider);
                            if(location != null){

                                onLocationChanged(location);
                                Double longitute = location.getLongitude();
                                Double latitute = location.getLatitude();

                                Geocoder geocoder = new Geocoder(this.getActivity(), Locale.getDefault());
                                List<Address> addresses = null;
                                try {
                                    addresses = geocoder.getFromLocation(latitute, longitute, 1);
                                    cityName = addresses.get(0).getLocality();
                                    stateName = addresses.get(0).getAdminArea();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                populateFragment();

                            }
                            else{
                                locationManager.requestLocationUpdates(provider, 400, 1, (android.location.LocationListener) this);
                            }


                        }

                } else {
                    populateOnlyNews();
                    Log.d("onRequestPermission","denied");

                }
                return;
            }

        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("LIFECYCLE", "Create Home Activity");
         fragmentView = inflater.inflate(R.layout.fragment_home, container, false);

        spinner = fragmentView.findViewById(R.id.progressBar1);
        progressMessage = fragmentView.findViewById(R.id.progressInfo);
        spinner.setVisibility(View.GONE);
        progressMessage.setVisibility(View.GONE);
        recyclerView = fragmentView.findViewById(R.id.homeRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        listItems = new ArrayList<>();
        adapter = new MyAdapter(listItems, getContext());
        recyclerView.setAdapter(adapter);

        checkLocationPermission();

        mSwipeRefreshLayout = fragmentView.findViewById(R.id.homeSwipeRefresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mSwipeRefreshLayout.isRefreshing()) {
                            StringRequest stringRequest = new StringRequest(Request.Method.GET,
                                    URL_DATA,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            if(listItems != null){
                                                for(int i = 0; i< listItems.size(); i++){
                                                    if(listItems.get(i).getType() == 1){
                                                        listItems.remove(i);
                                                    }
                                                }
                                            }
                                            try {
                                                JSONObject jsonObject = new JSONObject(response);
                                                JSONArray array = jsonObject.getJSONObject("response").getJSONArray("results");
                                                for(int i = 0; i < 10 ; i++){
                                                    JSONObject currObj = array.getJSONObject(i);
                                                    NewsCard listItem = new NewsCard(currObj.getString("webTitle"), 1, currObj);
                                                    listItems.add(listItem);
                                                }
                                                adapter.notifyDataSetChanged();

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {

                                        }
                                    }
                            );
                            RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                            requestQueue.add(stringRequest);
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }
                }, 1000);
            }
        });



        return fragmentView;
    }

    private void populateFragment(){
        loadWeatherData();
        loadNewsCardData();

    }
    private void populateOnlyNews(){
        loadNewsCardData();
    }



    private void loadWeatherData(){
        spinner.setVisibility(View.VISIBLE);
        progressMessage.setVisibility(View.VISIBLE);

        String URL = "https://api.openweathermap.org/data/2.5/weather?q="+ cityName+ "&units=metric&appid=c23accab53887351f9f54710a3487c7e";
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        spinner.setVisibility(View.GONE);
                        progressMessage.setVisibility(View.GONE);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            NewsCard listItem = new NewsCard(stateName, 0, jsonObject);
                            listItems.add(listItem);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }
        );
        RequestQueue requestQueue = Volley.newRequestQueue(this.getContext());
        requestQueue.add(stringRequest);

    }


    private void loadNewsCardData() {

        spinner.setVisibility(View.VISIBLE);
        progressMessage.setVisibility(View.VISIBLE);


        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                URL_DATA,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONArray array = jsonObject.getJSONObject("response").getJSONArray("results");
                            for(int i = 0; i < 10 ; i++){
                                JSONObject currObj = array.getJSONObject(i);
                                NewsCard listItem = new NewsCard(currObj.getString("webTitle"), 1, currObj);
                                listItems.add(listItem);
                            }
                            Log.d("class info", this.toString());
                            adapter = new MyAdapter(listItems, getContext());
                            recyclerView.setAdapter(adapter);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        spinner.setVisibility(View.GONE);
                        progressMessage.setVisibility(View.GONE);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }
        );
        RequestQueue requestQueue = Volley.newRequestQueue(this.getContext());
        requestQueue.add(stringRequest);

    }

    @Override
    public void onLocationChanged(Location location) {
        Double lat = location.getLatitude();
        Double lng = location.getLongitude();
        Log.i("Location info: Lat", lat.toString());
        Log.i("Location info: Lng", lng.toString());

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }



}
