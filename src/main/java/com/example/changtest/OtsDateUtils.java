package com.example.changtest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * 日期处理工具类
 *
 * @author changjiasheng
 * @date 2024/3/4
 **/
public class OtsDateUtils {

    private static final String dateRegx =
        "^((\\d{2}(([02468][048])|([13579][26]))[\\-\\/\\s]?((((0?[13578])|(1[02]))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(3[01])))|(((0?[469])|(11))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(30)))|(0?2[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])))))|(\\d{2}(([02468][1235679])|([13579][01345789]))[\\-\\/\\s]?((((0?[13578])|(1[02]))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(3[01])))|(((0?[469])|(11))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(30)))|(0?2[\\-\\/\\s]?((0?[1-9])|(1[0-9])|(2[0-8]))))))(\\s(((0?[0-9])|([1-2][0-3]))\\:([0-5]?[0-9])((\\s)|(\\:([0-5]?[0-9])))))?$";

    public static String Time2GMT(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(date);
    }

    public static boolean isDate(String dateStr) {
        Pattern pattern = Pattern.compile(dateRegx);
        Matcher matcher = pattern.matcher(dateStr);
        return matcher.matches();
    }

    public static String covertDateStrFormat(String dateStr, String srcFormat, String descFormat) throws Exception {
        SimpleDateFormat format_src = new SimpleDateFormat(srcFormat);
        SimpleDateFormat format_desc = new SimpleDateFormat(descFormat);
        try {
            Date date = format_src.parse(dateStr);
            dateStr = format_desc.format(date);
        } catch (ParseException e) {
            throw new Exception(e);
        }
        return dateStr;
    }

    public static String getWeekByDate(Date date) {
        String weekofDay = "";
        try {
            Calendar now = Calendar.getInstance();
            now.setTime(date);
            int w = now.get(Calendar.DAY_OF_WEEK);

            switch (w) {
                case 1:
                    weekofDay = "周日";
                    break;
                case 2:
                    weekofDay = "周一";
                    break;
                case 3:
                    weekofDay = "周二";
                    break;
                case 4:
                    weekofDay = "周三";
                    break;
                case 5:
                    weekofDay = "周四";
                    break;
                case 6:
                    weekofDay = "周五";
                    break;
                case 7:
                    weekofDay = "周六";
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return weekofDay;
    }

}
