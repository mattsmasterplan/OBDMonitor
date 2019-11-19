package hello.matt.bury.obdmonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import hello.matt.bury.commands.SpeedCommand;
import hello.matt.bury.commands.control.TroubleCodesCommand;
import hello.matt.bury.commands.control.VinCommand;
import hello.matt.bury.commands.engine.RPMCommand;
import hello.matt.bury.commands.protocol.CloseCommand;
import hello.matt.bury.commands.protocol.EchoOffCommand;
import hello.matt.bury.commands.protocol.LineFeedOffCommand;
import hello.matt.bury.commands.protocol.ResetTroubleCodesCommand;
import hello.matt.bury.commands.protocol.SelectProtocolCommand;
import hello.matt.bury.commands.protocol.TimeoutCommand;
import hello.matt.bury.enums.ObdProtocols;
import hello.matt.bury.exceptions.NoDataException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static final int REQUEST_ENABLE_BT = 1;
    BluetoothSocket socket;
    BluetoothAdapter btAdapter;


    Button RPMFragToggle, SpeedFragToggle;
    String deviceAddress = "", RPM = "0", SPEED = "0", VIN = "", result = "";
    Boolean continueRPM = true, continueSpeed = true, bluetoothConnected = false;
    Handler RPMHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Bundle b = msg.getData();
            RPM = b.getString("RPM");
           // rpmView.setText(RPM);
        }
    };

    Handler SpeedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Bundle b = msg.getData();
            SPEED = b.getString("Speed");
          //  speedView.setText(SPEED);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_view);




    }

    protected void baseViewChange() {

        setContentView(R.layout.fragment_view_base);




    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "BlueTooth Enabled", Toast.LENGTH_LONG).show();
                displayBTConnectDialog();
            }
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(MainActivity.this, "BlueTooth Error", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {


            case R.id.connectButton:
                checkBT();

               // connectionView = (TextView) findViewById(R.id.connectionView);
               // connectionView.setText(R.string.noConnection);

              //  rpmView = (TextView) findViewById(R.id.rpmView);
              //  speedView = (TextView) findViewById(R.id.speedView);
              //  vinView = (TextView) findViewById(R.id.vinView);
              //  codeView = (TextView) findViewById(R.id.codeView);


              //  connectButton = (Button) findViewById(R.id.connectButton);
               // connectButton.setOnClickListener(this);

               // DisplayRPMButton = (Button) findViewById(R.id.DisplayRPMButton);
               // DisplayRPMButton.setTag(1);
               // DisplayRPMButton.setOnClickListener(this);

               // DisplaySpeedButton = (Button) findViewById(R.id.DisplaySpeedButton);
               // DisplaySpeedButton.setTag(1);
               // DisplaySpeedButton.setOnClickListener(this);

               // DetectVINButton = (Button) findViewById(R.id.DetectVINButton);
               // DetectVINButton.setOnClickListener(this);

              // CheckDTCButton = (Button) findViewById(R.id.CheckDTCButton);
               // CheckDTCButton.setOnClickListener(this);
                break;

           /* case R.id.RPMFragToggle:

                if (!bluetoothConnected) {
                    Toast.makeText(MainActivity.this, "No BlueTooth Connection", Toast.LENGTH_SHORT).show();
                    break;
                }

                int RPMstatus = (Integer) v.getTag();
                if (RPMstatus == 1) {
                   // DisplayRPMButton.setText(R.string.pause_rpm);
                  //  DisplayRPMButton.setTag(0);
                    continueRPM = true;
                    displayRPM();
                    break;
                } else if (RPMstatus == 0) {
                   // DisplayRPMButton.setText(R.string.resume_rpm);
                  //  DisplayRPMButton.setTag(1);
                    continueRPM = false;
                    break;
                }
*/
            case R.id.DisplaySpeedButton:

                if (!bluetoothConnected) {
                    Toast.makeText(MainActivity.this, "No BlueTooth Connection", Toast.LENGTH_SHORT).show();
                    break;
                }

                int speedstatus = (Integer) v.getTag();
                if (speedstatus == 1) {
                  //  DisplaySpeedButton.setText(R.string.pause_speed);
                   // DisplaySpeedButton.setTag(0);
                    continueSpeed = true;
                    displaySpeed();
                    break;
                } else if (speedstatus == 0) {
                 //   DisplaySpeedButton.setText(R.string.resume_speed);
                 //   DisplaySpeedButton.setTag(1);
                    continueSpeed = false;
                    break;
                }
            case R.id.DetectVINButton:

                if (!bluetoothConnected) {
                    Toast.makeText(MainActivity.this, "No BlueTooth Connection", Toast.LENGTH_SHORT).show();
                    break;
                }
                displayVIN();
                break;
            case R.id.CheckDTCButton:
                if (!bluetoothConnected) {
                    Toast.makeText(MainActivity.this, "No BlueTooth Connection", Toast.LENGTH_SHORT).show();
                    break;
                }
                displayCodes();
                break;
            case R.id.ClearDTCButton:
                clearDTC();
                break;
            default:
                break;
        }
    }

    public void checkBT() {

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        //If device does not support Bluetooth
       /* if (btAdapter == null) {
            //TODO handle error

        }*/

        //Enable BT if not enabled
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            displayBTConnectDialog();
        }
    }

    public void displayBTConnectDialog() {

        final ArrayList deviceStrs = new ArrayList();
        //final ArrayList devices = new ArrayList();

        Set pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (Object device : pairedDevices) {
                BluetoothDevice device1 = (BluetoothDevice) device;
                Log.d("gping2", "BT: " + device1.getName() + " - " + device1.getAddress());
                deviceStrs.add(device1.getName() + "\n" + device1.getAddress());
                // devices.add(device1.getAddress());
            }
        }

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice,
                deviceStrs.toArray(new String[deviceStrs.size()]));

        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                deviceAddress = (String) deviceStrs.get(position);
                Log.d("gping2", "Picked: " + deviceAddress);
                int index = deviceAddress.indexOf(':');
                deviceAddress = deviceAddress.substring(index - 2);
                connect_bt();
            }
        });
        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();
    }

    public void connect_bt() {

        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            socket.connect();
            Log.d("gping2", "Connected: " + uuid);

            //connectionView = (TextView) findViewById(R.id.connectionView);
            bluetoothConnected = true;
            Toast.makeText(MainActivity.this, "ODB-II BlueTooth Connection Established", Toast.LENGTH_LONG).show();
           // connectionView.setText(R.string.connected);
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Failed to connect BlueTooth device", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            Log.e("gping2", "BT connect error");
        }

        try {
            new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            Thread.sleep(200);
            new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            Thread.sleep(200);
            new TimeoutCommand(25).run(socket.getInputStream(), socket.getOutputStream());
            Thread.sleep(200);
            new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
            Thread.sleep(200);

            baseViewChange();



        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void displayVIN() {

        VinCommand VinCommand = new VinCommand();

        try {

            VinCommand.run(socket.getInputStream(), socket.getOutputStream());


            VIN = VinCommand.getFormattedResult();
            Log.d("gping2", "VIN: " + VIN);

           // vinView.setText(VIN);

            //NullPointerException = no BlueTooth connection
        } catch (IOException | InterruptedException | NoDataException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void displayRPM() {

        Runnable rpmmonitor = new Runnable() {
            @Override
            public void run() {

                while (!Thread.currentThread().isInterrupted() && continueRPM) {
                    RPMCommand engineRpmCommand = new RPMCommand();

                    try {
                        engineRpmCommand.run(socket.getInputStream(), socket.getOutputStream());
                    } catch (IOException | InterruptedException | NullPointerException e) {
                        e.printStackTrace();

                    }


                    RPM = engineRpmCommand.getFormattedResult();
                    Log.d("gping2", "RPM: " + RPM);

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Message msg = new Message();
                    Bundle b = new Bundle();
                    b.putString("RPM", RPM);
                    msg.setData(b);
                    RPMHandler.sendMessage(msg);


                }
            }
        };
        Thread t = new Thread(rpmmonitor);
        t.start();

    }

    public void displaySpeed() {

        Runnable speedmonitor = new Runnable() {

            @Override
            public void run() {

                while (!Thread.currentThread().isInterrupted() && continueSpeed) {
                    SpeedCommand engineSpeedCommand = new SpeedCommand();

                    try {
                        engineSpeedCommand.run(socket.getInputStream(), socket.getOutputStream());


                        SPEED = engineSpeedCommand.getFormattedResult();
                        Log.d("gping2", "Speed: " + SPEED);

                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Message msg = new Message();
                        Bundle b = new Bundle();

                        b.putString("Speed", SPEED);
                        msg.setData(b);
                        SpeedHandler.sendMessage(msg);

                    } catch (IOException | InterruptedException | NullPointerException e) {
                        e.printStackTrace();
                        continueSpeed = false;
                    }

                }
            }
        };
        Thread t = new Thread(speedmonitor);
        t.start();
    }

    public void displayCodes() {

        ModifiedTroubleCodesObdCommand tcoc = new ModifiedTroubleCodesObdCommand();
        try {
            tcoc.run(socket.getInputStream(), socket.getOutputStream());


            result = tcoc.getFormattedResult();

            if (result.length() > 1) {

               // codeView.setText(result);

                //If codes are found, display button to clear codes
               // ClearDTCButton = (Button) findViewById(R.id.ClearDTCButton);
               // ClearDTCButton.setOnClickListener(this);
               // ClearDTCButton.setVisibility(View.VISIBLE);


            } else {
                //codeView.setText(R.string.no_codes);

                Timer t = new Timer(false);
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                               // codeView.setText("");
                            }
                        });
                    }
                }, 4000);

            }
        } catch (IOException | InterruptedException | NullPointerException e) {
            e.printStackTrace();
        }


    }

    public void clearDTC() {

        ResetTroubleCodesCommand clear = new ResetTroubleCodesCommand();
        try {
            clear.run(socket.getInputStream(), socket.getOutputStream());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        String result = clear.getFormattedResult();
        Log.d("CLEAR DTC", "Trying clearDTC result: " + result);

        if (result.equals("44")) {

          //  codeView.setText("");
           // ClearDTCButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CloseCommand closeCommand = new CloseCommand();
        try {
            closeCommand.run(socket.getInputStream(), socket.getOutputStream());
        } catch (IOException | InterruptedException | NoDataException e) {
            e.printStackTrace();
        }
    }

    private class ModifiedTroubleCodesObdCommand extends TroubleCodesCommand {

        @Override
        public String getResult() {

            return rawData.replace("SEARCHING...", ""); //.replace("NODATA", "");
        }


    }

}


//maintain bluetooth when changing activities




//IDEAS

// connect view
//Scan for BlueTooth devices button and a display for already connected devices

//put bluetooth connecting in its own thread