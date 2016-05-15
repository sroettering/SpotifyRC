package com.hci.sroettering.spotifyrc;

/**
 * Created by sroettering on 15.05.16.
 */
public class RightListDataItem {

    public String text;
    public String duration;
    public String spotifyID;
    public int parentCategory;

    public RightListDataItem(String t, int d, String id, int parent) {
        this.text = t;
        if(d != -1) {
            this.duration = MainActivity.formatMilliseconds(d);
        } else {
            this.duration = "";
        }
        this.spotifyID = id;
        this.parentCategory = parent;
    }

}
