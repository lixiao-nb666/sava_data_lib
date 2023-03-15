package com.newbee.save_data_;

import android.os.Bundle;
import android.util.Log;


import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyShare myShare=new MyShare(this);
        String s="fdsklfjlsf";
        myShare.putString("a",s);
        String s1=myShare.getString("a");
        String s2=myShare.getString("b");
        String s3=myShare.getString("c","d");
        Log.i("kankan","kankanstr1:"+s1);
        Log.i("kankan","kankanstr2:"+s2);
        Log.i("kankan","kankanstr3:"+s3);
    }

}