package io.mo.replace;

import com.github.javafaker.Faker;

import java.util.Locale;

public class PreparedReplaceType {
    public static String DATETIME = "DATETIME";
    public static String DATE = "DATE";
    public static String UNIQUE = "UNIQUE";
    public static String PERSONNAME = "PERSONNAME";
    public static String IDCARDNO = "IDCARDNO";

    public static Faker myfaker = new Faker(Locale.CHINA);

    public Faker getMyfaker() {
        return myfaker;
    }

    public static void main(String[] args){
        for(int i = 0;i < 10; i++){
            System.out.println(PreparedReplaceType.myfaker.address().fullAddress());
        }

        for(int i = 0;i < 10; i++){
            System.out.println(PreparedReplaceType.myfaker.name().fullName());
        }

        for(int i = 0;i < 10; i++){
            System.out.println(PreparedReplaceType.myfaker.demographic().educationalAttainment());
        }

        for(int i = 0;i < 10; i++){
            System.out.println(PreparedReplaceType.myfaker.idNumber().invalid());
        }

    }

}
