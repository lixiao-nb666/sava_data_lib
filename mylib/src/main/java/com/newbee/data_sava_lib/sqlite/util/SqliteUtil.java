package com.newbee.data_sava_lib.sqlite.util;

import android.text.TextUtils;

import com.newbee.data_sava_lib.sqlite.config.BaseSqliteConfig;


/**
 * @author lixiaogege!
 * @description: one day day ,no zuo no die !
 * @date :2020/9/21 0021 11:03
 */
public class SqliteUtil {

    public static boolean isSqlDefId(String checkStr){
        if(!TextUtils.isEmpty(checkStr)&&checkStr.equals(BaseSqliteConfig.def_id_str)){
            return true;
        }
        return false;
    }


}
