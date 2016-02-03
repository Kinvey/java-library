package com.kinvey.android.cache;


import com.kinvey.java.KinveyException;
import com.kinvey.java.Query;
import com.kinvey.java.query.AbstractQuery;
import com.kinvey.java.query.QueryFilter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import io.realm.DynamicRealmObject;
import io.realm.RealmQuery;

/**
 * Created by Prots on 2/1/16.
 */
public abstract class QueryHelper {

    public static RealmQuery<DynamicRealmObject>  prepareRealmQuery(RealmQuery<DynamicRealmObject> realmQuery, Map<String, Object> queryMap){
        for (Map.Entry<String, Object> entity : queryMap.entrySet()){
            String field = entity.getKey();
            Object params = entity.getValue();


            if (field.equalsIgnoreCase("$or")){
                realmQuery.beginGroup();
                if (params.getClass().isArray()){
                    Map<String, Object>[] components = (Map<String, Object>[])params;
                    if (components != null && components.length > 0) {
                        realmQuery.beginGroup();
                        prepareRealmQuery(realmQuery, components[0]);
                        realmQuery.endGroup();
                        for (int i = 1 ; i < components.length; i++) {
                            realmQuery.or();
                            realmQuery.beginGroup();
                            prepareRealmQuery(realmQuery, components[i]);
                            realmQuery.endGroup();
                        }
                    }
                }
                realmQuery.endGroup();

            } else if (params instanceof Map){
                for (Map.Entry<String, Object> paramMap : ((Map<String, Object>) params).entrySet()){
                    String operation = paramMap.getKey();
                    if (operation.equalsIgnoreCase("$in")){
                        in(realmQuery, field, paramMap.getValue());
                    } else if (operation.equalsIgnoreCase("$nin")) {
                        realmQuery.beginGroup().not();
                        in(realmQuery, field, paramMap.getValue());
                        realmQuery.endGroup();
                    } else if (operation.equalsIgnoreCase("$gt")){
                        gt(realmQuery, field, paramMap.getValue());
                    } else if (operation.equalsIgnoreCase("$lt")){
                        lt(realmQuery, field, paramMap.getValue());
                    } else if (operation.equalsIgnoreCase("$gte")){
                        gte(realmQuery, field, paramMap.getValue());
                    } else if (operation.equalsIgnoreCase("$lte")){
                        lte(realmQuery, field, paramMap.getValue());
                    } else if (operation.equalsIgnoreCase("$ne")){
                        notEqualTo(realmQuery, field, paramMap.getValue());
                    } else {
                        throw new UnsupportedOperationException("this query is not supported by cache");
                    }

                }
            } else {
                equalTo(realmQuery, field, params);
            }
        }
        return realmQuery;
    }


    private static void in(RealmQuery query, String field, Object params){
        if (params.getClass().isArray()) {
            Object[] operatorParams = (Object[]) params;
            query.beginGroup();
            if (operatorParams != null && operatorParams.length > 0) {
                equalTo(query, field, operatorParams[0]);
                for (int i = 1; i < operatorParams.length; i++) {
                    query.or();
                    equalTo(query, field, operatorParams[i]);
                }
            }
            query.endGroup();
        }
    }

    private static void gt(RealmQuery query, String field, Object param){
        try {
            if (param instanceof Number) {

                Method m = query.getClass().getMethod("greaterThan", String.class,
                        (Class) param.getClass().getDeclaredField("TYPE").get(param));
                m.invoke(query, field, param);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static void gte(RealmQuery query, String field, Object param){
        try {
            if (param instanceof Number) {

                Method m = query.getClass().getMethod("greaterThanOrEqualTo", String.class,
                        (Class) param.getClass().getDeclaredField("TYPE").get(param));
                m.invoke(query, field, param);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static void lte(RealmQuery query, String field, Object param){
        try {
            if (param instanceof Number) {

                Method m = query.getClass().getMethod("lessThanOrEqualTo", String.class,
                        (Class) param.getClass().getDeclaredField("TYPE").get(param));
                m.invoke(query, field, param);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static void lt(RealmQuery query, String field, Object param){

        try {
            if (param instanceof Number){
                Method m = query.getClass().getMethod("lessThan", String.class,
                        (Class)param.getClass().getDeclaredField("TYPE").get(param));
                m.invoke(query, field, param);
            }

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static void equalTo(RealmQuery query, String field, Object param){

        try {
            Method m = query.getClass().getMethod("equalTo", String.class,
                    param.getClass());
            m.invoke(query, field, param);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private static void notEqualTo(RealmQuery query, String field, Object param){
        try {
            Method m = query.getClass().getMethod("notEqualTo", String.class, param.getClass());
            m.invoke(query, field, param);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

}
