package com.launcher.multiapp;

import android.app.Application;
import java.lang.reflect.Field;

import com.jbselfcompany.tyr.TyrApplication;

public class TyrInitHelper {

    public static void init(Application app) {
        try {
            // Проверяем, инициализирован ли уже
            boolean initialized = false;
            try {
                TyrApplication.Companion.class.getDeclaredField("instance");
                // Получаем companion instance
                Field instanceField = TyrApplication.Companion.class.getDeclaredField("instance");
                instanceField.setAccessible(true);
                Object existing = instanceField.get(TyrApplication.Companion);
                if (existing != null) return;
            } catch (Exception e) {
                // Не инициализирован - продолжаем
            }

            TyrApplication tyrApp = new TyrApplication();
            
            // Устанавливаем instance
            Field instanceField = TyrApplication.Companion.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(TyrApplication.Companion, tyrApp);

            // Устанавливаем configRepository
            Field configField = TyrApplication.class.getDeclaredField("configRepository");
            configField.setAccessible(true);
            configField.set(tyrApp, new com.jbselfcompany.tyr.data.ConfigRepository(app));

            // Вызываем onCreate с контекстом
            tyrApp.attachBaseContext(app.getBaseContext());
            tyrApp.onCreate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
