package com.example.newsapp;


import org.json.JSONObject;

public class NewsCard {
    private String head;
    private int type;
    private JSONObject jsonObject;

    public NewsCard(String head, int type, JSONObject jsonObject) {
        this.head = head;
        this.type = type;
        this.jsonObject =jsonObject;
    }

    public String getHead() {
        return head;
    }

    public int getType() {
        return type;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }
}
