package com.newbee.data_sava_lib.share;

import android.content.Context;
import android.content.SharedPreferences;


public class BaseShare {


    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    public BaseShare(Context context) {
        String packName =   getClass().getPackage().getName();
        String className = getClass().getSimpleName();
       // 创建共享存储
        sharedPreferences = context.getApplicationContext().getSharedPreferences("xiaoge_sp_xml_"+packName+className,
                Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }






    public void putString(String K, String V) {
        editor.putString(K, V);
        editor.commit();
    }

    // 取出共享文件的String
    public String getString(String k) {
        String data = sharedPreferences.getString(k, "");
        return data;
    }

    public String getString(String k,String defStr) {
        String data = sharedPreferences.getString(k, defStr);
        return data;
    }

    // 取出共享文件的String
    public void clear() {
        editor.clear();
        editor.commit();
    }



}
