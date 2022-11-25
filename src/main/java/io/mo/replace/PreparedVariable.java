package io.mo.replace;

import com.github.javafaker.Faker;
import io.mo.transaction.SQLScript;
import io.mo.util.IdCardNoGenerator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PreparedVariable {
    public static Faker myfaker = new Faker(Locale.CHINA);
    public static String datetime = "datetime";
    public static String date = "date";
    public static String unique = "unique";
    public static String fullname = "fullname";
    public static String idcardno = "idcardno";
    public static String cellphone = "cellphone";
    public static String phonenumber = "phonenumber";
    public static String address = "address";

    public static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static String getDatetime() {
        return TIME_FORMAT.format(new Date());
    }

    public static String getDate() {
        try {
            Date from = DATE_FORMAT.parse("1900-01-01");
            return DATE_FORMAT.format(myfaker.date().between(from,new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getUnique() {
        return String.valueOf(System.nanoTime());
    }

    public static void setUnique(String unique) {
        PreparedVariable.unique = unique;
    }

    public static String getFullname() {
        return myfaker.name().fullName();
    }

    public static String getIdcardno() {
        return IdCardNoGenerator.generate();
    }

    public static String getCellphone() {
        return myfaker.phoneNumber().cellPhone();
    }

    public static String getPhonenumber() {
        return  myfaker.phoneNumber().phoneNumber();
    }

    public static String getAddress() {
        return myfaker.address().fullAddress();
    }

    public static String replace(String str){
        if(str.indexOf("$datetime") != -1){
            str = str.replaceAll("\\$datetime",getDatetime());
        }

        if(str.indexOf("$date") != -1){
            str = str.replaceAll("\\$date",getDate());
        }

        if(str.indexOf("$unique") != -1){
            str = str.replaceAll("\\$unique",getUnique());
        }

        if(str.indexOf("$fullname") != -1){
            str = str.replaceAll("\\$fullname",getFullname());
        }

        if(str.indexOf("$idcardno") != -1){
            str = str.replaceAll("\\$idcardno",getIdcardno());
        }

        if(str.indexOf("$cellphone") != -1){
            str = str.replaceAll("\\$cellphone",getCellphone());
        }

        if(str.indexOf("$phonenumber") != -1){
            str = str.replaceAll("\\$phonenumber",getPhonenumber());
        }

        if(str.indexOf("$address") != -1){
            str = str.replaceAll("\\$address",getAddress());
        }

        return str;
    }

    public static void replace(SQLScript script){
        if(script.indexOf("$datetime") != -1){
            script.replaceAll("\\$datetime",getDatetime());
        }

        if(script.indexOf("$date") != -1){
            script.replaceAll("\\$date",getDate());
        }

        if(script.indexOf("$unique") != -1){
            script.replaceAll("\\$unique",getUnique());
        }

        if(script.indexOf("$fullname") != -1){
            script.replaceAll("\\$fullname",getFullname());
        }

        if(script.indexOf("$idcardno") != -1){
            script.replaceAll("\\$idcardno",getIdcardno());
        }

        if(script.indexOf("$cellphone") != -1){
            script.replaceAll("\\$cellphone",getCellphone());
        }

        if(script.indexOf("$phonenumber") != -1){
            script.replaceAll("\\$phonenumber",getPhonenumber());
        }

        if(script.indexOf("$address") != -1){
            script.replaceAll("\\$address",getAddress());
        }
    }

    public static void main(String[] args){
        String sql = "select JNSJ,TYXYM,GJ_ID,JNND,NJE  from dwd_zwy_zgjfqk_iwi where zjhm ='{zjhm} and aa='$fullname' and bb='$date' and cc='$idcardno' and dd = '$cellphone' adn ee='$address';";

        long beginTime = System.currentTimeMillis();
        System.out.println(PreparedVariable.replace(sql));
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - beginTime);
    }

}
