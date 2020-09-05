package com.smoke.uptime;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;


import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    TextView txt_view;
    TextView txt_view4;
    Handler handler = new Handler();
    String stat_string;
    String[] stat_string_array;
    ArrayList<BarEntry> value_chart = new ArrayList<>();
    ArrayList<String> labels_chart = new ArrayList<>();
    BarChart chart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt_view = findViewById(R.id.textView);
        txt_view4 = findViewById(R.id.textView4);
        chart = findViewById(R.id.chart);
        Toolbar toolbar1 = findViewById(R.id.toolbar1);
        setSupportActionBar(toolbar1);

        // проверка разрешений
        AppOpsManager appOpsManager = (AppOpsManager)getSystemService(APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), this.getPackageName());
        Log.i("uptime", "mode = " + Integer.toString(mode));
        // если нет, то направляем в настройки для установки разрешений
        if (mode != AppOpsManager.MODE_ALLOWED) {
            Intent stat_access = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            stat_access.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(stat_access);
        }


        // проверяем запущена ли служба
        boolean tStartService = true;
        String service_name = clock_service.class.getName();
        ActivityManager am = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> rs = am.getRunningServices(50);
        for (int i=0; i<rs.size(); i++) {
            ActivityManager.RunningServiceInfo rsi = rs.get(i);
            if(service_name.equalsIgnoreCase(rsi.service.getClassName())){
                tStartService = false;
                break;
            }
        }
        //если служба не запущена, то запускаем
        if(tStartService){
            startService(new Intent(MainActivity.this, clock_service.class));
        }

        //инициализация строковых массивов stat_string перед отображением чарта
        stat_reader();
        // первый запуск чарта. true - значит запуск первый - показываем анимацию
        if (stat_string != null) { // если статиситки еще нет, то ничего не выводим
            get_chart(true);
        }

        handler.post(time_upd);

    }


    Runnable time_upd = new Runnable() {
        public void run(){
            //обновлеие строковых массивов stat_string и stat_string_array
            stat_reader();
            if (stat_string != null) { // если статиситки еще нет, то ничего не выводим
                Date current_date = new Date();
                SimpleDateFormat format_date = new SimpleDateFormat("dd.MM");
                int counter = Integer.parseInt(stat_string_array[1]);
                txt_view.setText(format_date.format(current_date) + " " + get_time_clock(counter)); // выводим время в формате "ЧЧ:ММ:СС"

                //txt_view4.setText("Текущая дата: " + format_date.format(current_date));


                // отображение чарта. подтягиваем данные из метода get_chart
                get_chart(false);
            }
            handler.postDelayed(time_upd, 1000);
        }
    };

    // чтение файла статистики _____________________________________________________________________
    public void stat_reader () {
        File stat_file = new File(getFilesDir(),"uptime_stat.txt");
        try
        {
            char[] stat_char = new char[1000];
            FileReader stat_file_reader = new FileReader(stat_file);
            while((stat_file_reader.read(stat_char)) != -1) {}
            stat_string = String.valueOf(stat_char);
            stat_file_reader.close();
            // разбиваем stat_string на массив {дата, секунды}
            stat_string_array = stat_string.split(",|;|\u0000");

        }
        catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    // формирование данных для чарта _______________________________________________________________
    public void get_chart(boolean create_chart) {
        value_chart.clear();
        labels_chart.clear();
        int chart_day; // число дней за которые показываем данные диаграммы
        if (stat_string_array.length > 7*2) {chart_day = 7*2;}
        else {chart_day = stat_string_array.length;}
        // проходим каждый 2-й элекмент массива stat_string_array, там где секунды
        for (int i = 0; i < chart_day; i = i + 2) {
            int sec = Integer.parseInt(stat_string_array[i + 1]);
            // добавляем в массив данных чарта данные вида (порядковый номер, секунды)
            value_chart.add(new BarEntry(i / 2, sec, get_time_clock(sec)));
            labels_chart.add(stat_string_array[i]);
        }

        BarDataSet set = new BarDataSet(value_chart, "Диаграмма активности");
        set.setValueTextColor(getResources().getColor(R.color.secondary_text_default));
        BarData data = new BarData(set);

        //настройка отображения оси Х
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //подпись значений по оси Х - снизу
        xAxis.setTextColor(getResources().getColor(R.color.secondary_text_default)); // цвет подписи
        xAxis.setDrawGridLines(false); // убираем линии сетки по вертикали
        xAxis.setLabelRotationAngle(30); //поворачиваем подписи по оси Х на 90 градусов
        // выводим над каждым Bar значения в формате "ЧЧ:ММ:СС"
        set.setValueFormatter(new AboveBarLabelFormatter());
        // выводим подписи по оси Х в виде "ДД.ММ.ГГГГ"
        chart.getXAxis().setValueFormatter(new XAxisLabelsFormatter());
        // настройка отображения оси У
        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(0f); // ось У начинаем с 0
        left.setDrawLabels(false); // убираем подписи по оси У
        left.setDrawAxisLine(false); // убираем линию оси У
        left.setDrawGridLines(false); // убираем линии сетки по горизонтали;
        chart.getAxisRight().setEnabled(false); // убираем ось У справа
        if (create_chart) {chart.animateY(2000);}

        chart.notifyDataSetChanged(); //уведомление Чарта об изменении данных
        chart.invalidate(); // рефреш чарта
        chart.setData(data);
    }

    // класс формирует подписи по оси Х в виде "ДД.ММ.ГГГГ" ________________________________________
    public class XAxisLabelsFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return labels_chart.get((int) value);
        }
    }
    // класс формирует над каждым Bar значения в формате "ЧЧ:ММ:СС"_________________________________
    public class AboveBarLabelFormatter implements IValueFormatter {
        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return (String) entry.getData();
        }
    }

    // метод преобразует секунды в формат "ЧЧ:ММ:СС" _______________________________________________
    public String get_time_clock(int sec){
        String time_clock = String.format("%2d:%02d:%02d", sec/3600, (sec%3600)/60, sec%60);
            return time_clock;
    }

    // отображаем меню на тулбаре __________________________________________________________________
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // обрабатываем выбор item в меню ______________________________________________________________
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_stat: {
                Intent intent = new Intent(MainActivity.this, StatActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.app_list: {
                Intent intent = new Intent(MainActivity.this, AppSelectActivity.class);
                startActivity(intent);
            }
            break;

            case R.id.item_clear: { // удаляем файл статистики и сбрасываем счетчик counter
                txt_view.setText("Обнуление статистики");
                SharedPreferences pref1 = getSharedPreferences("uptime", MODE_PRIVATE);
                SharedPreferences.Editor edit_pref1 = pref1.edit();
                edit_pref1.putInt("counter", 0); // счетчик = 0
                edit_pref1.apply();
                File stat_file = new File(getFilesDir(), "uptime_stat.txt");
                if (stat_file.exists() && stat_file.delete()) {
                    Toast.makeText(this, "Файл статистики удален", Toast.LENGTH_SHORT).show();
                }
                chart.clear();
                break;
            }
        }
        return true;
    }
}
