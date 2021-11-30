package com.soapgu.hellobluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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

    ActivityResultLauncher<Intent> mDiscoverability = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                String message = result.getResultCode() > 0 ? String.format( "蓝牙被发现%s秒",result.getResultCode()) : "被取消";
                Toast.makeText( getApplicationContext() , message , Toast.LENGTH_SHORT).show();
            });

    ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if( result.getDevice().getName() != null )
                scanDeviceView.setText(scanOutput.append(result.getDevice().getName())
                        .append(  String.format( "(%s dBm)" , result.getRssi() ) )
                        .append("\r\n"));
        }

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
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        this.initializeControls();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

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
                    scanner.startScan(mLeScanCallback);
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