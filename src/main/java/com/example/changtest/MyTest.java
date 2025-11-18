/**
 * 
 */
package com.example.changtest;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class MyTest {

    public MyTest() {
        // TODO Auto-generated constructor stuba
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

}
