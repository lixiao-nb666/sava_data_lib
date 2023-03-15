package com.newbee.data_sava_lib.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


import com.newbee.data_sava_lib.bean.MyClassInfoBean;
import com.newbee.data_sava_lib.sqlite.config.BaseSqliteConfig;
import com.newbee.data_sava_lib.sqlite.event.SqlListenSubscriptionSubject;
import com.newbee.data_sava_lib.sqlite.util.SqliteUtil;
import com.newbee.data_sava_lib.util.ReflectUtil;
import com.newbee.gson_lib.gson.MyGson;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lixiaogege!
 * @description: one day day ,no zuo no die !
 * @date :2021/3/18 0018 9:57
 */
public class BaseSqlServer<T> {
    private MyClassInfoBean classInfoBean;
    private BaseSqlite spl;
    private String tablename;
    private Class defCls;
    private int v;
    private SQLiteDatabase sqLiteDatabase;
    private boolean needSendEvent;

    public BaseSqlServer(Context context,T t) {
        defCls = t.getClass();
        classInfoBean = new MyClassInfoBean(defCls);
        tablename = defCls.getSimpleName();
        v = 1;
        getSql(context);
    }

    public BaseSqlServer(Context context,T t, int v) {
        defCls = t.getClass();
        classInfoBean = new MyClassInfoBean(defCls);
        tablename = defCls.getSimpleName();
        this.v = v;
        getSql(context);
    }

    public void close(){
        if (sqLiteDatabase != null) {
            sqLiteDatabase.close();
            sqLiteDatabase = null;
        }
        if (spl != null) {
            spl.close();
            spl = null;
        }
    }


    private void getSql(Context context) {

        StringBuilder sb = new StringBuilder();
        sb.append("create table " + tablename + "("
                + "id integer primary key autoincrement, ");
        int i=0;
        for (String getStr : classInfoBean.getMethodMap.keySet()) {
            if (!SqliteUtil.isSqlDefId(getStr)) {
                sb.append(getStr + " text");
                if(i<classInfoBean.getMethodMap.keySet().size()-1){
                    sb.append(", ");
                }
            }
            i++;

        }
        sb.append(")");
        String createtableStr = sb.toString();
        String sqlName=usePackageGetSqlName(context);
        spl = new BaseSqlite(context,sqlName,tablename, createtableStr, v);
    }

    private String usePackageGetSqlName(Context context) {
        try {
            PackageManager packageManager = context.getApplicationContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getApplicationContext().getPackageName(), 0);
            return packageInfo.packageName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getClass().getPackage().getName()+"_"+getClass().getSimpleName();
    }



    public SQLiteDatabase getDB(){
        try {
            return spl.getReadableDatabase();
        }catch (Exception e){
            return null;
        }

    }

    public String getTablename(){
        return tablename;
    }

    public void setNeedSendEvent(boolean needSendEvent) {
        this.needSendEvent = needSendEvent;
    }


    public T add(T t) {
        if (null == t) {
            if (needSendEvent) {
                SqlListenSubscriptionSubject.getInstence().err("add: t is null");
                SqlListenSubscriptionSubject.getInstence().add(defCls, null, false);
            }
            return null;
        }

        try {
            sqLiteDatabase = spl.getReadableDatabase();
            ContentValues contentValues = new ContentValues();
            for (String getStr : classInfoBean.getMethodMap.keySet()) {
                if (!BaseSqliteConfig.def_id_str.equals(getStr)) {
                    Object object=classInfoBean.getMethodMap.get(getStr)
                            .invoke(t);
                    if(null==object){
                        contentValues.put(getStr, "");
                    }else {
                        if(ReflectUtil.isConvert(classInfoBean.fieldMap.get(getStr).getType().getName())){
                            contentValues.put(getStr, object.toString());
                        }else {
                            contentValues.put(getStr, MyGson.getInstance().toGsonStr(object));
                        }
                    }
                }
            }
            long id = sqLiteDatabase.insert(tablename, null, contentValues);
            if(id>0){
                classInfoBean.setMethodMap.get(BaseSqliteConfig.def_id_str).invoke(t, id);
                if (needSendEvent) {
                    SqlListenSubscriptionSubject.getInstence().add(t.getClass(), t, true);
                }
                return t;
            }else {
                SqlListenSubscriptionSubject.getInstence().err("add: t is err-- id <=0");
            }
        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("add: err-"+e.toString());
        }
        if (needSendEvent) {
            SqlListenSubscriptionSubject.getInstence().add(t.getClass(), null, false);
        }
        return null;
    }


