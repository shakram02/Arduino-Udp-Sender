package com.example.ahmed.udpsender;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    // Regex for validating IP addresses
    private static final Pattern IP_ADDRESS
            = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");

    private DatagramSocket socket;
    private EditText ipEditText;
    private EditText portEditText;
    private static final int TIMEOUT_MILLIS = 2000;   // Wait for 3 secs for each socket operation before time out

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        // Get the handles of the text fields to use them in validation later
        // NOTE: this must exist after "setContentView(R.layout.activity_main);"
        ipEditText = ((EditText) findViewById(R.id.editTextIp));
        portEditText = ((EditText) findViewById(R.id.editTextPort));

        try {
            socket = new DatagramSocket();  // Create a UDP socket
            socket.setBroadcast(true);  // Enable broadcasts
            socket.setSoTimeout(TIMEOUT_MILLIS); // Set timeout for socket operations

        } catch (SocketException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        final String ipString = ipEditText.getText().toString();
        final String portString = portEditText.getText().toString();

        boolean ipValidated = validateIp(ipString);
        boolean portValidated = validatePort(portString);

        if (!ipValidated && !portValidated) {
            showToast("Ip and Port are invalid");
        } else if (!ipValidated) {
            showToast("IP is invalid");
        } else if (!portValidated) {
            showToast("Port number is invalid");
        } else {
            // Correct params
            final Editable message = ((EditText) findViewById(R.id.textAreaMessage)).getText();

            if (message.length() == 0) return;


            // Network operations must be started on a separate
            // thread other than the UI thread
            new Thread() {
                public void run() {

                    if (sendPacket(message.toString(), ipString, Short.parseShort(portString))) {
                        String reply = receivePacket();
                        showToastOnUiThread(reply);
                    }
                }
            }.start();

            message.clear();    // Clear the message edit text


        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean validateIp(String ip) {


        if (ip.length() == 0) return false;

        if (!IP_ADDRESS.matcher(ip).matches()) {
            // Show a message
            showToast("Please enter a valid IP Address");
            return false;
        } else {
            return true;
        }
    }

    private boolean validatePort(String text) {

        if (text.length() == 0) return false;

        try {
            Short.parseShort(text); // If this succeeds, then it's a valid port
            return true;
        } catch (NumberFormatException e) {
            showToast("A valid port number is between 1 and " + Short.MAX_VALUE);
            return false;
        }

    }

    private boolean sendPacket(String message, String ipString, short portNumber) {

        byte messageBytes[] = message.getBytes();

        try {

            // Create the packet containing the message, IP and port number
            final DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length,
                    InetAddress.getByName(ipString), portNumber);

            socket.send(packet);

            // If you want to manipulate the GUI, you must run that
            // code on the MainUI thread
            showToastOnUiThread("Sent!");
            return true;    // Everything wen well

        } catch (final UnknownHostException e) {

            showToastOnUiThread("Couldn't find host");
            return false;

        } catch (final IOException e) {

            showToastOnUiThread(e.getMessage());
            return false;   // Something went wrong
        }
    }

    private boolean sendPacket(DatagramPacket packet) {

        try {

            socket.send(packet);

            // If you want to manipulate the GUI, you must run that
            // code on the MainUI thread
            showToastOnUiThread("Sent!");
            return true;    // Everything wen well

        } catch (final IOException e) {

            showToastOnUiThread(e.getMessage());
            return false;   // Something went wrong
        }
    }

    private String receivePacket() {
        try {
            byte buffer[] = new byte[255];
            DatagramPacket p = new DatagramPacket(buffer, buffer.length);
            socket.receive(p);
            return new String(p.getData());    // Convert the packet to a string and return it
        } catch (IOException e) {
            showToastOnUiThread(e.getMessage());
        }
        return "";  // Return nothing
    }

    private void showToastOnUiThread(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showToast(text);
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
