package com.example.newsapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class BookmarkFragment extends Fragment {
    private ArrayList<String> sharedList;
    List<BookmarkedNewsCard> listItems;
    BookmarkAdapter bookmarkAdapter;
    RecyclerView recyclerView;
    private ProgressBar spinner;
    private TextView progressMessage;
    private  TextView noBookmarkedInfo;



    public BookmarkFragment() {
        // Required empty public constructor
    }

    public TextView getNoBookmarkedInfo(){
        return noBookmarkedInfo;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("LIFECYCLE", "Create bookmark frag");
        // Inflate the layout for this fragment
        View inflate = inflater.inflate(R.layout.fragment_bookmark, container, false);
        spinner = inflate.findViewById(R.id.bookmarkProgressBar);
        progressMessage = inflate.findViewById(R.id.bookmarkProgressInfo);
        noBookmarkedInfo = inflate.findViewById(R.id.noBookmarkedinfo);
        spinner.setVisibility(View.GONE);
        progressMessage.setVisibility(View.GONE);
        noBookmarkedInfo.setVisibility(View.GONE);


        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("shared preferences", Context.MODE_PRIVATE );
        Gson gson = new Gson();
        String json = sharedPreferences.getString("shared list", null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        sharedList = gson.fromJson(json, type);
        if(sharedList == null || sharedList.size() == 0){
            sharedList = new ArrayList<>();
            noBookmarkedInfo.setVisibility(View.VISIBLE);
            return inflate;

            // show no bookmarked articles
        }
        listItems = new ArrayList<>();
        for(int i = 0; i < sharedList.size(); i++){
            String id = sharedList.get(i);
            loadNewsCardData(id);
        }

       recyclerView = inflate.findViewById(R.id.bookmarkRecyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(),2));
        bookmarkAdapter = new BookmarkAdapter(getContext(),listItems, this);
        recyclerView.setAdapter(bookmarkAdapter);

        return inflate;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("LIFECYCLE", "Back to bookmark frag");
    }

    private void loadNewsCardData(String id) {

        spinner.setVisibility(View.VISIBLE);
        progressMessage.setVisibility(View.VISIBLE);
        final String newsId = id;

        String url = "https://content.guardianapis.com/"+id+"?api-key=eaa8eb7e-f96d-402a-932f-4f888b4df04f&show-blocks=all";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onResponse(JSONObject response) {
                        String newsTitle = "";
                        String imageUrl ="";
                        String section ="";
                        String date = "";
                        String webUrl="";
                        try {

                              JSONObject content = response.getJSONObject("response").getJSONObject("content");
                              newsTitle = content.getString("webTitle");
                              section = content.getString("sectionName");
                              date = content.getString("webPublicationDate");
                              webUrl = content.getString("webUrl");
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                            Date strDate = formatter.parse(date);

                            System.out.println(strDate.toString());
                            date = strDate.toString().substring(8,11) + strDate.toString().substring(4,7);
                                JSONObject asset = content.getJSONObject("blocks").getJSONObject("main").getJSONArray("elements").getJSONObject(0).getJSONArray("assets").getJSONObject(0);
                                imageUrl = asset.getString("file");

                        } catch (JSONException | ParseException e) {
                            imageUrl = "https://assets.guim.co.uk/images/eada8aa27c12fe2d5afa3a89d3fbae0d/fallback-logo.png";
                            System.out.println(e.getMessage());
                            e.printStackTrace();
                        }

                        BookmarkedNewsCard bookmarkedNewsCard = new BookmarkedNewsCard(newsTitle,imageUrl," | "+section,date,newsId,webUrl );
                        listItems.add(bookmarkedNewsCard);
                        bookmarkAdapter.notifyDataSetChanged();
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
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        requestQueue.add(jsonObjectRequest);

    }
}
