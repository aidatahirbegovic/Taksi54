package com.example.aida.newtaxi;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import afu.org.checkerframework.checker.units.qual.A;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, RoutingListener {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest; //used to request a quality of service for location updates
    private FusedLocationProviderClient mFusedLocationClient; //location updates, fused location api, the fused location provider is used to retrieve the device's last known location.
    SupportMapFragment mapFragment; //This fragment is the simplest way to place a map in an application
    Marker mCurrLocationMarker;

    private Button mLogout, mSettings, mRideStatus, mHistory, mKabul, mRed;
    private Switch mWorkingSwitch;


    private int status = 0;

    private String customerId = "", destination, buton;
    private LatLng destinationLatLng,pickupLatLng;
    private float rideDistance;

    private Boolean isLoggingOut = false;

    private LinearLayout mCustomerInfo;
    private ImageView mCustomerProfileImage;
    private TextView mCustomerName, mCustomerPhone, mCustomerDestination;

    boolean clicked;
    private String durum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        buton = "";


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        polylines = new ArrayList<>();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this); //Creates a new instance of FusedLocationProviderClient for use in an Activity.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map); //placing map in app
        mapFragment.getMapAsync(this); //this comes with map activity, calling map, and starts the map

        mCustomerInfo = findViewById(R.id.customerInfo);
        mCustomerName = findViewById(R.id.customerName);
        mCustomerPhone = findViewById(R.id.customerPhone);
        mCustomerDestination = findViewById(R.id.customerDestination);
        mCustomerProfileImage = findViewById(R.id.customerProfileImage);

        mWorkingSwitch = findViewById(R.id.workingSwitch);

        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    connectDriver();
                }else{
                    disconnectDriver();
                }
            }
        });


        mSettings = findViewById(R.id.settings);
        mLogout = findViewById(R.id.logout);
        mHistory = findViewById(R.id.history);
        mKabul = findViewById(R.id.kabul);
        mRed = findViewById(R.id.red);
        mKabul.setVisibility(View.VISIBLE);
        mRed.setVisibility(View.VISIBLE);
        mRideStatus = findViewById(R.id.rideStatus);
        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status){
                    case 1:  //on itss way to pick up customer

                            status = 2;
                            erasePolylines();
                            if(destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0)
                            {
                                getRouteToMarker(destinationLatLng);
                            }
                            mRideStatus.setText("drive completed");

                        break;

                    case 2: //driver inside his car
                        if(clicked == true){
                            recordRide();
                            endRide();
                        }else{
                            rideRed();
                        }

                        break;

                }
            }
        });

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut = true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut(); //firebase gives us options to sign out easily
                Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); //to close this activity
                return;
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapActivity.this, DriverSettingsActivity.class);
                startActivity(intent);
                return;

            }
        });


        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Drivers");
                startActivity(intent);
                return;
            }
        });
        getAssignedCustomer();
        clicked=false;
        mKabul.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //isWorking();
                clicked = true;
                buton = "kabul";

                /*String driverUserId = FirebaseAuth.getInstance().getCurrentUser().getUid(); //adds in firebase driversAvailable
                DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversWorking"); //users id is stored in drivers avai;ab;e

                GeoFire geoFireDriverAvb = new GeoFire(refAvailable);
                geoFireDriverAvb .setLocation(driverUserId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversAvailable"); //users id is stored in drivers avai;ab;e

                GeoFire geoFireDriverWrk = new GeoFire(refWorking);
                geoFireDriverWrk .setLocation(driverUserId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));*/
                //getRouteToMarker(pickupLatLng);
                mKabul.setVisibility(View.GONE);
                mRed.setVisibility(View.GONE);
                mRideStatus.setVisibility(View.VISIBLE);
            }
        });
        mRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clicked = false;
                rideRed();
                buton = "red";

                //String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId);

                HashMap map = new HashMap(); //Hash table based implementation of the Map interface
                map.put("rideDurumu", buton);
                assignedCustomerRef.updateChildren(map);
                //durum = pref.getString("butonDurumu", "red");

                /*Intent i = new Intent(getApplicationContext(), CustomerMapActivity.class);
                i.putExtra("buton", "red");*/
                //startActivityForResult(i, 1);
                //rideRed();
               /* SharedPreferences prefs = DriverMapActivity.this.getSharedPreferences("com.Taksi54", Context.MODE_PRIVATE);
                prefs.edit().putString("butonSecimi", buton).commit();*/

                /*SharedPreferences.Editor editor = getSharedPreferences("com.Taksi54", MODE_PRIVATE).edit();
                editor.putString("butonSecimi", "red");
                editor.apply();*/

                String driverUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
                DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
                GeoFire geoFireAvailable = new GeoFire(refAvailable);
                GeoFire geoFireWorking = new GeoFire(refWorking);

                geoFireWorking.removeLocation(driverUserId);
                geoFireAvailable.setLocation(driverUserId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                /*String driverUserId = FirebaseAuth.getInstance().getCurrentUser().getUid(); //adds in firebase driversAvailable
                DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable"); //users id is stored in drivers avai;ab;e

                GeoFire geoFireDriverAvb = new GeoFire(refAvailable);
                geoFireDriverAvb .setLocation(driverUserId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking"); //users id is stored in drivers avai;ab;e

                GeoFire geoFireDriverWrk = new GeoFire(refWorking);
                geoFireDriverWrk .setLocation(driverUserId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                erasePolylines();


                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(customerId);


                //removing location od userid, customer from firebase
                customerId = "";
                rideDistance = 0;

                if(pickupMarker != null){
                    pickupMarker.remove(); //removing marker
                }
                if(assignedCustomerPickUpLocationRefListener != null){
                    assignedCustomerPickUpLocationRef.removeEventListener(assignedCustomerPickUpLocationRefListener);
                    //cancelling listener

                }
                mCustomerInfo.setVisibility(View.GONE);
                mCustomerName.setText("");
                mCustomerPhone.setText("");
                mCustomerDestination.setText("Destination: --");
                mCustomerProfileImage.setImageResource(R.mipmap.ic_launcher);
                mRideStatus.setVisibility(View.GONE);
                mKabul.setVisibility(View.VISIBLE);
                mRed.setVisibility(View.VISIBLE);*/
            }
        });

    }
    /*public void onClick(View view) {
        int code;

        switch(view.getId()){
            case R.id.button1:
                code=1;
                break;

            case R.id.button2:
                code=2;
                break;
        }
        Intent i = new Intent(this, ResultActivity.class);
        i.putExtra("yourcode", code);
        startActivityForResult(i, 1);
    }*/

    private void getAssignedCustomer(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        //finding a customer that called this user
        //final DatabaseReference cust = FirebaseDatabase.getInstance().getReference().child("customerRequest").child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){ //read data from a specific location, Returns true if the snapshot contains a non-null value.
                    status =1;
                    customerId = dataSnapshot.getValue().toString(); //getting value in this case location of customer
                    getAssignedCustomerPickUpLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();

                }else{
                    if(clicked == false){ //else will work  when ride is cancelled
                        rideRed();
                    }else{
                        endRide();
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }


    private Marker pickupMarker;
    private DatabaseReference assignedCustomerPickUpLocationRef;
    private ValueEventListener assignedCustomerPickUpLocationRefListener;
    private void getAssignedCustomerPickUpLocation(){

        assignedCustomerPickUpLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l"); //child l

        assignedCustomerPickUpLocationRefListener = assignedCustomerPickUpLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && !customerId.equals("")){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat =0;
                    double locationLng = 0;

                    if(map.get(0) != null){ //zato sto jelatituda u firebasu 0
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){ //zato sto jelatituda u firebasu 0
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    pickupLatLng = new LatLng(locationLat,locationLng); //has coordinates of lat and lan, customer
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pick Up Lokasyonu").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker)));

                    getRouteToMarker(pickupLatLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getRouteToMarker(LatLng pickupLatLng) {
        if (pickupLatLng != null && mLastLocation != null) {

            Routing routing = new Routing.Builder()
                    .key("AIzaSyC6mIQdANJdf4uBhiZ8pCV32AsEcH2irDw")
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)
                    .build();
            routing.execute();

        }
    }


    private void getAssignedCustomerDestination(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest"); //finding a customer that called this user
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) { //read data from a specific location, Returns true if the snapshot contains a non-null value.
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if(map.get("destination") != null){
                        destination = map.get("destination").toString();
                        mCustomerDestination.setText("Destination: " +destination);
                    }
                    else{
                        mCustomerDestination.setText("Destination:  --");
                    }
                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if(map.get("destinationLat") != null){
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if(map.get("destinationLng") != null){
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatLng = new LatLng(destinationLat, destinationLng);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }
    private void getAssignedCustomerInfo(){
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase=FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);

        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){ //makes sure that there is stg in database

                    Map<String,Object> map = (Map<String,Object>)dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mCustomerName.setText("Adı     :" +map.get("name").toString());
                    }

                    if(map.get("phone")!=null){
                        mCustomerPhone.setText("Telefon No :" +map.get("phone").toString());
                    }

                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void rideRed(){

        erasePolylines();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); //getting users id


        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest"); //removing customer id from drivers
        driverLocation.setValue(true); //it will rewrite child


        if(pickupMarker != null){
            pickupMarker.remove(); //removing marker
        }
        if(assignedCustomerPickUpLocationRefListener != null){
            assignedCustomerPickUpLocationRef.removeEventListener(assignedCustomerPickUpLocationRefListener);
            //cancelling listener
        }
        mCustomerInfo.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerPhone.setText("");

        mCustomerDestination.setText("Destination: --");
        mCustomerProfileImage.setImageResource(R.mipmap.ic_launcher);
        mRideStatus.setVisibility(View.GONE);
        mKabul.setVisibility(View.VISIBLE);
        mRed.setVisibility(View.VISIBLE);

    }
    private void endRide() {
        erasePolylines();

            mRideStatus.setText("picked customer");

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); //geting users id
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
            driverRef.removeValue();



        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerId); //removing location od userid, customer from firebase
        customerId = "";
        rideDistance = 0;

        if(pickupMarker != null){
            pickupMarker.remove(); //removing marker
        }
        if(assignedCustomerPickUpLocationRefListener != null){
            assignedCustomerPickUpLocationRef.removeEventListener(assignedCustomerPickUpLocationRefListener);
            //cancelling listener

        }
        mCustomerInfo.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerPhone.setText("");
        mCustomerDestination.setText("Destination: --");
        mCustomerProfileImage.setImageResource(R.mipmap.ic_launcher);
        mRideStatus.setVisibility(View.GONE);
        mKabul.setVisibility(View.VISIBLE);
        mRed.setVisibility(View.VISIBLE);

    }

    private void recordRide() {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); //geting users id
        DatabaseReference drRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");
        DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference().child("historySingleForCustomer");

        String requestId = historyRef.push().getKey();


        customerRef.child(requestId).setValue(true);
        drRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("driver",userId);
        map.put("customer",customerId);
        map.put("rating",0);
        map.put("timestamp",getCurrentTimestamp());
        map.put("destination", destination);
        if(pickupLatLng != null){
            map.put("location/from/lat",pickupLatLng.latitude );
            map.put("location/from/lng",pickupLatLng.longitude );
        }
        map.put("location/to/lat", destinationLatLng.latitude );
        map.put("location/to/lng", destinationLatLng.latitude );
        map.put("distance", rideDistance);

        historyRef.child(requestId).updateChildren(map);
        HashMap mapR = new HashMap();
        mapR.put("rideId",requestId);
        requestRef.child(customerId).updateChildren(mapR);

    }

    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return  timestamp;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) { //works when maps opens
        mMap = googleMap;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); //for changing location evry secound
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //the best accuracy phone can handle, but it requires a lot of battery


        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            }else{
                checkLocationPermission();
            }
        }
        /*if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) //which version are we using, version of phone of users, comparing to marchmello

        {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //checking if the premission is granded
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper()); //Looper.myLooper() - Return the Looper object associated with the current thread. Returns null if the calling thread is not associated with a Looper.
                googleMap.setMyLocationEnabled(true);

            }else {
                //Request Location Permission
                checkLocationPermission(); //asking for permission
            }
        }
        else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            googleMap.setMyLocationEnabled(true); //refreshment of location
        }*/

        }
    boolean cameraSet = false;
    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext()!=null){


                    if(!customerId.equals("") && mLastLocation!=null && location != null){
                        rideDistance += mLastLocation.distanceTo(location)/1000;
                    }
                    mLastLocation = location;

                    if (mCurrLocationMarker != null) {
                        mCurrLocationMarker.remove();
                    }


                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                    float zoomLevel = 15.0f; //This goes up to 21
                    if(!cameraSet) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
                        cameraSet = true;
                    }
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title("Konumunuz");
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)); //color of markers is hue magenta, ljubicasta
                    mCurrLocationMarker = mMap.addMarker(markerOptions);
                    //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    //mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
                    GeoFire geoFireAvailable = new GeoFire(refAvailable);
                    GeoFire geoFireWorking = new GeoFire(refWorking);

                    switch (customerId){
                        case "":
                            //if(clicked == true){
                                geoFireWorking.removeLocation(userId);
                                geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                                break;
                            //}
                        default:
                           if(clicked == true){
                            geoFireAvailable.removeLocation(userId);
                            geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                        }
                            if(clicked != true){
                                geoFireWorking.removeLocation(userId);
                                geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            }
                            break;
                    }
                }
            }
        }
    };

    /*LocationCallback mLocationCallback = new LocationCallback(){ //onLocationChanges
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                if(!customerId.equals("") && mLastLocation!=null && location != null){
                    rideDistance += mLastLocation.distanceTo(location)/1000;
                }
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Konumunuz");
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)); //color of markers is hue magenta, ljubicasta
                mCurrLocationMarker = mMap.addMarker(markerOptions);

                //move map camera
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
               // mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); //userid
                DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable"); //users id is stored in drivers avai;ab;e
                DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking"); //users id is stored in drivers avai;ab;e ..promenıla sam bılo je drıversworkıng

                GeoFire geoFireAvailable = new GeoFire(refAvailable); //saving to database as geofire
                GeoFire geoFireWorking = new GeoFire(refWorking);



                if(getApplicationContext() != null){ //if we are on the page
                switch (customerId){
                    case "": //no cueromer means that driver is available
                        geoFireWorking.removeLocation(userId); //remove driver from working
                        geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude())); //we store the postoion of available driver

                        //driver stops working and starts being available again
                        break;

                        default:
                            geoFireAvailable.removeLocation(userId);
                            geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude())); //we store the postoion of available driver, we store lan and lat to this users id



                            break;
                }
            }

            }

        }
    };*/
    //public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99; //dodato sa stacka, sto bas 99

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }
    /*private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //if there is no permission

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Izin ver").setMessage("Mesaj at") //for older devices
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        }).create().show();
                //place where user can give premission string manifest
                //request code is 1 to know when we check that it was right-true
            } else {
                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }
        }else {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION );
        }
    }*/

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            switch(requestCode){ //ako vec imamo premission,  tj ne ulazimo u aplikaciju drugi put
                case 1: //u videu je zamenio 1 MY_PERMISSIONS_REQUEST_LOCATION
                    if(grantResults.length<0 && grantResults[0]== PackageManager.PERMISSION_GRANTED)
                    {
                        //mapFragment.getMapAsync(this);
                        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                            mMap.setMyLocationEnabled(true);
                        }

                    }
                    else
                        Toast.makeText(getApplicationContext(), "Izin Vermelisiniz", Toast.LENGTH_SHORT).show();
                    //break;
                return; //pisalo u stacku umesto breaka

            }
        }



   private void connectDriver(){
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    private void disconnectDriver(){
        if(mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        //ostatak koda je isti
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); //adds in firebase driversAvailable
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable"); //users id is stored in drivers avai;ab;e

        GeoFire geoFire = new GeoFire(refAvailable);
        geoFire.removeLocation(userId); //when user goes out we are removing him from available list


    }

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    @Override
    public void onRoutingFailure(RouteException e) {

        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void erasePolylines(){

        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();

    }

}
