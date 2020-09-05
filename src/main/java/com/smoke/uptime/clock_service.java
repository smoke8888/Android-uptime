package com.smoke.uptime;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import android.widget.Toast;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class clock_service extends Service {

    NotificationManager nm;
    Handler handler = new Handler();
    String stat_string = "";
    int stat_string_length = 0;
    String last_stat_date = "";

    @Override
    public void onCreate() {

        super.onCreate();
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }


    // инициализация службы ________________________________________________________________________
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences pref1 = getSharedPreferences("uptime", MODE_PRIVATE);
        SharedPreferences.Editor edit_pref1 = pref1.edit();
        Date current_date = new Date();
        SimpleDateFormat format_date = new SimpleDateFormat("dd.MM.yyyy");

        //вызываем начальное чтение статистики из файла в stat_string
        stat_reader();

        int index = stat_string.indexOf(","); // проверка на наличие хотя бы одной записи в файле статистики
        if (index > 0) { // запись есть
            last_stat_date = stat_string.substring(0, index); //вытаскаиваем последнюю дату записанную в файле статистики
        }
        else {//если статистики еще нет, то установка стартовой даты запуска сервиса по текущей дате
            last_stat_date = format_date.format(current_date);
        }
        sendNotif("Идет запись активности смартфона");
        handler.post(runTime); // запускаем Handler в отдельном потоке new Runnable для отсчета секунд активности экрана

        return START_STICKY;
    }

    // генерируем постоянно висящее уведомление в панели задач андроид - необходимо для того, чтобы сервис не убивался системой
    void sendNotif(String ContentText) {

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notif = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setTicker(getResources().getString(R.string.app_name))
                .setContentText(ContentText)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pIntent)
                .setOngoing(true)
//                .setDeleteIntent(contentPendingIntent)  // if needed
                .build();

        notif.flags = notif.flags | Notification.FLAG_NO_CLEAR;     // NO_CLEAR makes the notification stay when the user performs a "delete all" command
        startForeground(888, notif);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // _____________________________________________________________________________________________
    @Override
    public void onDestroy() {

        Toast.makeText(this, "Уничтожение Uptime", Toast.LENGTH_SHORT).show();
        handler.removeCallbacks(runTime);
        sendBroadcast(new Intent("service_restart"));
        super.onDestroy();
    }

    // таймер для подсчета времени активности экрана _______________________________________________
    Runnable runTime = new Runnable() {
        int counter = 0;
        public void run(){
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            SharedPreferences pref1 = getSharedPreferences("uptime", MODE_PRIVATE);
            SimpleDateFormat format_date = new SimpleDateFormat("dd.MM.yyyy");

            if (powerManager.isScreenOn()) {
                Date current_date = new Date();
                String current_date_simple = format_date.format(current_date);
                SharedPreferences.Editor edit_pref1 = pref1.edit();
                counter = pref1.getInt("counter", 0); // достаем из преференсов счетчик, чтобы увеличить его на 1 сек
                // если день изменился, то обнуляем счетчик, записываем в файл статистику за предыдущий день и обновляем last_stat_date
                if (!last_stat_date.equals(current_date_simple)){
                    stat_writer(last_stat_date, counter);
                    stat_reader();
                    counter = 0;
                    last_stat_date = current_date_simple;
                }
                else {stat_writer(current_date_simple, counter);}
                edit_pref1.putInt("counter", ++counter); // увеличиваем счетчик и записываем в преференсы
                edit_pref1.apply();
            }


            /*UsageStatsManager usm = (UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
            long end_time = System.currentTimeMillis();
            long begin_time = end_time - (1000);
            List<UsageStats> us = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin_time, end_time);
            if (us != null) {
                for(UsageStats us_one : us) {
                   Log.i("uptime", us_one.getPackageName() + " = " + us_one.getTotalTimeInForeground());
                }
            }*/
            handler.postDelayed(this, 1000);
        }
    };


    // запись статистики по текущей дате в файл ____________________________________________________
    public void stat_writer(String current_date_str, int counter) {
        int index = stat_string.indexOf(","); // проверка на наличие хотя бы одной записи в файле статистики
        if (index > 0) { // запись есть
            String save_file_date = stat_string.substring(0, index); //вытаскаиваем последнюю дату записанную в файле статистики
        //если последняя дата из файла статистики совпала с текущей датой то нужно ее просто обновить
            if (save_file_date.equals(current_date_str)) {
                stat_string = stat_string.substring(stat_string.indexOf(";") + 1); //удаляем из stat_string совпавшую последнюю дату
            }
        }
        //если число записанных дней в статистике > 30, то удаляем самую старую запись
        if (stat_string_length > 30){
            index = stat_string.lastIndexOf(";",stat_string.length() - 5) + 1;
            stat_string = stat_string.substring(0, index);
        }
        File stat_file = new File(this.getFilesDir(), "uptime_stat.txt");
        try {
            FileWriter stat_file_writer = new FileWriter(stat_file);
            stat_file_writer.write(current_date_str + "," + counter + ";" + stat_string);
            stat_file_writer.flush();
            stat_file_writer.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    // чтение статистики из файла __________________________________________________________________
    public void stat_reader(){
        File stat_file = new File(this.getFilesDir(),"uptime_stat.txt");
        try
        {
            char[] set_char = new char[1000];
            FileReader stat_file_reader = new FileReader(stat_file);
            while((stat_file_reader.read(set_char)) != -1) {}
            stat_string = String.valueOf(set_char);
            stat_file_reader.close();
            stat_string = stat_string.split("\u0000")[0];
            stat_string_length = stat_string.split(";").length;
        }
        catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}


