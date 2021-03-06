package com.example.ethiril.a15068126_mobile_dev_1cwk50_app;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ethiril.a15068126_mobile_dev_1cwk50_app.mqtt.mqtt.publisher.Publisher;
import com.example.ethiril.a15068126_mobile_dev_1cwk50_app.mqtt.subscriber.Subscriber;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public Spinner roomSelection;
    public SwitchCompat switchCompat;
    public FloatingActionButton openDoor;
    private static Context context;

    // localhost at IPv4 For testing with mobile device
    //public static final String sensorServerURL = "http://192.168.1.9:8080/15068126_Mobile_Dev_1CWK50_Server/SensorServerDB";
    public final String sensorServerURL = "http://10.0.2.2:8080/15068126_Mobile_Dev_1CWK50_Server/SensorServerDB";
    public static final String userID = "15068126";
    public static final String clientId = userID + "-subAndroid";
    private final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String androidID;
    String lockState;
    Gson gson = new Gson();
    Date date;

    TextView roomState;
    TextView lastEntryValue;
    TextView mostRecentUser;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainActivity.context = getApplicationContext();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Room Activity");
        roomState = findViewById(R.id.roomStateField);
        lastEntryValue = findViewById(R.id.LastEntryValueField);
        mostRecentUser = findViewById(R.id.mostRecentValueField);
        roomSelection = (Spinner) findViewById(R.id.roomSelection);
        roomSelection.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, getRooms()));
        final String[] room = {roomSelection.getSelectedItem().toString()};
        final int[] roomID = {getRoomID(room[0])};
        roomSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            String firstItem = String.valueOf(roomSelection.getSelectedItem());

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (firstItem.equals(String.valueOf(roomSelection.getSelectedItem()))) {
                    Log.d("UI DEBUGGER", "First item is selected, updating UI..");
                    room[0] = roomSelection.getSelectedItem().toString();
                    roomID[0] = getRoomID(room[0]);
                    updateUI();
                } else {
                    Toast.makeText(parent.getContext(),
                            "You have selected : " + parent.getItemAtPosition(pos).toString(),
                            Toast.LENGTH_LONG).show();
                    Log.d("UI DEBUGGER", "UI Updated with values for the current room.");
                    room[0] = roomSelection.getSelectedItem().toString();
                    roomID[0] = getRoomID(room[0]);
                    updateUI();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg) {
            }
        });
        final Subscriber sub = new Subscriber();
        final Publisher publisher = new Publisher();
        androidID = "androidDevice::" + android.provider.Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        switchCompat = findViewById(R.id.doorLockSwitch);
        openDoor = findViewById(R.id.fab);

        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                room[0] = roomSelection.getSelectedItem().toString();
                roomID[0] = getRoomID(room[0]);
                lockState = (isChecked) ? "Locked" : "Unlocked";
                String dateStr = getDate(date);
                Snackbar.make(buttonView, "Room " + room[0] + " is: " + lockState, Snackbar.LENGTH_LONG)
                        .setAction("ACTION", null).show();
                roomState.setText(lockState);
                lockState = (isChecked) ? "lock" : "unlock";
                Sensor sensor = new Sensor(5, roomID[0], "Android_Device", room[0], androidID, dateStr, isChecked, "success");
                String json = gson.toJson(sensor);
                String topic = "/" + room[0] + "/" + lockState;
                Log.d("UI DEBUGGER", "Lock switch state changed, sending message: " + json + ", to topic: " + topic);
                publisher.publishJson(json, topic);
            }
        });

        openDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // select from dropdown
                Log.d("LOG DEBUG", "Opening the door, providing it has not been locked.");
                Snackbar.make(v, "Opened Door " + room[0], Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        date = new Date();
                        String dateStr = getDate(date);
                        // TagID is hardcoded as the application will have to work on all doors, in
                        // the real world, this would be expanded to handle users per device
                        // such as signing up on the app, using your play store account and linking to a door
                        boolean locked = switchCompat.isChecked();
                        Sensor sensor = new Sensor(5, roomID[0], "Android_Device", room[0], androidID, dateStr, locked, "success");
                        String json = gson.toJson(sensor);
                        String topic = "/" + room[0];
                        Log.d("UI DEBUGGER", "Making an attempt to open door in room " + room[0] + ", sending message: " + json + ", to topic: " + topic);
                        publisher.publishJson(json, topic);
                    }
                });
            }
        });
        // call the mqtt client here
        // StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        // StrictMode.setThreadPolicy(policy);

        Log.d("LOG DEBUG", "Trying to subscribe . . .");
        sub.start(MainActivity.this, getRooms());
    }

    public void updateUI() {
        Log.d("UI DEBUG", "Updating the UI after notification or room change");
        int roomID = getRoomID(roomSelection.getSelectedItem().toString());
        boolean locked = getLocked(roomID);
        String stateStr = (locked) ? "Locked" : "Unlocked";
        roomState.setText(stateStr);
        Sensor recentData = getMostRecentForRoom(roomID);
        lastEntryValue.setText(recentData.getTimeInserted());
        String[] user = getUser(recentData.getUserID()).split(",");
        if (user.length == 1) {
            mostRecentUser.setText("No data");
        } else {
            mostRecentUser.setText(user[1] + " " + user[2]);
        }
        switchCompat.setChecked(locked);
    }

    public String getDate(Date date) {
        date = new Date();
        return sdf.format(date);
    }

    private int getRoomID(String room) {
        getRoomIDTask getRoomID = new getRoomIDTask();
        int roomID = 0;
        try {
            roomID = getRoomID.execute(room).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return roomID;
    }

    private boolean getLocked(int roomID) {
        getLockStateTask lockStateTask = new getLockStateTask();
        boolean locked = true;
        try {
            locked = lockStateTask.execute(roomID).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return locked;
    }

    private String[] getRooms() {
        getRoomsTask getRooms = new getRoomsTask();
        String[] rooms = null;
        try {
            rooms = getRooms.execute().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rooms;
    }

    public String[] fetchRooms(String sensorServerURL) {
        String fullURL = sensorServerURL + "?getrooms";
        String result = tryRequest(fullURL).replaceAll("\\[", "").replaceAll("\\]", "").replace("\"", "");
        String[] rooms = result.split(",");
        return rooms;
    }

    public Sensor fetchMostRecentForRoom(int RoomID) {
        String fullURL = null;
        try {
            fullURL = sensorServerURL + "?getdata=" + URLEncoder.encode("{\"retrieveForRoom\":" + RoomID + "}", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String result = tryRequest(fullURL);
        return gson.fromJson(result, Sensor.class);
    }

    private Sensor getMostRecentForRoom(int RoomID) {
        getRecentRoomTask recentRoom = new getRecentRoomTask();
        Sensor room = null;
        try {
            room = recentRoom.execute(RoomID).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return room;
    }

    private String getUser(int UserID) {
        getUserDetailsTask getUsers = new getUserDetailsTask();
        String users = null;
        try {
            users = getUsers.execute(UserID).get().replaceAll("\\[", "").replaceAll("\\]", "").replace("\"", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    public String getUserDetails(int UserID) {
        String fullURL = null;
        try {
            fullURL = sensorServerURL + "?getuserdetails=" + URLEncoder.encode("{\"UserID\":\"" + UserID + "\"}", "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String result = tryRequest(fullURL);
        JsonElement jElem = gson.fromJson(result, JsonElement.class);
        JsonObject obj = jElem.getAsJsonObject();
        // Remove the JSON syntax and return the room id
        String idStr = obj.get("userdetails").toString().replaceAll("^\"|\"$", "");
        return idStr;
    }

    public int fetchRoomID(String sensorServerURL, String roomName) {
        String fullURL = null;
        try {
            fullURL = sensorServerURL + "?getroomid=" + URLEncoder.encode("{\"RoomName\":\"" + roomName + "\"}", "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String result = tryRequest(fullURL);
        JsonElement jElem = gson.fromJson(result, JsonElement.class);
        JsonObject obj = jElem.getAsJsonObject();
        // Remove the JSON syntax and return the room id
        String idStr = obj.get("RoomID").toString().replaceAll("^\"|\"$", "");
        return Integer.parseInt(idStr);
    }


    public String[] getTopics() {
        String[] topics = getRooms();
        for (int i = 0; i < topics.length; i++) {
            topics[i] = "/" + topics[i];
        }
        return topics;
    }

    public boolean checkLock(String sensorServerURL, int roomid) {
        String fullURL = null;
        try {
            fullURL = sensorServerURL + "?getlockstatus="
                    + URLEncoder.encode("{\"RoomID\":\"" + roomid + "\"}", "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String result = tryRequest(fullURL);
        JsonElement jElem = gson.fromJson(result, JsonElement.class);
        JsonObject obj = jElem.getAsJsonObject();
        // Remove the JSON syntax and return the room id
        String lockedStr = obj.get("locked").toString().replaceAll("^\"|\"$", "");
        boolean locked = Boolean.valueOf(lockedStr);
        return locked;
    }

    public String tryRequest(String fullURL) {
        String result = "";
        String line;
        try {
            URL url = new URL(fullURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    private class getLockStateTask extends AsyncTask<Integer, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Integer... params) {
            return checkLock(sensorServerURL, params[0]);
        }

        @Override
        protected void onPostExecute(Boolean lockState) {

        }
    }

    private class getRecentRoomTask extends AsyncTask<Integer, Void, Sensor> {

        @Override
        protected Sensor doInBackground(Integer... params) {
            return fetchMostRecentForRoom(params[0]);
        }

        @Override
        protected void onPostExecute(Sensor sensor) {

        }
    }

    private class getRoomIDTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            return fetchRoomID(sensorServerURL, params[0]);
        }

        @Override
        protected void onPostExecute(Integer roomID) {

        }
    }

    private class getUserDetailsTask extends AsyncTask<Integer, Void, String> {

        @Override
        protected String doInBackground(Integer... params) {
            return getUserDetails(params[0]);
        }

        @Override
        protected void onPostExecute(String user) {

        }
    }

    private class getRoomsTask extends AsyncTask<Void, Void, String[]> {
        @Override
        protected String[] doInBackground(Void... Void) {
            return fetchRooms(sensorServerURL);
        }

        @Override
        protected void onPostExecute(String[] rooms) {
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
}