    public boolean delect(String key, String vue) {
        boolean delect = false;
        try {
            sqLiteDatabase = spl.getReadableDatabase();
            String sql = "delete from " + tablename
                    + " where " + key + " = ?";
            Object shuzu[] = {vue};
            sqLiteDatabase.execSQL(sql, shuzu);
            if (sqLiteDatabase != null)
                sqLiteDatabase.close();
            delect = true;
        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("delect: err-"+e.toString());
        } finally {
            if (sqLiteDatabase != null)
                sqLiteDatabase.close();
        }
        if (needSendEvent) {
            SqlListenSubscriptionSubject.getInstence().remove(defCls, key,vue, delect);
        }
        return delect;
    }

    public boolean delectById(long id) {
        return delect("id", id + "");
    }

    public boolean delectAll() {
        boolean delect = false;
        try {
            sqLiteDatabase = spl.getReadableDatabase();
            String sql = "delete from " + tablename;
            Object shuzu[] = {};
            sqLiteDatabase.execSQL(sql, shuzu);

            delect = true;
        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("delectAll: err-"+e.toString());
        } finally {
            if (sqLiteDatabase != null) {
                sqLiteDatabase.close();
            }
        }
        if (needSendEvent) {
            SqlListenSubscriptionSubject.getInstence().remove(defCls, "",null, delect);
        }
        return delect;
    }

