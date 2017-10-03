package com.dreambandsdk;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by seanf on 10/3/2017.
 */

public class TableRow implements Parcelable {
    // Private members
    private String key, value;

    // Constructor
    private TableRow(Parcel in) {
        key = in.readString();
        value = in.readString();
    }

    public TableRow(String keyIn, String valueIn) {
        key = keyIn;
        value = valueIn;
    }

    @Override
    public int describeContents() {
        return this.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeString(value);
    }

    public static final Parcelable.Creator<TableRow> CREATOR
            = new Parcelable.Creator<TableRow>() {
        public TableRow createFromParcel(Parcel in) {
            return new TableRow(in);
        }

        public TableRow[] newArray(int size) {
            return new TableRow[size];
        }
    };

    @Override
    public String toString() {
        return "{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}