package org.swmem.healthclient.graph;

/**
 * Created by hyunjae on 16. 7. 28.
 */
public class MyEntry {

    private int index;
    private float value;
    private int color;

    public MyEntry(int index, float value, int color) {
        this.index = index;
        this.value = value;
        this.color = color;
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
}
