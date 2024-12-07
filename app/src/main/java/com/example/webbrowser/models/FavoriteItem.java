package com.example.webbrowser.models;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

public class FavoriteItem {

    private String title;
    private String url;

    public FavoriteItem() {}

    public FavoriteItem(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == null)
            return false;
        return this.title.equals(((FavoriteItem) obj).title);
    }
}
