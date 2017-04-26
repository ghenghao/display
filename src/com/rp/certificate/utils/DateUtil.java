package com.rp.certificate.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil{
	
	enum DatePatternEnum{
		AuthDatePattern("yyyy-MM-dd HH:mm:ss");
		
		private String pattern;
		
		DatePatternEnum(String pattern) {
			this.pattern = pattern;
		}
		
		public String getPattern(){
			return pattern;
		}
	}
	
	public static String convertDate2SignString(Date date){
        return getGMTTimeString(date).replace(" ", "T") + ".000";
	}
	
	public static String covertDate2TranscriptString(Date date){
		 return getGMTTimeString(date) + "Z";
	}
	
	public static String getGMTTimeString(Date date){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DatePatternEnum.AuthDatePattern.getPattern());
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return simpleDateFormat.format(date);
	}
	
	public static Date getGMTTime(Date date){
		String gmtStr = getGMTTimeString(date);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DatePatternEnum.AuthDatePattern.getPattern());
		Date gmt_date = null;
		try {
			gmt_date = simpleDateFormat.parse(gmtStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return gmt_date;
	}
	
	public static Date convertString2Date(String str){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DatePatternEnum.AuthDatePattern.getPattern());
		Date date = null;
		try {
			date = simpleDateFormat.parse(str);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}
	
	public static Date convertTransSearchTimeString2Date(String str){
		str = str.replace("T", " ").replace("+0000", "");
		return convertString2Date(str);
	}
	
	
	public static Date addDay(Date date, int dayAmount) {
		return addInteger(date, Calendar.DATE, dayAmount);
	}
	
	public static Date addHour(Date date, int hourAmount) {
		return addInteger(date, Calendar.HOUR, hourAmount);
	}
	
	private static Date addInteger(Date date, int dateType, int amount) { 
		Date newDate = null;  
        if (date != null) {  
        	Calendar calendar = Calendar.getInstance();  
            calendar.setTime(date);  
            calendar.add(dateType, amount);
            newDate = calendar.getTime();  
        }  
        return newDate;  
	}
	
	
	public static String convertDate2String(Date date){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DatePatternEnum.AuthDatePattern.getPattern());
        return simpleDateFormat.format(date);
	}
	
	public static String convertDate2String(Date date, String pattern){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(date);
	}
}