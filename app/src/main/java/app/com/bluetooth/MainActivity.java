package app.com.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 456;

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothClient bluetoothClient = null;
    private List<BluetoothDevice> knownDevices = null;

    private TextView lblConnectedDevice;
    private Button connect;
    private Button btnForward;
    private Button btnStop;
    private Button btnBackward;
    private Button btnLeft;
    private Button btnRight;

    private ListView deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lblConnectedDevice = findViewById( R.id.lblConnectedDevice);
        connect = findViewById( R.id.connect );

        btnForward = findViewById( R.id.btnForward );
        btnForward.setOnClickListener( buttonsListener );
        btnStop = findViewById( R.id.btnStop );
        btnStop.setOnClickListener( buttonsListener );
        btnBackward = findViewById( R.id.btnBackward );
        btnBackward.setOnClickListener( buttonsListener );
        btnLeft = findViewById( R.id.btnLeft );
        btnLeft.setOnClickListener( buttonsListener );
        btnRight = findViewById( R.id.btnRight );
        btnRight.setOnClickListener( buttonsListener );

        deviceList = findViewById( R.id.deviceList);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( ! bluetoothAdapter.isEnabled() ) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

         knownDevices = new ArrayList<>( bluetoothAdapter.getBondedDevices() );
        ArrayAdapter<BluetoothDevice> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, knownDevices);
        deviceList.setAdapter(adapter);
        deviceList.setOnItemClickListener( deviceListListener );
        connect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(connect.getText().equals("CONNECT")){
                connectHC05();
                connect.setText("Disconnect");}else{
                    bluetoothClient.close();
                    connect.setText("CONNECT");
                    Toast.makeText( MainActivity.this, "HC-05 is disconnected", Toast.LENGTH_LONG ).show();
                }
            }
        });
    }

private void connectHC05(){
    BluetoothDevice device = bluetoothAdapter.getRemoteDevice("98:D3:36:00:BD:03");
    bluetoothClient = new BluetoothClient( device );

    lblConnectedDevice.setText( "Connected to " + device.getName() );
    lblConnectedDevice.setText( " " );
    bluetoothClient.start();
}
    private ListView.OnItemClickListener deviceListListener = new ListView.OnItemClickListener() {
        @Override public void onItemClick(AdapterView<?> adapter, View view, int arg2, long rowId) {
            BluetoothDevice device = knownDevices.get( (int) rowId );

            bluetoothClient = new BluetoothClient( device );

            lblConnectedDevice.setText( "Connected to " + device.getName() );
            lblConnectedDevice.setText( " " );
            bluetoothClient.start();

        }
    };

    private View.OnClickListener buttonsListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if ( bluetoothClient == null ) return;
            char code = 's';

            switch( view.getId() ) {
                case R.id.btnForward: code = 'a'; break;
                case R.id.btnBackward: code = 'r'; break;
                case R.id.btnStop: code = 's'; break;
                case R.id.btnLeft: code = 'g'; break;
                case R.id.btnRight: code = 'd'; break;

            }
          //  lblConnectedDevice.setText("");
            bluetoothClient.writeChar( code );

        }
    };


    private class BluetoothClient extends Thread {

        private BluetoothDevice bluetoothDevice;
        private BluetoothSocket bluetoothSocket;
        private InputStream inputStream;
        private OutputStream outputStream;
        private boolean isAlive = true;
        public  int i=0;

        public BluetoothClient( BluetoothDevice device ) {
            try {
                bluetoothDevice = device;
                bluetoothSocket = device.createRfcommSocketToServiceRecord( device.getUuids()[0].getUuid() );
                bluetoothSocket.connect();

                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();

                Toast.makeText( MainActivity.this, device.getName() + " connected", Toast.LENGTH_LONG ).show();
            } catch ( IOException exception ) {
                Log.e( "DEBUG", "Cannot establish connection", exception );
                Toast.makeText( MainActivity.this, device.getName() + " Cannot establish connection", Toast.LENGTH_LONG ).show();
            }
        }

        private void setText(final TextView text,final String value){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    text.setText(text.getText()+value);}

            });
        }
        private void clearText(final TextView text){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    text.setText("");}

            });
        }
        // Inutile dans le code actuel. Mais cela permettrait de recevoir
        // des informations du véhicule dans une future version.
        @Override
        public void run() {

         //   Toast.makeText( MainActivity.this, "no", Toast.LENGTH_LONG ).show();
            byte[] buffer = new byte[2048];  // buffer store for the stream
            int bytes;
            while (true) {
                try {


                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();


                    if(inputStream.available()>0){
                        bytes = inputStream.read(buffer);
                        String readMessage = new String(buffer, 0, bytes);
                        if(readMessage.equals("@")){
                            bytes=0;
                            clearText(lblConnectedDevice);
                        }else{
                       // lblConnectedDevice.setText(readMessage);
                        setText(lblConnectedDevice,readMessage);}}
                    else{
                        Thread.sleep(100);
                    }



                } catch (Exception exception) {
                    Log.e("DEBUG", "Cannot read data", exception);
                    //   close();

                }
            }
        }
        public void writeString(String text) {

            try {
                outputStream.write(text.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void writeChar(char code) {

            try {

                outputStream.write( code );

                outputStream.flush();

            } catch (IOException e) {
                Log.e( "DEBUG", "Cannot write message", e );
            }
        }

        // Termine la connexion en cours et tue le thread
        public void close() {
            try {
                bluetoothSocket.close();

                isAlive = false;
            } catch (IOException e) {
                Log.e( "DEBUG", "Cannot close socket", e );
            }
        }


    }

}
