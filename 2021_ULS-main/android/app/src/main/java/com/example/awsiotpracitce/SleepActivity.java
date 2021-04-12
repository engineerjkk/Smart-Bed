package com.example.awsiotpracitce;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SleepActivity extends Activity {
    private static final String CHANNEL_ID = "1001";

    BarChart resultChart;
    Button sleepOn, sleepOff;
    TextView startTime, endTime;
    Button reset;

    NotificationManagerCompat notificationManager;

    JSONArray jsonArray, calJsonArray;
    JsonArrayRequest jsonArrayRequest, calJsonArrayRequest;
    ArrayList<Double> resultList = new ArrayList<>();
    ArrayList<Double> calResultList = new ArrayList<>();
    String start, end;
    long lStart, lEnd, setTime, currentTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sleep);

        sleepOn = findViewById(R.id.button_on);
        sleepOff = findViewById(R.id.button_off);
        resultChart = findViewById(R.id.chart);
        startTime = findViewById(R.id.start_text);
        endTime = findViewById(R.id.end_text);
        reset = findViewById(R.id.button1);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://jfh0slz0l0.execute-api.ap-northeast-2.amazonaws.com/stager/find";

        setTime = new Date().getTime();

        TimerTask check = new TimerTask() {
            @Override
            public void run() {
                readyToCalcuAccel(url + "operator=ALARM_DATA");
            }
        };

        Timer timer = new Timer();
        timer.schedule(check, 0, 1200000);

        sleepOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start = Long.toString(new Date().getTime());
                lStart = Long.parseLong(start);
                startTime.setText(new Date(lStart).toString());
            }
        });

        sleepOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                end = Long.toString(new Date().getTime());
                lEnd = Long.parseLong(end);
                if(lEnd - lStart >= 300000){
                    endTime.setText(new Date(lEnd).toString());
                    jsonArrayRequest = startAPIGateway(url, "?operator=SLEEP_DATA", start, end);
                    queue.start();
                    queue.add(jsonArrayRequest);
                }else{
                    builder.setMessage("측정을 5분 이상해야 합니다.");
                    builder.setTitle("경고");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queue.stop();
                startTime.setText("");
                endTime.setText("");
                resultList.clear();
                resultChart.clear();
            }
        });
    }

    private void readyToCalcuAccel(String mainUrl){
        String url = mainUrl + "operator=ALARM_DATA";
        calJsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                calJsonArray = response;
                try{
                    for(int i = 0; i < calJsonArray.length(); i++){
                        JSONObject jsonObject = calJsonArray.getJSONObject(i);
                        Double accel = jsonObject.getDouble("accel");

                        calResultList.add(accel);
                    }
                    returnCalResult();
                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error);
            }
        });
    }

    private void returnCalResult(){
        int loc = 0, defal = 0;
        if((loc % 4 == 0) &&  calResultList.size() > 4){
            loc = calResultList.size() - (calResultList.size() / 4);
        }
        for(int i = loc; i < calResultList.size(); i++){
            if(i == loc){
                defal = calResultList.indexOf(i);
            }else{
                int plus = defal + 5;
                int min = defal - 5;
                if(calResultList.indexOf(i) <= plus && calResultList.indexOf(i) <= min){
                    notificationManager.notify(88888, startNotification("ALARM").build());
                }
            }
        }
    }

    private JsonArrayRequest startAPIGateway(String mainUrl, String operator, String param1, String param2){
        String url = mainUrl + operator + "&start=" + param1 + "&end=" + param2;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                jsonArray = response;
                jsonToArrayData(jsonArray);
                subChartMain();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error);
            }
        });
        return jsonArrayRequest;
    }

    private void subChartMain(){
        makeIntChart(resultChart, "GREEN", "가속도", resultList);
    }

    private void jsonToArrayData(JSONArray jsonArray){
        try{
            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Double accel = jsonObject.getDouble("accel");

                resultList.add(accel);
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void makeIntChart(BarChart chart, String color, String chartTag, ArrayList<Double> arrayList){
        chart.setDoubleTapToZoomEnabled(false);
        chart.setPinchZoom(false);

        makeAxis(chart);

        List<BarEntry> entries = new ArrayList<BarEntry>();

        int count = 1;
        for(Double data : arrayList){
            float parseData = data.floatValue();
            entries.add(new BarEntry(count, parseData));
            count++;
        }

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
//        dataSets.add(makeDataSet(color, entries));
        dataSets.add(makeBarDataSet(color, chartTag, entries));

        BarData data = new BarData(dataSets);
        data.setValueTextSize(10f);

        chart.setData(data);
        chart.notifyDataSetChanged();
        chart.animateX(1000);
        chart.invalidate();
    }

    private void makeAxis(BarChart chart){
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(false);

        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
    }

    private BarDataSet makeBarDataSet(String color, String chartTest, List<BarEntry> entries){
        BarDataSet set = new BarDataSet(entries, chartTest);
        int setColor = 0;
        switch(color){
            case "RED":
                setColor = ContextCompat.getColor(this, R.color.red);
                break;
            case "GREEN":
                setColor = ContextCompat.getColor(this, R.color.green);
                break;
            case "BLUE":
                setColor = ContextCompat.getColor(this, R.color.blue);
                break;
            default:
                break;
        }
        set.setColor(setColor);
        set.setHighlightEnabled(false);
        return set;
    }

    private NotificationCompat.Builder startNotification(String mode) {
        Intent intent = new Intent(this, SensorActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,  PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setFullScreenIntent(pendingIntent, true);

        switch (mode){
            case "ALARM":
                builder.setContentTitle("욕창 주의!!").setContentText("몸을 한번 움직이여야 합니다.");
                break;
            default:
                break;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notification Channel";
            String description = "Testing notification for android oreo version.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        return builder;
    }
}