    public List<T> que(String key,String vue) {
        Cursor cursor = null;
        List<T> findList=null;
        try {
            String[] strS = {vue};
            sqLiteDatabase = spl.getReadableDatabase();

            cursor = sqLiteDatabase.query(tablename, classInfoBean.fNameS, key+"=?", strS, null, null, null);
            findList= useCursorGetList(cursor);
        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("que: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }

    public List<T> que(String key,String vue,int start,int needNumb) {
        Cursor cursor = null;
        List<T> findList=null;
        try {
            String[] strS = {vue};
            sqLiteDatabase = spl.getReadableDatabase();

            cursor = sqLiteDatabase.query(tablename, classInfoBean.fNameS, key+"=?", strS, null, null, null,start+","+needNumb);
            findList= useCursorGetList(cursor);
        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("que: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }

    public List<T> que(String[] keyS,String[] vueS) {
        Cursor cursor = null;
        List<T> findList=null;
        StringBuilder sb=new StringBuilder();
        try {
            String[] strS = vueS;
            sqLiteDatabase = spl.getReadableDatabase();
            int i=0;
            for(String str:keyS){
                if(i>0){
                    sb.append(" and ");
                }
                sb.append(str+"=?");

                i++;
            }
            cursor = sqLiteDatabase.query(tablename, classInfoBean.fNameS, sb.toString(), strS, null, null, null);
            findList= useCursorGetList(cursor);
        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("que: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }

    public List<T> que(String[] keyS,String[] vueS,int start,int needNumb) {
        Cursor cursor = null;
        List<T> findList=null;
        StringBuilder sb=new StringBuilder();
        try {
            String[] strS = vueS;
            sqLiteDatabase = spl.getReadableDatabase();
            int i=0;
            for(String str:keyS){
                if(i>0){
                    sb.append(" and ");
                }
                sb.append(str+"=?");
                i++;
            }
            cursor = sqLiteDatabase.query(tablename, classInfoBean.fNameS, sb.toString(), strS, null, null, null,start+","+needNumb);
            findList= useCursorGetList(cursor);
        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("que: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }

    public List<T> fuzzyQue(String key,String vue) {
        Cursor cursor = null;
        List<T> findList=null;
        try {
            String[] fuzzyS = {"%" + vue + "%"};
            sqLiteDatabase = spl.getReadableDatabase();
            cursor = sqLiteDatabase.query(tablename, classInfoBean.fNameS, key + " LIKE ? ",
                    fuzzyS, null, null, null);
            findList = useCursorGetList(cursor);

        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("fuzzyQue: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }

    public List<T> fuzzyQue(String key,String vue,int start,int needNumb) {
        Cursor cursor = null;
        List<T> findList=null;
        try {
            String[] fuzzyS = {"%" + vue + "%"};
            sqLiteDatabase = spl.getReadableDatabase();
            cursor = sqLiteDatabase.query(tablename, classInfoBean.fNameS, key + " LIKE ? ",
                    fuzzyS, null, null, null,start+","+needNumb);
            findList = useCursorGetList(cursor);

        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("fuzzyQue: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }

    public List<T> fuzzyQue(String[] keyS,String[] vueS) {
        Cursor cursor = null;
        List<T> findList=null;
        try {
            String[] fuzzyS=new String[vueS.length];
            String v=null;
            for(int i=0;i<vueS.length;i++){
               v =vueS[i];
                fuzzyS[i] = "%" + v + "%";
            }
            StringBuilder sb=new StringBuilder();
            int i=0;
            for(String str:keyS){
                if(i>0){
                    sb.append(" and ");
                }
                sb.append(str+" LIKE ? ");
                i++;
            }
            sqLiteDatabase = spl.getReadableDatabase();
            cursor = sqLiteDatabase.query(tablename, classInfoBean.fNameS, sb.toString(),
                    fuzzyS, null, null, null);
            findList = useCursorGetList(cursor);

        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("fuzzyQue: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }

    public List<T> fuzzyQue(String[] keyS,String[] vueS,int start,int needNumb) {
        Cursor cursor = null;
        List<T> findList=null;
        try {
            String[] fuzzyS=new String[vueS.length];
            String v=null;
            for(int i=0;i<vueS.length;i++){
                v =vueS[i];
                fuzzyS[i] = "%" + v + "%";
            }
            StringBuilder sb=new StringBuilder();
            int i=0;
            for(String str:keyS){
                if(i>0){
                    sb.append(" and ");
                }
                sb.append(str+" LIKE ? ");
                i++;
            }
            sqLiteDatabase = spl.getReadableDatabase();
            cursor = sqLiteDatabase.query(tablename, classInfoBean.fNameS, sb.toString(),
                    fuzzyS, null, null, null,start+","+needNumb);
            findList = useCursorGetList(cursor);

        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("fuzzyQue: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }

    public List<T> queAndfuzzyQue(String[] keyS,String[] vueS,String[] fuzzyKeyS,String[] fuzzyVueS) {
        Cursor cursor = null;
        List<T> findList=null;
        try {
            StringBuilder sb=new StringBuilder();
            int i=0;
            for(String key:keyS){
                if(i>0){
                    sb.append(" and ");
                }
                sb.append(key+"=?");
                i++;
            }
            for(String fuzzyKey:fuzzyKeyS){
                if(i>0){
                    sb.append(" and ");
                }
                sb.append(fuzzyKey+" LIKE ? ");
                i++;
            }
            String[] queAndFuzzyS=new String[vueS.length+fuzzyVueS.length];
            int j=0;
            for(String vue:vueS){
                queAndFuzzyS[j]=vue;
                j++;
            }
            for(String fuzzyVue:fuzzyVueS){
                queAndFuzzyS[j]=fuzzyVue;
                j++;
            }
            sqLiteDatabase = spl.getReadableDatabase();
            cursor = sqLiteDatabase.query(tablename, classInfoBean.fNameS, sb.toString(),
                    queAndFuzzyS, null, null, null);
            findList = useCursorGetList(cursor);
        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("fuzzyQue: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }

    public List<T> queAndfuzzyQue(String[] keyS,String[] vueS,String[] fuzzyKeyS,String[] fuzzyVueS,int start,int needNumb) {
        Cursor cursor = null;
        List<T> findList=null;
        try {
            StringBuilder sb=new StringBuilder();
            int i=0;
            for(String key:keyS){
                if(i>0){
                    sb.append(" and ");
                }
                sb.append(key+"=?");
                i++;
            }
            for(String fuzzyKey:fuzzyKeyS){
                if(i>0){
                    sb.append(" and ");
                }
                sb.append(fuzzyKey+" LIKE ? ");
                i++;
            }
            String[] queAndFuzzyS=new String[vueS.length+fuzzyVueS.length];
            int j=0;
            for(String vue:vueS){
                queAndFuzzyS[j]=vue;
                j++;
            }
            for(String fuzzyVue:fuzzyVueS){
                queAndFuzzyS[j]=fuzzyVue;
                j++;
            }
            sqLiteDatabase = spl.getReadableDatabase();
            cursor = sqLiteDatabase.query(tablename, classInfoBean.fNameS, sb.toString(),
                    queAndFuzzyS, null, null, null,start+","+needNumb);
            findList = useCursorGetList(cursor);

        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("fuzzyQue: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }


    public List<T> queAll() {
        Cursor cursor = null;
        List<T> findList=null;
        try {
            sqLiteDatabase = spl.getReadableDatabase();
            cursor = sqLiteDatabase.query(tablename,classInfoBean.fNameS, null, null, null, null, null);

            findList= useCursorGetList(cursor);
        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("queAll: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }

    public List<T> queAll(int start,int needNumb) {
        Cursor cursor = null;
        List<T> findList=null;
        try {
            sqLiteDatabase = spl.getReadableDatabase();

            cursor = sqLiteDatabase.query(tablename,classInfoBean.fNameS, null, null, null, null, null,start+","+needNumb);
            for(String ss:classInfoBean.fNameS){

            }
            findList= useCursorGetList(cursor);
        } catch (Exception e) {
            SqlListenSubscriptionSubject.getInstence().err("queAll: err-"+e.toString());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
            if (null != sqLiteDatabase)
                sqLiteDatabase.close();
        }
        return findList;
    }

    private List<T> useCursorGetList(Cursor cursor) {
        List<T> findList=new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                T t = (T) defCls.newInstance();

                for (int i = 0; i < classInfoBean.fNameS.length; i++) {
                    String column = classInfoBean.fNameS[i];
                    Method setMethod = classInfoBean.setMethodMap
                            .get(column);
                    String v=cursor.getString(i);

                    if(ReflectUtil.isConvert(classInfoBean.fieldMap.get(column).getType().getName())){
                        Object o = ReflectUtil.typeConvert(v,classInfoBean.fieldMap.get(column).getType().getName());
                        setMethod.invoke(t,o);
                    }else {
                        setMethod.invoke(t,   MyGson.getInstance().fromJson(v,classInfoBean.fieldMap.get(column).getType()));
                    }
                }
                findList.add(t);
            }
        }catch (Exception e){
            SqlListenSubscriptionSubject.getInstence().err("useCursorGetList: err-"+e.toString());
        }
        return findList;
    }





    public boolean update(T t) {
        if(null==t){
            return false;
        }
        boolean update = false;
        try {
            sqLiteDatabase = spl.getReadableDatabase();
            ContentValues contentValues = new ContentValues();
            for (String getStr : classInfoBean.getMethodMap.keySet()) {
                Object object=classInfoBean.getMethodMap.get(getStr)
                        .invoke(t);
                if(null==object){
                    contentValues.put(getStr, "");
                }else {
                    if(ReflectUtil.isConvert(classInfoBean.fieldMap.get(getStr).getType().getName())){
                        contentValues.put(getStr, object.toString());
                    }else {
                        contentValues.put(getStr, MyGson.getInstance().toGsonStr(object));
                    }
                }
            }
            Method idGetMethod = classInfoBean.getMethodMap
                    .get(BaseSqliteConfig.def_id_str);
            Object o=idGetMethod.invoke(t);
            String[] whereStrs = {o.toString()};
            sqLiteDatabase.update(tablename, contentValues, "id=?", whereStrs);
            update = true;
        } catch (Exception e) {

        } finally {
            if (null != sqLiteDatabase) {
                sqLiteDatabase.close();
            }

        }

        if (needSendEvent) {

            SqlListenSubscriptionSubject.getInstence().update(defCls, t, needSendEvent);
        }
        return update;
    }


}
