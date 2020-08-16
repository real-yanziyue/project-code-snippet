package com.example.newsapp;



public class BookmarkedNewsCard {
    private String  title;
    private String imageUrl;
    private String section;
    private String date;
    private String id;
    private String webUrl;
    public BookmarkedNewsCard(String title, String imageUrl, String section, String date, String id ,String webUrl) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.section =section;
        this.date = date;
        this.id = id;
        this.webUrl = webUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getSection() {
        return section;
    }

    public String getDate() {
        return date;
    }

    public String getId() {
        return id;
    }

    public String getWebUrl() {
        return webUrl;
    }
}
