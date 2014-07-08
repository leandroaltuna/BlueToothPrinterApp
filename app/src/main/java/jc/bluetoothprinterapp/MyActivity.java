package jc.bluetoothprinterapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class MyActivity extends ActionBarActivity {

    TextView myLabel;
    EditText myTextbox;

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;

    OutputStream outputStream;
    InputStream inputStream;
    Thread workerThread;

    byte[] readBuffer;
    int readerBufferPosition;
    int counter;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        try
        {
            Button openButton = (Button) findViewById(R.id.open);
            Button sendButton = (Button) findViewById(R.id.send);
            Button closeButton = (Button) findViewById(R.id.close);

            myLabel = (TextView) findViewById(R.id.label);
            myTextbox = (EditText) findViewById(R.id.entry);

            openButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try
                    {
                        findBT();
                        openBT();
                    }
                    catch (IOException ex)
                    {

                    }
                }
            });

            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try
                    {
                        sendData();
                    }
                    catch (IOException ex)
                    {

                    }
                }
            });

            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try
                    {
                        closeBT();
                    }
                    catch (IOException ex)
                    {

                    }
                }
            });

        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void findBT()
    {
        try
        {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if ( bluetoothAdapter == null )
            {
                myLabel.setText("No bluetooth adapter available");
            }

            if ( !bluetoothAdapter.isEnabled() )
            {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult( enableBluetooth, 0 );
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if ( pairedDevices.size() > 0 )
            {
                for ( BluetoothDevice device: pairedDevices )
                {
                    if ( device.getName().equals("MP300") )
                    {
                        bluetoothDevice = device;
                        break;
                    }
                }
            }

            myLabel.setText("Bluetooth Device Found");
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    void openBT() throws IOException
    {
        try
        {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            beginListenForData();
            myLabel.setText("Bluetooth Opened");
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    void beginListenForData()
    {
        try
        {
            final Handler handler;

            handler = new Handler() {
                @Override
                public void close() {

                }

                @Override
                public void flush() {

                }

                @Override
                public void publish(LogRecord logRecord) {

                }
            };

            final byte delimiter = 10;

            stopWorker = false;
            readerBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopWorker)
                    {
                        try
                        {
                            int bytesAvailable = inputStream.available();

                            if (bytesAvailable > 0)
                            {
                                byte[] packetBytes = new byte[bytesAvailable];
                                inputStream.read(packetBytes);

                                for (int i = 0; i < bytesAvailable; i++)
                                {
                                    byte b = packetBytes[i];

                                    if (b == delimiter)
                                    {
                                        byte[] encondedBytes = new  byte[readerBufferPosition];
                                        System.arraycopy(readBuffer, 0, encondedBytes, 0, encondedBytes.length);
                                        final String data = new String(encondedBytes, "US-ASCII");
                                        readerBufferPosition = 0;

                                        handler.post(new Runnable(){
                                            @Override
                                            public void run() {
                                                myLabel.setText(data);
                                            }
                                        });
                                    }
                                    else
                                    {
                                        readBuffer[readerBufferPosition++] = b;
                                    }
                                }
                            }
                        }
                        catch (IOException e)
                        {
                            stopWorker = true;
                        }
                    }
                }
            });

            workerThread.start();
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    void sendData() throws IOException
    {
        try
        {
            String msg = myTextbox.getText().toString();
            msg += "\n";

            outputStream.write(msg.getBytes());

            myLabel.setText("Data Sent");
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    void closeBT() throws IOException
    {
        try
        {
            stopWorker = true;
            outputStream.close();
            inputStream.close();
            bluetoothSocket.close();
            myLabel.setText("Bluetooth Closed");
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
