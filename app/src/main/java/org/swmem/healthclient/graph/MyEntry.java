package org.swmem.healthclient.graph;

import org.swmem.healthclient.utils.Utility;

/**
 * Created by hyunjae on 16. 7. 28.
 */
public class MyEntry {

    private int index;
    private float value;
    private int color;
    private String date;


    private long longDate;


    public MyEntry(int index, float value, int color, String date) {
        this.index = index;
        this.value = value;
        this.color = color;
        this.date = date;
        this.longDate = Utility.cursorDateToLong(date);
    }

    public long getLongDate() {
        return longDate;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
