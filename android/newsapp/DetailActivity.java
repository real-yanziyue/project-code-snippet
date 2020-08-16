package com.example.newsapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DetailActivity extends AppCompatActivity {
    TextView progressMessage;
    private ProgressBar spinner;
    TextView title;
    TextView section;
    TextView date;
    TextView viewFullLink;
    String imageUrl;
    ImageView image;
    Toolbar detailToolbar;
    TextView body;
    ImageView twitter;
    ImageView bookmark;
    String  webUrl;
    String titleText;
    String id;
    ArrayList<String> sharedList;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_activity);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        id = extras.getString("ID");

        detailToolbar =  findViewById(R.id.detailActivityToolbar);
        setSupportActionBar(detailToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

       title = findViewById(R.id.detailtitle);
       image = findViewById(R.id.detailImage);
       body = findViewById(R.id.detailbody);
       section =findViewById(R.id.detailSection);
       date = findViewById(R.id.detailDate);
       viewFullLink = findViewById(R.id.viewFullAritcleLink);
       viewFullLink.setVisibility(View.GONE);

        spinner = findViewById(R.id.detailPageprogressBar);
        progressMessage = findViewById(R.id.detailPageprogressInfo);
        twitter = findViewById(R.id.bluetwitter);
        bookmark = findViewById(R.id.toolbarbookmark);
        spinner.setVisibility(View.VISIBLE);
        progressMessage.setVisibility(View.VISIBLE);
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", Context.MODE_PRIVATE );
        if(sharedPreferences.contains(id)){
            bookmark.setImageResource(R.drawable.ic_bookmark_black_24dp);
        }




        String URL_DATA = "http://ec2-54-173-221-8.compute-1.amazonaws.com:4000/androidDetail/"+id;
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                URL_DATA,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONObject content = jsonObject.getJSONObject("response").getJSONObject("content");
                            titleText = content.getString("webTitle");
                            webUrl = content.getString("webUrl");
                            String sectionName = content.getString("sectionName");
                            section.setText(sectionName);
                            String time = content.getString("webPublicationDate");
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                            Date strDate = null;
                            try {
                                strDate = formatter.parse(time);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            System.out.println(strDate.toString());
                            time = strDate.toString().substring(8,11) + strDate.toString().substring(4,7) + strDate.toString().substring(23,27) ;
                            date.setText(time);
                            JSONArray bodyArr = content.getJSONObject("blocks").getJSONArray("body");
                            StringBuilder stringBuilder = new StringBuilder();
                            for(int i = 0; i < bodyArr.length(); i++){
                                stringBuilder.append(bodyArr.getJSONObject(i).getString("bodyHtml"));
                            }
                            String bodyText = stringBuilder.toString();
                            title.setText(titleText);
                            body.setText(Html.fromHtml(bodyText,Html.FROM_HTML_MODE_LEGACY));
                            getSupportActionBar().setTitle(titleText);
                            JSONObject asset = content.getJSONObject("blocks").getJSONObject("main").getJSONArray("elements").getJSONObject(0).getJSONArray("assets").getJSONObject(0);
                            imageUrl = asset.getString("file");
                            Picasso.with(getApplicationContext()).load(imageUrl).into(image);

                        } catch (JSONException e) {
                            if(imageUrl == null){
                                imageUrl = "https://assets.guim.co.uk/images/eada8aa27c12fe2d5afa3a89d3fbae0d/fallback-logo.png";
                                Picasso.with(getApplicationContext()).load(imageUrl).into(image);
                            }
                            e.printStackTrace();
                        }
                        spinner.setVisibility(View.GONE);
                        progressMessage.setVisibility(View.GONE);
                        viewFullLink.setVisibility(View.VISIBLE);

                        twitter.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String tweeterUrl = "https://twitter.com/intent/tweet?text=Check out this Link:&url=" + webUrl + "&hashtags=CSCI571NewsSearch";
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweeterUrl));
                                startActivity(browserIntent);
                            }
                        });

                        bookmark.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", Context.MODE_PRIVATE );
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                Gson gson = new Gson();
                                if(!sharedPreferences.contains(id)){
                                    String prevjson = sharedPreferences.getString("shared list", null);
                                    Type type = new TypeToken<ArrayList<String>>() {}.getType();
                                    sharedList = gson.fromJson(prevjson, type);
                                    if(sharedList == null){
                                        sharedList = new ArrayList<>();
                                    }
                                    sharedList.add(id);
                                    String json = gson.toJson(sharedList);
                                    editor.remove("shared list");
                                    editor.putString("shared list", json);
                                    editor.putBoolean(id, true);
                                    editor.commit();
                                    bookmark.setImageResource(R.drawable.ic_bookmark_black_24dp);
                                    Toast.makeText(getApplicationContext(),"\""+ titleText +"\"" + " was added to bookmarks", Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    String prevjson = sharedPreferences.getString("shared list", null);
                                    Type type = new TypeToken<ArrayList<String>>() {}.getType();
                                    sharedList = gson.fromJson(prevjson, type);
                                    for (int i = 0; i < sharedList.size(); i++) {
                                        if (sharedList.get(i).equals(id)) {
                                            sharedList.remove(i);
                                            editor.remove(id);
                                            break;
                                        }
                                    }
                                    String json = gson.toJson(sharedList);
                                    editor.remove("shared list");
                                    editor.putString("shared list", json);
                                    editor.commit();
                                    bookmark.setImageResource(R.drawable.ic_bookmark_border_black_24dp);
                                    Toast.makeText(getApplicationContext(), "\""+ titleText +"\"" + " was removed from bookmarks", Toast.LENGTH_SHORT).show();
                                }

                            }
                        });

                        viewFullLink.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                                startActivity(browserIntent);
                            }
                        });

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }
        );
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);






    }
}
