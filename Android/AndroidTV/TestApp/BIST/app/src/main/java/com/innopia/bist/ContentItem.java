package com.innopia.bist;

public class ContentItem {
    private String title;
    private String subtitle;
    private int imageResourceId;

    public ContentItem(String title, String subtitle, int imageResourceId) {
        this.title = title;
        this.subtitle = subtitle;
        this.imageResourceId = imageResourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
    }
}
