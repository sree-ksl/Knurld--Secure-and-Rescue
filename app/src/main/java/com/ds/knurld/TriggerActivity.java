package com.ds.knurld;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by hello on 30/06/16.
 */
public class TriggerActivity extends Activity implements View.OnClickListener {

    TextView tvAddress;
    String result;
    String locAddress;
    String link;

    Button triggerCall;

    AppLocationService appLocationService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trigger);

        tvAddress = (TextView) findViewById(R.id.tvAddress);
        appLocationService = new AppLocationService(TriggerActivity.this);

        triggerCall = (Button) findViewById(R.id.triggerBtn);
        triggerCall.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        getLocation();
        sendMessage();

    }

    public void getLocation() {

        Location gpsLocation = appLocationService.getLocation(LocationManager.GPS_PROVIDER);
        //Location networkLocation = appLocationService.getLocation(LocationManager.NETWORK_PROVIDER);
        if (gpsLocation != null) {
            double latitude = gpsLocation.getLatitude();
            double longitude = gpsLocation.getLongitude();
            LocationAddress locationAddress = new LocationAddress();
            locationAddress.getAddressFromLocation(latitude, longitude, getApplicationContext(), new GeocoderHandler());
            result = "Lat:" + latitude + " Longi:" + longitude;
            link = "http://maps.google.com/maps?q=loc:" + String.format("%f,%f", latitude, longitude);
        } else {
            showSettingsAlert();
        }

    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(TriggerActivity.this);
        alertDialog.setTitle("Settings");
        alertDialog.setMessage("Enable Location provider.Go to Settings menu?");
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        TriggerActivity.this.startActivity(intent);
                    }
                });
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        alertDialog.show();
    }

    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    Log.d("Address : ", locationAddress);
                    break;
                default:
                    locationAddress = null;
            }
            tvAddress.setText(locationAddress);
            locAddress = tvAddress.getText().toString();
            Log.d("LocationAddress:", locAddress);

            //sendMessage();
        }

    }

    private void sendMessage() {
        //Send help message
        StringBuilder sosMsg = new StringBuilder();

        //TO DO: send this result to google maps
        sosMsg.append(link);
        sosMsg.append(locAddress);

        //TO DO: change this to the numbers the user selects from their contacts
        String[] numbers = new String[]{"Moble Number"};
        for (int i = 0; i < numbers.length; i++) {
            SmsManager sosHelpMsg = SmsManager.getDefault();
            //TO DO: Set the phone number to list of numbers based on nearest located people available
            sosHelpMsg.sendTextMessage(numbers[i], null, sosMsg.toString(), null, null);
        }

        // add PhoneStateListener for monitoring
        MyPhoneListener phoneListener = new MyPhoneListener();
        //call
        try {
            TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            // receive notifications of telephony state changes
            telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
            String helpline = "Mobile Number";  //change this to the user selected contact
            Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + helpline));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivity(callIntent);
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),"Call Failed",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }


    }

    private class MyPhoneListener extends PhoneStateListener{
        private boolean onCall = false;

        @Override
        public void  onCallStateChanged(int state, String incomingNumber){
            switch (state){
                case TelephonyManager.CALL_STATE_RINGING:
                    //phone ringing
                    Toast.makeText(getApplicationContext(), incomingNumber + " calls you",
                            Toast.LENGTH_LONG).show();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // one call exists that is dialing, active, or on hold
                    Toast.makeText(getApplicationContext(), "on call...",
                            Toast.LENGTH_LONG).show();
                    //because user answers the incoming call
                    onCall = true;
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    // in initialization of the class and at the end of phone call

                    // detect flag from CALL_STATE_OFFHOOK
                    if (onCall == true) {
                        Toast.makeText(getApplicationContext(), "restart app after call", Toast.LENGTH_LONG).show();
                        // restart our application
                        Intent restart = getBaseContext().getPackageManager().
                                getLaunchIntentForPackage(getBaseContext().getPackageName());
                        restart.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(restart);
                        onCall = false;
                    }
                    break;
                default:
                    break;
            }
        }

    }


}
