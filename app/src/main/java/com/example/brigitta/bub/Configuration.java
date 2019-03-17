package com.example.brigitta.bub;

/**
 * Created by Brigitta on 3/2/2019.
 */

public class Configuration {

    public static final String URL_GET_ANSWER_MATH = "http://192.168.0.8/thesis/getanswerkey_math.php";
    public static final String URL_GET_ANSWER_ENGLISH = "....";
    public static final String URL_GET_STD = "http://192.168.0.8/thesis/getname.php?id=";
    public static final String URL_GET_ALL_STD = "http://192.168.0.8/thesis/getalldata.php?id=";
    public static final String URL_UPDATE_SCORE = "http://192.168.0.8/thesis/updatescore.php";
    public static final String URL_UPDATE_SCORE_MATH = "http://192.168.0.8/thesis/updatescore_math.php";

    //Dibawah ini merupakan Kunci yang akan digunakan untuk mengirim permintaan ke Skrip PHP
    public static final String KEY_STD_ID = "id";
    public static final String KEY_STD_NAME = "name";
    public static final String KEY_STD_EMAIL = "email";
    public static final String KEY_STD_SCORE = "score";

    //JSON Tags
    public static final String TAG_JSON_ARRAY = "result";
    public static final String TAG_ID = "id";
    public static final String TAG_NAME = "name";
    public static final String TAG_EMAIL = "email";
    public static final String TAG_STATUS = "status";
    public static final String TAG_SCORE = "score";
    public static final String TAG_ANSWERKEY = "answer_key";

    //STD = Student
    public static final String STD_ID = "std_id";
}
