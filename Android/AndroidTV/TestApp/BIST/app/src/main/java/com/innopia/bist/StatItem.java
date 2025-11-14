package com.innopia.bist;

public class StatItem {
    private String label;
    private String value;
    private String change;
    private int iconResourceId;

    public StatItem(String label, String value, String change, int iconResourceId) {
        this.label = label;
        this.value = value;
        this.change = change;
        this.iconResourceId = iconResourceId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public void setIconResourceId(int iconResourceId) {
        this.iconResourceId = iconResourceId;
    }
}
