package com.example.newsapp;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;





public class HeadlinesTabAdapter extends RecyclerView.Adapter {

    private List<NewsCard> listItems;
    private Context context;
    private ArrayList<String> sharedList ;



    public HeadlinesTabAdapter(List<NewsCard> listItems, Context context) {
        this.listItems = listItems;
        this.context = context;
    }



    @Override
    public int getItemViewType(int position) {
        return listItems.get(position).getType();

    }



    public class ViewHolderTwo extends RecyclerView.ViewHolder implements View.OnClickListener , View.OnLongClickListener {
        public TextView title;
        public ImageView imageView;
        public TextView date;
        public TextView section;
        private ImageView bookmark;
        ImageView dialogBookmark;
        String id ="";
        String toastTitle ="";

        public ViewHolderTwo(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.cardHead);
            imageView = itemView.findViewById(R.id.CardImage);
            date = itemView.findViewById(R.id.cardDate);
            section = itemView.findViewById(R.id.CardSection);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            bookmark = itemView.findViewById(R.id.Cardbookmark);
            bookmark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    SharedPreferences sharedPreferences = context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE );
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    Gson gson = new Gson();
                    try {
                        id =listItems.get(getAbsoluteAdapterPosition()).getJsonObject().getString("id");
                        toastTitle = listItems.get(getAbsoluteAdapterPosition()).getJsonObject().getString("webTitle");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
                        Toast.makeText(context,"\""+ toastTitle +"\"" + " was added to bookmarks", Toast.LENGTH_SHORT).show();
                    }
                    else {

                        String prevjson = sharedPreferences.getString("shared list", null);
                        Type type = new TypeToken<ArrayList<String>>() {}.getType();
                        sharedList = gson.fromJson(prevjson, type);

                        for (int i = 0; i < sharedList.size(); i++) {
                            if (sharedList.get(i).equals(id)) {
                                sharedList.remove(i);
                                editor.remove(id);
                            }
                            String json = gson.toJson(sharedList);
                            editor.remove("shared list");
                            editor.putString("shared list", json);
                            editor.commit();
                            bookmark.setImageResource(R.drawable.ic_bookmark_border_black_24dp);
                            Toast.makeText(context, "\""+ toastTitle +"\"" + " was removed from bookmarks", Toast.LENGTH_SHORT).show();
                        }

                    }
                }
            });

        }

        @Override
        public void onClick(View v) {
            NewsCard news = listItems.get(getAbsoluteAdapterPosition());

            Intent intent = new Intent(context, DetailActivity.class);
            try {
                id =news.getJsonObject().getString("id");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            intent.putExtra("ID", id);
            context.startActivity(intent);
        }

        @Override
        public boolean onLongClick(View v) {
            JSONObject obj= listItems.get(getAbsoluteAdapterPosition()).getJsonObject();
            final Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.dialog);
            ImageView image = dialog.findViewById(R.id.dialogImage);
            TextView title = dialog.findViewById(R.id.dialogtitle);
            ImageView twitter = dialog.findViewById(R.id.dialogtwitter);
            dialogBookmark = dialog.findViewById(R.id.dialogbookmark);
            SharedPreferences sharedPreferences = context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE );



            try {
                id= obj.getString("id");
                if(sharedPreferences.contains(id)){
                    dialogBookmark.setImageResource(R.drawable.ic_bookmark_black_24dp);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


            try {
                String newsTitle = obj.getString("webTitle");
                title.setText(newsTitle);
                String imageUrl = obj.getJSONObject("blocks").getJSONObject("main").getJSONArray("elements").getJSONObject(0).getJSONArray("assets").getJSONObject(0).getString("file");
                Picasso.with(context).load(imageUrl).into(image);

            } catch (JSONException e) {
                String imageUrl = "https://assets.guim.co.uk/images/eada8aa27c12fe2d5afa3a89d3fbae0d/fallback-logo.png";
                Picasso.with(context).load(imageUrl).into(image);
                e.printStackTrace();
            }
            dialogBookmark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences sharedPreferences = context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE );
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    Gson gson = new Gson();
                    try {
                        id = listItems.get(getAbsoluteAdapterPosition()).getJsonObject().getString("id");
                        toastTitle = listItems.get(getAbsoluteAdapterPosition()).getJsonObject().getString("webTitle");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
                        dialogBookmark.setImageResource(R.drawable.ic_bookmark_black_24dp);
                        bookmark.setImageResource(R.drawable.ic_bookmark_black_24dp);
                        Toast.makeText(context,"\""+ toastTitle +"\"" + " was added to bookmarks", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        String prevjson = sharedPreferences.getString("shared list", null);
                        Type type = new TypeToken<ArrayList<String>>() {}.getType();
                        sharedList = gson.fromJson(prevjson, type);

                        for (int i = 0; i < sharedList.size(); i++) {
                            if (sharedList.get(i).equals(id)) {
                                sharedList.remove(i);
                                editor.remove(id);
                            }
                            String json = gson.toJson(sharedList);
                            editor.remove("shared list");
                            editor.putString("shared list", json);
                            editor.commit();
                            bookmark.setImageResource(R.drawable.ic_bookmark_border_black_24dp);
                            dialogBookmark.setImageResource(R.drawable.ic_bookmark_border_black_24dp);

                            Toast.makeText(context, "\""+ toastTitle +"\"" + " was removed from bookmarks", Toast.LENGTH_SHORT).show();
                        }

                    }
                    notifyDataSetChanged();




                }
            });


            twitter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String webUrl="";
                    try {
                        webUrl = listItems.get(getAbsoluteAdapterPosition()).getJsonObject().getString("webUrl");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String tweeterUrl = "https://twitter.com/intent/tweet?text=Check out this Link:&url=" + webUrl + "&hashtags=CSCI571NewsSearch";
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweeterUrl));
                    context.startActivity(browserIntent);

                }
            });

            dialog.show();

            return true;
        }

    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view;
        view = layoutInflater.inflate(R.layout.list_card, parent,false);
        return new ViewHolderTwo(view);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            ViewHolderTwo viewHolderTwo = (ViewHolderTwo) holder;
            viewHolderTwo.title.setText(listItems.get(position).getHead());
            JSONObject obj = listItems.get(position).getJsonObject();

            String imageUrl = "";
            String time ="";
            String section ="";
            String id ="";
            try {
                time = obj.getString("webPublicationDate");
                section = obj.getString("sectionName");
                id= obj.getString("id");
                imageUrl = obj.getJSONObject("blocks").getJSONObject("main").getJSONArray("elements").getJSONObject(0).getJSONArray("assets").getJSONObject(0).getString("file");
            } catch (JSONException e) {
                imageUrl = "https://assets.guim.co.uk/images/eada8aa27c12fe2d5afa3a89d3fbae0d/fallback-logo.png";
                e.printStackTrace();
            }


            LocalDateTime ldt = LocalDateTime.now();
            ZoneId zoneId = ZoneId.of( "America/Los_Angeles" );
            ZonedDateTime zdtAtLa = ldt.atZone( zoneId );
            ZonedDateTime zdtAtNews = ZonedDateTime.parse(time);
            ZonedDateTime zdtAtNewsLA = zdtAtNews
                    .withZoneSameInstant( ZoneId.of( "America/Los_Angeles" ) ); 
            long diff = ChronoUnit.SECONDS.between(zdtAtNewsLA, zdtAtLa);
            if(diff <= 60){
                time = diff + " s ago";
            }
            else if(diff <= 3600 ){
                long minuteDiff = ChronoUnit.MINUTES.between(zdtAtNewsLA, zdtAtLa);
                time =  minuteDiff +" m ago";
            }
            else if(diff <= 86400){
                long minuteDiff = ChronoUnit.MINUTES.between(zdtAtNewsLA, zdtAtLa);
                time = minuteDiff/60 + " h ago";
            }
            else {
                long minuteDiff = ChronoUnit.MINUTES.between(zdtAtNewsLA, zdtAtLa);
                time = minuteDiff/1440 +" d ago";

            }

            viewHolderTwo.section.setText("|"+ section);
            viewHolderTwo.date.setText(time);
            Picasso.with(context).load(imageUrl).into(viewHolderTwo.imageView);


        SharedPreferences sharedPreferences = context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE );
        Gson gson = new Gson();
        String json = sharedPreferences.getString("shared list", null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();

        sharedList = gson.fromJson(json, type);

        if(sharedList == null) {
            sharedList = new ArrayList<>();
        }

        for(int i = 0 ; i < sharedList.size(); i++){
            if(sharedList.get(i).equals(id)){
                viewHolderTwo.bookmark.setImageResource(R.drawable.ic_bookmark_black_24dp);

            }

        }


        }


    @Override
    public int getItemCount() {
        return listItems.size();
    }



}