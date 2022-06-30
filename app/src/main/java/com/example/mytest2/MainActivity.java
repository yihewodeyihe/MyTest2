package com.example.mytest2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;

    private final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");//和客户端相同的UUID
    // NAME 没啥用
    private final String NAME = "ChinaNet-DGHJ";
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;

    //主界面文本框
    private static TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout linearLayout=new LinearLayout(this);
        linearLayout.setGravity(Gravity.TOP);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        textView =new TextView(this);
        textView.setTextSize(20);
        textView.setTextColor(Color.rgb(0x8E,0x74,0xCD));
        textView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));


        linearLayout.addView(textView);

        setContentView(linearLayout);



        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent,1);

        //定位权限
        requestBluetoothPermission();
        //设备可见性
        setDiscoverableTimeout(bluetoothAdapter,1);
        ensureDiscoverable();

        try{
            //创建一个正在监听的安全的带有服务记录的无线射频通信蓝牙端口
            serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME,MY_UUID);
            //在子线程 监听并接受socket请求（会阻塞所以放在子线程以免主线程卡死）
            new Thread(){
                @Override
                public void run(){
                    try{
                        socket=null;

                        while(true) {
                            //阻塞直到建立连接
                            socket = serverSocket.accept();
                            //在子线程执行 发送和接收（也会阻塞，因为输入输出流要随时等待数据的发送和接受）
                            Thread thread = new SocketThread(socket);
                            thread.start();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        if(socket!=null)
                        {
                            try {
                                socket.close();
                                socket=null;
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }.start();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void ensureDiscoverable(){     //设备可见
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,0);
            startActivity(discoverableIntent);
        }
    }

    public static void setDiscoverableTimeout(BluetoothAdapter adapter, int timeout) {
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode =BluetoothAdapter.class.getMethod("setScanMode", int.class,int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, timeout);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final int REQUEST_BLUETOOTH_PERMISSION=10;
    private void requestBluetoothPermission(){
        //判断系统版本
        if (Build.VERSION.SDK_INT >= 23) {
            //检测当前app是否拥有某个权限
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            //判断这个权限是否已经授权过
            if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED){
                //判断是否需要向⽤户解释，为什么要申请该权限
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION))
                    Toast.makeText(this,"Need bluetooth permission.",
                            Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this ,new String[]
                        {Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_BLUETOOTH_PERMISSION);
                return;
            }else{
            }
        } else {
        }
    }

    private class SocketThread extends Thread {
        private BluetoothSocket socket=null;
        private InputStream is;
        private OutputStream os;
        private boolean isOpen = false;

        public SocketThread(BluetoothSocket socket) {
            try {
                this.socket=socket;
                is = this.socket.getInputStream();
                os = this.socket.getOutputStream();
                isOpen = true;

            } catch (IOException e) {
                e.printStackTrace();
                release();
            }
        }

        @Override
        public void run() {

            String sendMessage;
            int readlen=0,a=1,b=2,c=3,d=4,i=5;
            byte[] bytes=new byte[1024];

            try{
                while(isOpen){

                    //发送时延
                    sleep(1000);


                    //发送的内容
                    sendMessage=(a++)+"-"+(b++)+"-"+(c++)+"-"+(d++)+"-"+(i++)+"";
                    //向本机主界面发送以便显示发送内容
                    MainActivity.handler.obtainMessage(1,0,0,sendMessage).sendToTarget();

                    //输出流发送
                    os.write(sendMessage.getBytes(StandardCharsets.UTF_8));
                    os.flush();


                    }
                } catch (IOException ex) {
                release();
                ex.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
                release();
            }
        }

        public void release(){
            try{
                isOpen=false;

                if(os!=null){
                    try{
                        os.close();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    os=null;
                }
                if(is!=null){
                    try{
                        is.close();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    is=null;
                }
                if(socket!=null){
                    try{
                        socket.close();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    socket=null;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    //handler用于实现 子线程向主线程（主界面）递交数据
    // 为避免在主线程执行耗时操作造成主线程卡死，很多参数变化交给子线程来运算，然后通过handler通知主线程修改UI界面
    public static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    textView.setText("sendMessage:"+"\n"+msg.obj.toString());
                    break;
            }
        }
    };
}

 /*
                    //如果要发送系统时间

                    //定义变量
                    Calendar cal;
                    String year,month,day,hour,minute,second,my_time_1,my_time_2;


                    //使用变量
                    cal = Calendar.getInstance();
                    cal.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
                    year = String.valueOf(cal.get(Calendar.YEAR));
                    month = String.valueOf(cal.get(Calendar.MONTH)+1);
                    day = String.valueOf(cal.get(Calendar.DATE));
                    if (cal.get(Calendar.AM_PM) == 0)
                        hour = String.valueOf(cal.get(Calendar.HOUR));
                    else
                        hour = String.valueOf(cal.get(Calendar.HOUR)+12);
                    minute = String.valueOf(cal.get(Calendar.MINUTE));
                    second = String.valueOf(cal.get(Calendar.SECOND));
                    my_time_1 = year + "-" + month + "-" + day;
                    my_time_2 = hour + "-" + minute + "-" + second;
                    sendMessage=my_time_1+"\n"+my_time_2;//(a++)+"-"+(b++)+"-"+(c++)+"-"+(d++)+"-"+(i++)+"";
                    */