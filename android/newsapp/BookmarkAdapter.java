package com.example.newsapp;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {
   private Context context;
   private List<BookmarkedNewsCard> mData;
    private ArrayList<String> sharedList;
    private BookmarkFragment bookmarkFragment;


    public BookmarkAdapter(Context context, List<BookmarkedNewsCard> mData, BookmarkFragment bookmarkFragment) {
        this.context = context;
        this.mData = mData;
        this.bookmarkFragment = bookmarkFragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        LayoutInflater inflater = LayoutInflater.from(context);
        v = inflater.inflate(R.layout.bookmarked_card,parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        holder.title.setText(mData.get(position).getTitle());
        holder.section.setText(mData.get(position).getSection());
        holder.date.setText(mData.get(position).getDate());
        Picasso.with(context).load(mData.get(position).getImageUrl()).into(holder.image);
        final String toastTitle = mData.get(position).getTitle();
        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, DetailActivity.class);
                String id = mData.get(position).getId();
                intent.putExtra("ID", id);
                context.startActivity(intent);
            }
        });

       holder.card.setOnLongClickListener(new View.OnLongClickListener() {
           @Override
           public boolean onLongClick(View v) {
               final Dialog dialog = new Dialog(context);
               dialog.setContentView(R.layout.dialog);
               ImageView image = dialog.findViewById(R.id.dialogImage);
               TextView title = dialog.findViewById(R.id.dialogtitle);
               ImageView twitter = dialog.findViewById(R.id.dialogtwitter);
               final ImageView dialogBookmark = dialog.findViewById(R.id.dialogbookmark);

               SharedPreferences sharedPreferences = context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE );
               final BookmarkedNewsCard bookmarkedNewsCard = mData.get(position);

               String id ="";
               id= bookmarkedNewsCard.getId();
               if(sharedPreferences.contains(id)){
                   dialogBookmark.setImageResource(R.drawable.ic_bookmark_black_24dp);
               }



               String newsTitle = bookmarkedNewsCard.getTitle();
               title.setText(newsTitle);
               String imageUrl = bookmarkedNewsCard.getImageUrl();
               Picasso.with(context).load(imageUrl).into(image);


               dialogBookmark.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View v) {
                       SharedPreferences sharedPreferences = context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE );
                       SharedPreferences.Editor editor = sharedPreferences.edit();
                       Gson gson = new Gson();
                       String id = bookmarkedNewsCard.getId();
                       String toastTitle = bookmarkedNewsCard.getTitle();
                       String prevjson = sharedPreferences.getString("shared list", null);
                       Type type = new TypeToken<ArrayList<String>>() {}.getType();
                       sharedList = gson.fromJson(prevjson, type);
                       for (int i = 0; i < sharedList.size(); i++) {
                           if (sharedList.get(i).equals(id)) {
                               sharedList.remove(i);
                               editor.remove(id);
                               mData.remove(position);
                               break;
                           }
                       }
                       String json = gson.toJson(sharedList);
                       editor.remove("shared list");
                       editor.putString("shared list", json);
                       editor.commit();

                       holder.bookmark.setImageResource(R.drawable.ic_bookmark_border_black_24dp);
                       dialogBookmark.setImageResource(R.drawable.ic_bookmark_border_black_24dp);
                       Toast.makeText(context, "\""+ toastTitle +"\"" + " was removed from bookmarks", Toast.LENGTH_SHORT).show();
                       notifyDataSetChanged();
                       dialog.dismiss();
                       if(sharedList.size() == 0){
                          bookmarkFragment.getNoBookmarkedInfo().setVisibility(View.VISIBLE);
                       }


                   }
               });


               twitter.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View v) {
                       String webUrl = mData.get(position).getWebUrl();
                       String tweeterUrl = "https://twitter.com/intent/tweet?text=Check out this Link:&url=" + webUrl + "&hashtags=CSCI571NewsSearch";
                       Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweeterUrl));
                       context.startActivity(browserIntent);
                   }
               });
               dialog.show();
               return true;

           }
       });

       holder.bookmark.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               SharedPreferences sharedPreferences = context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE );
               SharedPreferences.Editor editor = sharedPreferences.edit();
               Gson gson = new Gson();
               String prevjson = sharedPreferences.getString("shared list", null);
               Type type = new TypeToken<ArrayList<String>>() {}.getType();
               sharedList = gson.fromJson(prevjson, type);
               String id = mData.get(position).getId();
                   for(int i = 0; i< sharedList.size();i++) {
                       if (sharedList.get(i).equals(id)) {
                           sharedList.remove(i);
                           editor.remove(id);
                           mData.remove(position);
                           break;
                       }

                   }
                   String json = gson.toJson(sharedList);
                   editor.remove("shared list");
                   editor.putString("shared list", json);
                   editor.commit();
                   notifyDataSetChanged();
               if(sharedList.size() == 0){
                   bookmarkFragment.getNoBookmarkedInfo().setVisibility(View.VISIBLE);
               }

               Toast.makeText(context, "\""+ toastTitle +"\"" + " was removed from bookmarks", Toast.LENGTH_SHORT).show();
           }
       });

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView title;
        ImageView image;
        TextView section;
        TextView date;
        CardView card;
        ImageView bookmark;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.bookmarkedCardTitle);
            image = itemView.findViewById(R.id.bookmarkedCardImage);
            section = itemView.findViewById(R.id.bookmarkedCardSection);
            date = itemView.findViewById(R.id.bookmarkedCardDate);
            card = itemView.findViewById(R.id.bookmarkedCard);
            bookmark = itemView.findViewById(R.id.bookmarkedCardBookmark);
        }
    }

}
