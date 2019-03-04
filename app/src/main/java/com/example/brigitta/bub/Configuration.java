package com.example.brigitta.bub;

/**
 * Created by Brigitta on 3/2/2019.
 */

public class Configuration {

    //public static final String URL_ADD="http://192.168.1.9/Android/pegawai/tambahPgw.php";
    public static final String URL_GET_STD = "http://192.168.0.6/thesis/getname.php?id=";
    //public static final String URL_UPDATE_EMP = "http://192.168.1.9/Android/pegawai/updatePgw.php";

    //Dibawah ini merupakan Kunci yang akan digunakan untuk mengirim permintaan ke Skrip PHP
    public static final String KEY_STD_ID = "id";
    public static final String KEY_STD_NAME = "name";
    public static final String KEY_STD_EMAIL = "email";
    //public static final String KEY_STD_POSISI = "desg"; //desg itu variabel untuk posisi

    //JSON Tags
    public static final String TAG_JSON_ARRAY = "result";
    public static final String TAG_ID = "id";
    public static final String TAG_NAME = "name";

    //STD = Student
    public static final String STD_ID = "std_id";
}
