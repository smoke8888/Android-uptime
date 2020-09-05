package com.smoke.uptime;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class StatActivity extends AppCompatActivity {

    String[] stat_string_array;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stat);

        stat_reader();
        show_table();
    }


    // наполнение таблицы статистикой ______________________________________________________________
    public void show_table() {
        int ROWS = stat_string_array.length / 2;
        int COLUMNS = 2;
        int k = 0;

        TableLayout tableLayout = findViewById(R.id.tbl_layout1);

        for (int i = 0; i <= ROWS; i++) {
            TableRow tableRow = new TableRow(this);
            for (int j = 0; j < COLUMNS; j++) {
                TextView txt_view = new TextView(this);
                // описываем шапку таблицы
                if (i == 0) {
                    switch (j) {
                        case 0: txt_view.setText(R.string.tbl_h1); txt_view.setTypeface(null, Typeface.BOLD); break;
                        case 1: txt_view.setText(R.string.tbl_h2); txt_view.setTypeface(null, Typeface.BOLD); break;
                    }
                }
                else {
                    if (j == 0) {txt_view.setText(stat_string_array[k]);}
                    //вывод времени в формате ЧЧ:ММ:СС
                    else {txt_view.setText(get_time_clock(Integer.valueOf(stat_string_array[k])));}
                    ++k;
                }
                tableRow.addView(txt_view, j);

            }

            tableLayout.addView(tableRow, i);
        }
    }


    // чтение файла статистики _____________________________________________________________________
    public void stat_reader () {
        File stat_file = new File(getFilesDir(),"uptime_stat.txt");
        try
        {
            char[] stat_char = new char[1000];
            FileReader stat_file_reader = new FileReader(stat_file);
            while((stat_file_reader.read(stat_char)) != -1) {}
            String stat_string = String.valueOf(stat_char);
            stat_file_reader.close();
            // разбиваем stat_string на массив {дата, секунды}
            stat_string_array = stat_string.split(",|;|\u0000");

        }
        catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    // метод преобразует секунды в формат "ЧЧ:ММ:СС" _______________________________________________
    public String get_time_clock(int sec){
        String time_clock = String.format("%2d:%02d:%02d", sec/3600, (sec%3600)/60, sec%60);
        return time_clock;
    }
}
