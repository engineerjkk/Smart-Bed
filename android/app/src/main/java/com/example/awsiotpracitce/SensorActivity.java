package com.example.awsiotpracitce;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Regions;
import com.dinuscxj.progressbar.CircleProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class SensorActivity extends Activity implements CircleProgressBar.ProgressFormatter {
    private static final String DEFAULT_PATTERN = "%d%%";
    private static final String CHANNEL_ID = "1001";

    /*
    *
    *        Need to setup your endpoint, Congito, region condition first!!!!
    *        Need to setup your endpoint, Congito, region condition first!!!!
    *        Need to setup your endpoint, Congito, region condition first!!!!
    *
    *
    * */
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "*********.iot.**********.amazonaws.com"; //Set your endpoint
    private static final String COGNITO_POOL_ID = "ap-northeast-2:ec35f67b-ac47-4579-a289-9dfc85a6203d"; //Set your Cognito ID
    private static final Regions MY_REGION = Regions.AP_NORTHEAST_2; //Set your region


    final String topic = "dt/stm32l475e/sensor-data/topic"; //Set your topic

    AWSIotMqttManager awsIot;
    String clientId;

    CognitoCachingCredentialsProvider credentialsProvider;

    NotificationManagerCompat notificationManager;

    TextView text_connectState, pressure_text, prox_text;

    CircleProgressBar bed_pressure, bed_prox,
            bed_accelX, bed_accelY, bed_accelZ,
            outside_temp, outside_humdi;
    Switch checkAdmin;

    int[] beforeAniValue = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_final);

        bed_pressure = findViewById(R.id.bed_pressure);
        bed_prox = findViewById(R.id.bed_prox);
        bed_accelX = findViewById(R.id.bed_accelX);
        bed_accelY = findViewById(R.id.bed_accelY);
        bed_accelZ = findViewById(R.id.bed_accelZ);
        outside_temp = findViewById(R.id.outside_temp);
        outside_humdi = findViewById(R.id.outside_humdi);

        checkAdmin = findViewById(R.id.set_admin);

        text_connectState = findViewById(R.id.text_connectState);
        pressure_text = findViewById(R.id.pressure_text);
        prox_text = findViewById(R.id.prox_text);

        notificationManager = NotificationManagerCompat.from(this);

        clientId = UUID.randomUUID().toString();

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );
        awsIot = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // main
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectToMqtt();
                        new Handler().postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        bringData();
                                    }
                                }, 3000
                        );
                    }
                });
            }
        }).start();
    }

    // Start to connect aws
    private void connectToMqtt(){
        try {
            awsIot.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status, final Throwable throwable) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                text_connectState.setText("Connecting...");
                            }else if (status == AWSIotMqttClientStatus.Connected) {
                                text_connectState.setText("Connected");
                            }else {
                                text_connectState.setText("Disconnected");
                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            System.out.println("Error!! " + e.getMessage());
        }
    }

    // If subscribe from topic detected, bring message
    private void bringData(){
        try {
            awsIot.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String message = new String(data, "UTF-8");
                                        adjustCircleProgress(message);
                                    } catch (UnsupportedEncodingException e) {
                                        System.out.println("Message Error!! " + e.getMessage());
                                    }
                                }
                            });
                        }
                    });
        } catch (Exception e) {
            System.out.println("Subscribe Error!! " + e.getMessage());
        }
    }

    // Using CirleProgressBar
    private void adjustCircleProgress(String str){
        int press, proxi, temp, humi, accel_x, accel_y, accel_z;
        try{
            JSONObject jsonObject = new JSONObject(str);

            press = jsonObject.getInt("Press");
            proxi = jsonObject.getInt("Proxi");
            temp = jsonObject.getInt("Temp");
            humi = jsonObject.getInt("Hum");
            accel_x = jsonObject.getInt("Accel_X");
            accel_y = jsonObject.getInt("Accel_Y");
            accel_z = jsonObject.getInt("Accel_Z");

            animationProgress(temp, 0, outside_temp);
            animationProgress(humi, 1, outside_humdi);;
            animationProgress(press, 2, bed_pressure);
            animationProgress(proxi, 3, bed_prox);
            animationProgress(accel_x, 7, bed_accelX);
            animationProgress(accel_y, 8, bed_accelY);
            animationProgress(accel_z, 9, bed_accelZ);

            if(press >= 1040 && press <= 1049){
                pressure_text.setText("침대에 있음");
            }else{
                pressure_text.setText("침대에 없음");
            }

            if(proxi > 0){
                prox_text.setText("감지!!");
            }else{
                prox_text.setText("감지 없음...");
            }

            if((press >= 1050) && checkAdmin.isChecked() == false){
                notificationManager.notify(11111, startNotification("PRESSURE_ALARM").build());
            }else if(press > 0 && humi >= 90){
                notificationManager.notify(22222, startNotification("HUMIDITY_ALARM").build());
            }else if(press > 0 && temp <= 20){
                notificationManager.notify(33333, startNotification("TEMPERATURE_LOW_ALARM").build());
            }else if(press > 0 && temp >= 35){
                notificationManager.notify(44444, startNotification("TEMPERATURE_HIGH_ALARM").build());
            }else if(press > 0 && proxi > 400){
                notificationManager.notify(99999, startNotification("EMERGENCY_ALARM").build());
            }

        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    // Notification
    private NotificationCompat.Builder startNotification(String mode) {
        Intent intent = new Intent(this, SensorActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,  PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setFullScreenIntent(pendingIntent, true);

        switch (mode){
            case "PRESSURE_ALARM":
                builder.setContentTitle("경보 울림!!").setContentText("환자가 침대에 없습니다. 확인이 필요합니다.");
                break;
            case "HUMIDITY_ALARM":
                builder.setContentTitle("경보 울림!!").setContentText("매트 습도가 높습니다. 기저귀나 시트 갈 필요가 있습니다.");
                break;
            case "TEMPERATURE_HIGH_ALARM":
                builder.setContentTitle("경보 울림!!").setContentText("통풍 시트를 작동합니다. 환자의 체온 유지를 위해 한번 뒤집어주세요.");
                break;
            case "TEMPERATURE_LOW_ALARM":
                builder.setContentTitle("경보 울림!!").setContentText("외부 온도가 추워, 전기장판을 작동하겠습니다.");
                break;
            case "EMERGENCY_ALARM":
                builder.setContentTitle("경보 울림!!").setContentText("환자가 긴급 요청을 했습니다. 확인이 필요합니다.");
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

    // CircleProgressBar Animation
    private void animationProgress(int maxValue, int arrayNum, CircleProgressBar bar) {
        ValueAnimator animator = ValueAnimator.ofInt(beforeAniValue[arrayNum], maxValue);
        beforeAniValue[arrayNum] = maxValue;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int progress = (int) animation.getAnimatedValue();
                bar.setProgress(progress);
            }
        });
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(1000);
        animator.start();
    }

    // CircleProgressBar Format
    @Override
    public CharSequence format(int progress, int max) {
        return String.format(DEFAULT_PATTERN, (int) ((float) progress / (float) max * 100));
    }
}


