package com.example.awsiotpracitce;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChartActivity extends Activity {
    LineChart tempChart, humiChart, accelChart;
    ArrayList<Integer> tempList = new ArrayList<>();
    ArrayList<Integer> humiList = new ArrayList<>();
    ArrayList<Double> accelList = new ArrayList<>();
    JSONArray jsonArray;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chart);

        tempChart = findViewById(R.id.chart1);
        humiChart= findViewById(R.id.chart2);
        accelChart = findViewById(R.id.chart3);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://jfh0slz0l0.execute-api.ap-northeast-2.amazonaws.com/stager/find";

        JsonArrayRequest jsonArrayRequest = startAPIGateway(url, "?operator=RECENT_DATA");
        queue.start();
        queue.add(jsonArrayRequest);

    }

    private JsonArrayRequest startAPIGateway(String mainUrl, String param){
        String url = mainUrl + param;
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
                System.out.println("Error!!");
            }
        });
        return jsonArrayRequest;
    }

    private void subChartMain(){
        makeIntChart(tempChart, "RED", "온도", tempList);
        makeIntChart(humiChart, "BLUE", "습도", humiList);
        makeDoubleChart(accelChart,"GREEN", "가속도" , accelList);
    }

    private void jsonToArrayData(JSONArray jsonArray){
        try{
            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int temp = jsonObject.getInt("temperature");
                int humi = jsonObject.getInt("humidity");
                Double accel = jsonObject.getDouble("accel");

                tempList.add(temp);
                humiList.add(humi);
                accelList.add(accel);
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void makeIntChart(LineChart chart, String color, String chartTag, ArrayList<Integer> arrayList){
        chart.setDoubleTapToZoomEnabled(false);
        chart.setPinchZoom(false);

        makeAxis(chart);

        List<Entry> entries = new ArrayList<Entry>();

        int count = 1;
        for(Integer data : arrayList){
            float parseData = (float)data;
            entries.add(new Entry(count, parseData));
            count++;
        }

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(makeLineDataSet(color, chartTag, entries));

        LineData data = new LineData(dataSets);
        data.setValueTextSize(10f);

        chart.setData(data);
        chart.notifyDataSetChanged();
        chart.animateX(1000, Easing.EaseInOutCubic);
        chart.invalidate();
    }

    private void makeDoubleChart(LineChart chart, String color, String chartTag, ArrayList<Double> arrayList){
        List<Entry> entries = new ArrayList<Entry>();
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        chart.setDoubleTapToZoomEnabled(false);
        chart.setPinchZoom(false);

        makeAxis(chart);

        int count = 1;
        for(Double data : arrayList){
            float parseData = data.floatValue();
            entries.add(new Entry(count, parseData));
            count++;
        }
        dataSets.add(makeLineDataSet(color, chartTag, entries));

        LineData data = new LineData(dataSets);
        data.setValueTextSize(10f);

        chart.setData(data);
        chart.notifyDataSetChanged();
        chart.animateX(1000, Easing.EaseInOutCubic);
        chart.invalidate();
    }

    private void makeAxis(LineChart chart){
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(false);

        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
    }

    private LineDataSet makeLineDataSet(String color, String chartTest, List<Entry> entries){
        LineDataSet set = new LineDataSet(entries, chartTest);
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
        set.setCircleColor(ContextCompat.getColor(this, R.color.black));
        set.setDrawCircleHole(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setHighlightEnabled(false);
        return set;
    }
}
