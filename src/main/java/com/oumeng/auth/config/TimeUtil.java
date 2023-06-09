package com.oumeng.auth.config;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtil {

    public static String DATE_FORMAT_STR_ALL = "yyyy-MM-dd HH:mm:ss";

    public static String DATE_FORMAT_STR = "yyyy-MM-dd";

    private static SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT_STR_ALL, Locale.getDefault());

    public static String getCurrentDateTime() {
        return SDF.format(new Date());
    }

    public static String getCurrentDate() {
        return new SimpleDateFormat(DATE_FORMAT_STR, Locale.getDefault()).format(new Date());
    }

    public static String getTime(Date date) {
        return SDF.format(date);
    }

    public static String getFormatTime(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = new Date();
        return sdf.format(date);
    }

    public static String getFormatTime(String format, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    //计算两个时间相差的秒数
    public static long getTime(String startTime, String endTime) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long eTime = 0, sTime = 0;
        try {
            eTime = df.parse(endTime).getTime();
            sTime = df.parse(startTime).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long diff = (eTime - sTime) / 1000;
        return diff;
    }

    //计算两个时间相差的天数
    public static long getTimeDay(String startTime, String endTime) {
        return getTime(startTime, endTime)/(24*60*60);
    }

    public static Date getDate(String date) {
        try {
            return SDF.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date getDateByFormat(String date, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getCurrentYearMonth() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMM");
        Date date = new Date();
        return sdf.format(date);
    }

    public static String getCurrentYearMonthMysql() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        Date date = new Date();
        return sdf.format(date);
    }

    public static int getCurrentQuarterly() {
        String dateTime = getCurrentDateTime();
        int month = Integer.parseInt(dateTime.substring(5, 7));
        return month % 3 == 0 ? month / 3 : month / 3 + 1;
    }

    public static int getQuarterlyByMonth(String month) {
        int monthNum = Integer.parseInt(month.substring(5, 7));
        return monthNum % 3 == 0 ? monthNum / 3 : monthNum / 3 + 1;
    }

    public static int getCurrentMonthNum() {
        return new Date().getMonth() + 1;
    }

    public static String getQuarterByMonth(String month) {
        int monthNum = Integer.parseInt(month.substring(5, 7));
        return month.substring(0, 4) + "-q" + (monthNum % 3 == 0 ? monthNum / 3 : monthNum / 3 + 1);
    }

    public static int getQuarterlyByMonthNum(int monthNum) {
        return monthNum % 3 == 0 ? monthNum / 3 : monthNum / 3 + 1;
    }

    public static String getCurrentYearQuarterly() {
        int quarterly = TimeUtil.getCurrentQuarterly();
        String year = TimeUtil.getCurrentDateTime().substring(0, 4);
        return year + "-q" + quarterly;
    }

    public static String getLastMonth() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date); // 设置为当前时间
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1); // 设置为上一个月
        date = calendar.getTime();
        String accDate = format.format(date);
        return accDate;
    }

    public static String getDateByMonth(String dateStr, int month) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_STR);
        Date date = null;
        try {
            date = format.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date); // 设置为当前时间
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + (month));
        date = calendar.getTime();
        String accDate = format.format(date);
        return accDate;
    }

    public static void main(String[] args) {
        System.out.println(TimeUtil.getFormatTime(TimeUtil.DATE_FORMAT_STR).substring(8, 10));
    }


    public static Date timePastTenSecond(int value, Date time,int calendarUnit) {
        Calendar newTime = Calendar.getInstance();
        newTime.setTime(time);
        newTime.add(calendarUnit, value);
        return newTime.getTime();
    }

}
