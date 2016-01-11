package com.demo.dj.horizontaltimelineview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HorizontalTimeLineView timeLineView = (HorizontalTimeLineView) findViewById(R.id.time_line);
        final TextView textView = (TextView) findViewById(R.id.tv_selected_time);
        timeLineView.setDate(System.currentTimeMillis());
        timeLineView.setTimeSetCallback(new HorizontalTimeLineView.ITimeSetCallback() {
            @Override
            public void onTimeSet(long timeInMillis) {
                textView.setText("选择的时间是：" + HorizontalTimeLineView.formatTime(timeInMillis));
            }
        });
    }
}
