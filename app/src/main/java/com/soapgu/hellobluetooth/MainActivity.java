package com.soapgu.hellobluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;

import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView existDeviceView;
    private TextView scanDeviceView;
    private Button searchButton;
    private StringBuilder scanOutput;
    private boolean isScan;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothLeScanner scanner;
    ScanSettings settings;
    BeaconParser beaconParser;


    ActivityResultLauncher<Intent> mDiscoverability = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                String message = result.getResultCode() > 0 ? String.format( "蓝牙被发现%s秒",result.getResultCode()) : "被取消";
                Toast.makeText( getApplicationContext() , message , Toast.LENGTH_SHORT).show();
            });

    ActivityResultLauncher<Intent> mBluetoothEnable = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                boolean success = result.getResultCode() == RESULT_OK;
                String message = success ? "蓝牙开始成功": "蓝牙开启失败";
                Toast.makeText( getApplicationContext() , message , Toast.LENGTH_SHORT).show();
                if( success ){
                    initBlueTooth();
                }
            });

    ActivityResultLauncher<String> mRequestPermission = registerForActivityResult( new ActivityResultContracts.RequestPermission() ,
            result -> {
                Toast.makeText( getApplicationContext() , result ? "同意,请重试":"拒绝", Toast.LENGTH_SHORT).show();
                if( result ){
                    checkAndInitBlueTooth();
                }
            });

    ScanCallback mLeScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            Beacon beacon = beaconParser.fromScanData( result.getScanRecord().getBytes() , result.getRssi() , result.getDevice() , 0 );

            if( beacon != null ) {
                scanDeviceView.setText(scanOutput.append(result.getDevice().getName())
                        .append(String.format("(%s dBm)", result.getRssi()))
                         .append( String.format( "( uuid:%s major:%s minor:%s  ) ", beacon.getId1() , beacon.getId2(),beacon.getId3()) )
                        .append("\r\n"));
            }

        }

        @SuppressLint("MissingPermission")
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            scanOutput = new StringBuilder();
            for( ScanResult scanResult : results ) {
                scanDeviceView.setText(scanOutput.append(scanResult.getDevice().getName())
                                                .append( scanResult.getRssi() )
                                                .append("\r\n"));
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        this.checkPermission();
    }

    private void checkPermission(){
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? Manifest.permission.ACCESS_FINE_LOCATION : Manifest.permission.ACCESS_COARSE_LOCATION;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            checkAndInitBlueTooth();
        }
        else {
            mRequestPermission.launch( permission );
        }
    }

    private void checkAndInitBlueTooth(){
        if( bluetoothAdapter != null && bluetoothAdapter.isEnabled() ) {
            initBlueTooth();
        }
        else{
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mBluetoothEnable.launch( enableBtIntent );
        }
    }

    private void initBlueTooth(){
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        this.settings = builder.build();
        this.beaconParser = new BeaconParser()
                .setBeaconLayout(BeaconLayouts.IBEACON);
        this.initializeControls();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @SuppressLint("MissingPermission")
    private void initializeControls(){
        this.existDeviceView = this.findViewById(R.id.existDeviceView);
        this.scanDeviceView = this.findViewById(R.id.scanDeviceView);

        searchButton = this.findViewById(R.id.searchButton);
        searchButton.setOnClickListener( v ->{
            if( bluetoothAdapter.isEnabled() ){
                 Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if( !pairedDevices.isEmpty() ) {
                    StringBuilder existOutput = new StringBuilder();
                    for (BluetoothDevice device : pairedDevices) {
                        existOutput.append(device.getName()).append("\r\n");
                    }
                    this.existDeviceView.setText(existOutput);
                }

                //bluetoothAdapter.startDiscovery();
                if( !isScan ) {
                    scanOutput = new StringBuilder();
                    scanner.startScan( null , this.settings , mLeScanCallback);
                }
                else {
                    scanner.stopScan(mLeScanCallback);
                }
                isScan = !isScan;
                this.searchButton.setText( isScan ? "stop":"scan" );
            }
        } );


        this.findViewById(R.id.discoverabilityButton).setOnClickListener( v -> {
            Intent discoverableIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 240);
            //startActivity(discoverableIntent);
            mDiscoverability.launch( discoverableIntent );
        } );
    }

    @SuppressLint("MissingPermission")
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();

                //String deviceHardwareAddress = device.getAddress(); // MAC address
                if( deviceName != null && !deviceName.equals("null"))
                scanDeviceView.setText( scanOutput.append(deviceName).append("\r\n") );
            }
        }
    };

}