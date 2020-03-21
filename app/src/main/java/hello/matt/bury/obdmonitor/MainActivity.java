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

    //Button RPMFragToggle, SpeedFragToggle;
    private String deviceAddress = "", RPM = "0", SPEED = "0", VIN = "";
    private Boolean continueRPM = true, continueSpeed = true, bluetoothConnected = false;
    private Button DetectVINButton, DisplaySpeedButton, DisplayRPMButton, CheckDTCButton, ClearDTCButton;
    private TextView speedView, rpmView, vinView, codeView;

    // Handlers for passing the values from their running thead to the main activity view
    Handler RPMHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Bundle b = msg.getData();
            RPM = b.getString("RPM");
            rpmView.setText(RPM);
        }
    };
    Handler SpeedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Bundle b = msg.getData();
            SPEED = b.getString("Speed");
            speedView.setText(SPEED);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // When app is created, load the connect to BT device view
        setContentView(R.layout.connect_view);
    }

    protected void baseViewChange() {
        // Initialize the views and buttons for main screen
        rpmView = (TextView) findViewById(R.id.rpmView);
        speedView = (TextView) findViewById(R.id.speedView);
        vinView = (TextView) findViewById(R.id.vinView);
        codeView = (TextView) findViewById(R.id.codeView);

        DisplaySpeedButton = (Button) findViewById(R.id.DisplaySpeedButton);
        DisplaySpeedButton.setTag(1);
        DisplaySpeedButton.setOnClickListener(this);

        DisplayRPMButton = (Button) findViewById(R.id.DisplayRPMButton);
        DisplayRPMButton.setTag(1);
        DisplayRPMButton.setOnClickListener(this);

        DetectVINButton = (Button) findViewById(R.id.DetectVINButton);
        DetectVINButton.setOnClickListener(this);

        CheckDTCButton = (Button) findViewById(R.id.CheckDTCButton);
        CheckDTCButton.setOnClickListener(this);

        // Switch view to main screen
        setContentView(R.layout.activity_main);
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


    // Handles clicking of all buttons for now
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.connectButton:
                checkBT();
                break;

            case R.id.DisplayRPMButton:
                if (!bluetoothConnected) {
                    Toast.makeText(MainActivity.this, "No BlueTooth Connection", Toast.LENGTH_SHORT).show();
                    break;
                }
                // Swap button text based on thread status
                int RPMstatus = (Integer) v.getTag();
                if (RPMstatus == 1) {
                    DisplayRPMButton.setText(R.string.pause_rpm);
                    DisplayRPMButton.setTag(0);
                    continueRPM = true;
                    displayRPM();
                    break;
                } else if (RPMstatus == 0) {
                    DisplayRPMButton.setText(R.string.resume_rpm);
                    DisplayRPMButton.setTag(1);
                    continueRPM = false;
                    break;
                }

            case R.id.DisplaySpeedButton:
                if (!bluetoothConnected) {
                    Toast.makeText(MainActivity.this, "No BlueTooth Connection", Toast.LENGTH_SHORT).show();
                    break;
                }
                // Swap button text based on thread status
                int speedstatus = (Integer) v.getTag();
                if (speedstatus == 1) {
                    DisplaySpeedButton.setText(R.string.pause_speed);
                    DisplaySpeedButton.setTag(0);
                    continueSpeed = true;
                    displaySpeed();
                    break;
                } else if (speedstatus == 0) {
                    DisplaySpeedButton.setText(R.string.resume_speed);
                    DisplaySpeedButton.setTag(1);
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
        // Get Bluetooth adapater from device
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        //If device does not support Bluetooth
       /* if (btAdapter == null) {
            //TODO handle error
        }*/
        //Enable BT if not enabled
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // Prompts the user to enable Bluetooth
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // Else Bluetooth is enabled and we can proceed
            displayBTConnectDialog();
        }
    }

    public void displayBTConnectDialog() {
        // Create device list
        final ArrayList deviceStrs = new ArrayList();
        //final ArrayList devices = new ArrayList();

        Set pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (Object device : pairedDevices) {
                BluetoothDevice device1 = (BluetoothDevice) device;
                Log.d("gping2", "BT: " + device1.getName() + " - " + device1.getAddress());
                // Add device name AND address to the list
                deviceStrs.add(device1.getName() + "\n" + device1.getAddress());
                // devices.add(device1.getAddress());
            }
        }
        // Initialize the dialog to allow user to select Bluetooth device from list
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice,
                deviceStrs.toArray(new String[deviceStrs.size()]));
        // Control what happens when user selects a device from the list
        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                deviceAddress = (String) deviceStrs.get(position);
                Log.d("gping2", "Picked: " + deviceAddress);
                int index = deviceAddress.indexOf(':');
                // Grab the address portion of the device
                deviceAddress = deviceAddress.substring(index - 2);
                // Connect to the selected device using Bluetooth
                connect_bt();
            }
        });
        // Display the device selection dialog
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
        // Execute these commands per the obd-java-api instructions
        try {
            new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            Thread.sleep(200);
            new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            Thread.sleep(200);
            new TimeoutCommand(25).run(socket.getInputStream(), socket.getOutputStream());
            Thread.sleep(200);
            new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
            Thread.sleep(200);
            // Chanve view to main activity
            baseViewChange();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void displayVIN() {
        // Create instance of vin command class
        VinCommand VinCommand = new VinCommand();
        try {
            /* Call run function of VinCommand class to send the request over bluetooth */
            VinCommand.run(socket.getInputStream(), socket.getOutputStream());
            VIN = VinCommand.getFormattedResult();
            //Log.d("gping2", "VIN: " + VIN);
            // vinView.setText(VIN);
            //NullPointerException = no BlueTooth connection
        } catch (IOException | InterruptedException | NoDataException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void displayRPM() {
        // Declare runnable interface
        Runnable rpmmonitor = new Runnable() {
            /* When an object implementing interface Runnable is used to create a thread,
              starting the thread causes the object's run method to be called in that
              separately executing thread. */
            @Override
            public void run() {
                // Create instance of RpmCommand class
                RPMCommand engineRpmCommand = new RPMCommand();
                // Control when thread is running
                while (!Thread.currentThread().isInterrupted() && continueRPM) {
                    try {
                        // Run command
                        engineRpmCommand.run(socket.getInputStream(), socket.getOutputStream());
                        // Set result to RPM string
                        RPM = engineRpmCommand.getFormattedResult();
                        //Log.d("gping2", "RPM: " + RPM);

                        // Bundle the string and send to handler
                        Message msg = new Message();
                        Bundle b = new Bundle();
                        b.putString("RPM", RPM);
                        msg.setData(b);
                        RPMHandler.sendMessage(msg);
                        // Sleep thread to control refresh speed
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException | InterruptedException | NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        // Create a thread for the runnable
        Thread t = new Thread(rpmmonitor);
        t.start();
    }

    public void displaySpeed() {
        // Declare runnable interface
        Runnable speedmonitor = new Runnable() {
            /* When an object implementing interface Runnable is used to create a thread,
               starting the thread causes the object's run method to be called in that
               separately executing thread. */
            @Override
            public void run() {
                // Create instance of SpeedCommand class
                SpeedCommand vehicleSpeedCommand = new SpeedCommand();
                // Control when thread is running
                while (!Thread.currentThread().isInterrupted() && continueSpeed) {
                    try {
                        // Run command
                        vehicleSpeedCommand.run(socket.getInputStream(), socket.getOutputStream());
                        // Set result to SPEED string
                        SPEED = vehicleSpeedCommand.getFormattedResult();
                        //Log.d("gping2", "Speed: " + SPEED);

                        // Bundle the string and send to handler
                        Message msg = new Message();
                        Bundle b = new Bundle();
                        b.putString("Speed", SPEED);
                        msg.setData(b);
                        SpeedHandler.sendMessage(msg);
                        // Sleep thread to control refresh speed
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException | InterruptedException | NullPointerException e) {
                        e.printStackTrace();
                        //continueSpeed = false;
                    }
                }
            }
        };
        // Create a thread for the runnable
        Thread t = new Thread(speedmonitor);
        t.start();
    }

    public void displayCodes() {
        // Create instance of ModifiedTroubleCodesObdCommand class
        ModifiedTroubleCodesObdCommand TCOC = new ModifiedTroubleCodesObdCommand();
        try {
            /* Call run function of ModifiedTroubleCodesObdCommand class to send the clear
            Diagnostic Trouble Codes command over bluetooth */
            TCOC.run(socket.getInputStream(), socket.getOutputStream());
            // Grab results from run
            String result = TCOC.getFormattedResult();
            // If codes are found, display them in textview
            if (result.length() > 1) {
                codeView.setText(result);
                // Display button to clear codes
                ClearDTCButton = (Button) findViewById(R.id.ClearDTCButton);
                ClearDTCButton.setOnClickListener(this);
                ClearDTCButton.setVisibility(View.VISIBLE);
                // Else there are no codes to display
            } else {
                // Display no codes message for 4 seconds then remove
                codeView.setText(R.string.no_codes);
                Timer t = new Timer(false);
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                codeView.setText("");
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
        // Create instance of ResetTroubleCodesCommand class
        ResetTroubleCodesCommand RTCC = new ResetTroubleCodesCommand();
        try {
            /* Call run function of ResetTroubleCodesCommand class to send the clear
            Diagnostic Trouble Codes command over bluetooth */
            RTCC.run(socket.getInputStream(), socket.getOutputStream());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // Grab results from run
        String result = RTCC.getFormattedResult();
        Log.d("CLEAR DTC", "Trying clearDTC result: " + result);
        // The result '44' is the expected response for the command reset trouble code
        if (result.equals("44")) {
            codeView.setText("");
            ClearDTCButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close bluetooth socket
        try {
            socket.close();
        } catch (IOException e) {
            Log.e("gping2", "Could not close the client socket", e);
        }
        /* If a connection is lost, you will need to tell the ELM327 to ‘close’
           the current connection */
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
