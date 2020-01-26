package com.example.aida.newtaxi;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
// Add import statements for the new library.
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient; //updates
    SupportMapFragment mapFragment;
    //Marker mCurrLocationMarker;


    private Button mLogout, mRequest, mSettings, mHistory, mKapat;

    private LatLng  pickupLocation;
    private Boolean requestBol = false;
    private Marker pickupMarker;

    private String destination;
    private LatLng destinationLatLng, clickMarkerLatLng;

    private LinearLayout mDriverInfo;
    private ImageView mDriverProfileImage;
    private TextView mDriverName, mDriverPhone, mDriverCar;

    private RatingBar mRatingBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        destinationLatLng = new LatLng(0.0,0.0); //if there is no customer putting latlng somewhere in the ocean


        mDriverInfo = findViewById(R.id.driverInfo);
        mDriverName = findViewById(R.id.driverName);
        mDriverPhone = findViewById(R.id.driverPhone);
        mDriverCar = findViewById(R.id.driverCar);
        mDriverProfileImage = findViewById(R.id.driverProfileImage);

        mRatingBar = findViewById(R.id.ratingBar);

        mLogout = findViewById(R.id.logout);
        mRequest = findViewById(R.id.request);
        mSettings = findViewById(R.id.settings);
        mHistory = findViewById(R.id.history);
        mKapat = findViewById(R.id.close);

        mKapat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDriverInfo.setVisibility(View.GONE);
            }
        });

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut(); //firebase gives us options to sign out easily
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); //to close this activity
                return;
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestBol){ //when we cancel

                    endRide();

                }else{
                    requestBol = true;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pick Up"));//.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker)));

                    mRequest.setText("Müsait Taksici Arıyoruz");
                    sendRequestToDriver();

                   // getClosestDriver();

                }



            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CustomerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });
        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Customers");
                startActivity(intent);
                return;
            }
        });

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyC6mIQdANJdf4uBhiZ8pCV32AsEcH2irDw");
        }

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME,Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                destination = place.getName();
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
                Toast.makeText(getApplicationContext(),"hata destination", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private int radius = 1; //km
    private Boolean driverFound = false;
    private String driverFoundID;
    private String driverFoundID1;
    GeoQuery geoQuery;

    public void sendRequestToDriver(){
        //driverFoundID = key;
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID1).child("customerRequest");
        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        HashMap map = new HashMap(); //Hash table based implementation of the Map interface
        map.put("customerRideId", customerId);
        map.put("destination", destination);
        if(destinationLatLng!= null) {
            map.put("destinationLat", destinationLatLng.latitude);
            map.put("destinationLng", destinationLatLng.longitude);
        }
        driverRef.updateChildren(map);
        getDriverLocation();
        getHasRideEnded();
        mRequest.setText("Istek gönderildi");

    }

    private void getClosestDriver(){
        DatabaseReference  driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius); //searching for driver that are 1km far

        geoQuery.removeAllListeners(); //when drivers move not to show old position anymore
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) { //called when driver is found
                if(!driverFound && requestBol){ //bcz driver found is false when we start, first one we find we will choose

                    driverFound = true;

                    driverFoundID = key;
                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap(); //Hash table based implementation of the Map interface
                    map.put("customerRideId", customerId);
                    map.put("destination", destination);
                    if(destinationLatLng!= null) {
                        map.put("destinationLat", destinationLatLng.latitude);
                        map.put("destinationLng", destinationLatLng.longitude);
                    }
                    driverRef.updateChildren(map); //cutomer id will be inside drivers , and its name would be customerRideId


                    getDriverLocation();
                   // getDriverInfo();
                    getHasRideEnded();
                    mRequest.setText("Taksicinin Lokasyonu");
                    //dodato zbog radio group, key is drivers id
                    DatabaseReference mCustomerDataBase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    mCustomerDataBase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                                Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                                if(driverFound){
                                    return;
                                }

                                //if(driverMap.get("service").equals(requestServis)){
                                    driverFound = true;
                                    driverFoundID = dataSnapshot.getKey();



                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
                                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap(); //Hash table based implementation of the Map interface
                                    map.put("customerRideId", customerId);
                                    map.put("destination", destination);
                                    map.put("destinationLat", destinationLatLng.latitude);
                                    map.put("destinationLng", destinationLatLng.longitude);
                                    driverRef.updateChildren(map); //cutomer id will be inside drivers , and its name would be customerRideId





                                    getDriverLocation();
                                    //getDriverInfo();
                                    getHasRideEnded();

                                    mRequest.setText("Taksicinin Lokasyonu");

                                }
                            }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() { //ako nije pronasao vozaca
                if(!driverFound){
                    radius++; //zove taksije koje su 2km udaljena, tj dalje
                    getClosestDriver(); //and calls again it self
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationListener;
    private void getDriverLocation(){
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundID1).child("l"); //l bcz that how geofire stores value driversWorking or Available??
        driverLocationListener = driverLocationRef.addValueEventListener(new ValueEventListener() { //stavila driverFoundID1
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //every time location changes this function will be called
                if(dataSnapshot.exists() && requestBol){ //checks do we have driver on that locataion
                    List<Object> map = (List<Object>) dataSnapshot.getValue(); //puts everything from datasnapshot to list
                    double locationLat =0;
                    double locationLng = 0;
                    mRequest.setText("Taksici Bulundu");
                    if(map.get(0) != null){ //zato sto je latituda u firebasu 0
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){ //zato sto jelatituda u firebasu 1
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat,locationLng);
                    if(mDriverMarker != null) //ako vec postoji marker od pre prvo treba da ga ukloni
                    {
                        mDriverMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude); //distance between 2 locations

                    float distance = loc1.distanceTo(loc2);
                    if(distance<100){
                        mRequest.setText("Taksici Geldi");
                    }

                    else {
                        mRequest.setText("Taksici: " +String.valueOf(distance) + "uzaklıkta");

                    }
                            mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Taksiciniz").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_taxi)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }
    private void getDriverInfo(){
        mDriverInfo.setVisibility(View.VISIBLE);
        //mRequest.setVisibility(View.VISIBLE);
         DatabaseReference mDriverDatabase=FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID1);

        mDriverDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){ //makes sure that there is stg in database

                    Map<String,Object> map = (Map<String,Object>)dataSnapshot.getValue();
                    if(dataSnapshot.child("name").getValue()!=null){
                        mDriverName.setText("Adı:   " +dataSnapshot.child("name").getValue().toString());
                    }

                    if(dataSnapshot.child("phone").getValue()!=null){
                        mDriverPhone.setText("Telefon No:   " +dataSnapshot.child("phone").getValue().toString());
                    }
                    if(dataSnapshot.child("car").getValue()!=null){
                        mDriverCar.setText("Araba:   " +dataSnapshot.child("car").getValue().toString());
                    }
                    if(dataSnapshot.child("profileImageUrl").getValue()!=null){
                        Glide.with(getApplication()).load(dataSnapshot.child("profileImageUrl").getValue().toString()).into(mDriverProfileImage);

                    }
                    int ratingSum = 0;
                    float ratingsTotal = 0;
                    float ratingsAvg = 0;
                    for(DataSnapshot child : dataSnapshot.child("rating").getChildren()){
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingsTotal++;
                    }
                    if(ratingsTotal != 0){
                        ratingsAvg = ratingSum/ratingsTotal;
                        mRatingBar.setRating(ratingsAvg);
                    }
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private DatabaseReference driveHasEndedRef;
    private DatabaseReference csutomerRequestRef;
    private ValueEventListener driveHasEndedRefLIstener;
    private void getHasRideEnded() {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        csutomerRequestRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(userId);
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID1).child("customerRequest").child("customerRideId"); //finding a customer that called this user
        driveHasEndedRefLIstener = csutomerRequestRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) { //read data from a specific location, Returns true if the snapshot contains a non-null value.
                    Map<String,Object> map = (Map<String,Object>)dataSnapshot.getValue();
                    if(dataSnapshot.child("rideDurumu").getValue()!=null){
                        String durum = dataSnapshot.child("rideDurumu").getValue().toString();
                        if(durum.equals("red")){
                            Toast.makeText(getApplicationContext(),"Surucu Reddetti", Toast.LENGTH_SHORT).show();
                            mDriverInfo.setVisibility(View.GONE);
                            mRequest.setText("Ara");
                            rideRed();
                        }
                    }

                }
                else {
                    driveHasEndedRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) { //read data from a specific location, Returns true if the snapshot contains a non-null value.

                            }
                            else{
                                endRide(); //moze i stara verzija ako ne radi samo u else kontrolisati buton????
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    public void rideRed(){

        requestBol = false;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(userId);

        HashMap map = new HashMap(); //Hash table based implementation of the Map interface
        map.put("rideDurumu", null);
        assignedCustomerRef.updateChildren(map);

        //driveHasEndedRef.removeEventListener(driveHasEndedRefLIstener);
        if(driverFoundID1 != null){
            DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID1).child("customerRequest"); //removing customer id from drivers
            driverLocation.setValue(true); //it will rewrite child
            driverFoundID1=null;
        }
        if(mDriverMarker != null){
            mDriverMarker.remove();
        }

        driverFound = false;
        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.mipmap.ic_launcher);

    }
String rideId = ""; //ride id je uvek prazan
    private void endRide() {


        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); //geting users id
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference("historySingleForCustomer").child(userId);
        customerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) { //read data from a specific location, Returns true if the snapshot contains a non-null value.
                    Map<String,Object> map = (Map<String,Object>)dataSnapshot.getValue();
                    if(dataSnapshot.child("rideId").getValue()!=null){
                        rideId = dataSnapshot.child("rideId").getValue().toString();

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        /*customerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) { //makes sure that there is stg in database
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("rideId").toString() != null) {
                        rideId =  map.get("rideId").toString();;
                    }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });*/
        requestBol = false;

        if(geoQuery != null){
            geoQuery.removeAllListeners(); //when we cancel it will tell that to driver that
        }
        driverLocationRef.removeEventListener(driverLocationListener);
        if(driveHasEndedRef != null){
            driveHasEndedRef.removeEventListener(driveHasEndedRefLIstener);
        }
        if(driverFoundID1 != null){
            DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID1).child("customerRequest"); //removing customer id from drivers
            driverLocation.setValue(true); //it will rewrite child
            driverFoundID1=null;
        }
        driverFound = false;
        radius = 1;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId); //removing location od userid, customer from firebase
        if(pickupMarker != null){
            pickupMarker.remove(); //removing marker
        }
        if(mDriverMarker != null){
            mDriverMarker.remove();
        }

        mRequest.setText("Ara...");
        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.mipmap.ic_launcher);

        /*Intent intent = new Intent(getApplicationContext(), HistorySingleActivity.class);
        Bundle b = new Bundle();
        b.putString("rideId", rideId);
        intent.putExtras(b);
        startActivity(intent);*/

    }

    List<Marker> markerList1 = new ArrayList<Marker>();
    boolean doNotMoveCameraToCenterMarker = true;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    driverFoundID1 = marker.getTag().toString();
                    getDriverInfo();


                    return doNotMoveCameraToCenterMarker;
                }
            });

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); //for changing location evry secound
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                //googleMap.setMyLocationEnabled(true);

            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        googleMap.setMyLocationEnabled(true);

    }
    //Location location;
    boolean cameraSet = false;
    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext()!=null){
                    mLastLocation = location;

                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title("Konumunuz");
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    mMap.addMarker(markerOptions);

                    float zoomLevel = 15.0f; //This goes up to 21
                    if(!cameraSet) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
                        cameraSet = true;
                    }
                    if(!getDriversAroundStarted)
                        getDriversAround();
                }
            }
        }
    };

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 1: //u videu je zamenio 1
                if(grantResults.length<0 && grantResults[0]== PackageManager.PERMISSION_GRANTED)
                {
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
    boolean getDriversAroundStarted = false;
List<Marker> markerList = new ArrayList<Marker>(); //list of markers
private void getDriversAround(){
    getDriversAroundStarted = true;

        final DatabaseReference driversLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driversLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 1000);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) { //color green
                for(Marker markerIt : markerList1){ //all the markers that we have inside list
                    if(markerIt.getTag().equals(key)){
                        //not ot have the same markers
                        return;
                    }
                }
                LatLng driverLatLng = new LatLng(location.latitude, location.longitude);

                Marker mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title(key).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                mDriverMarker.setTag(key); //not good just to check id
                markerList1.add(mDriverMarker);

            }

            @Override
            public void onKeyExited(String key) { //when driver stops being available
                for(Marker markerIt : markerList1){ //all the markers that we have inside list
                    if(markerIt.getTag().equals(key)){ //
                        markerList1.remove(markerIt);
                        markerIt.remove(); //removing from the list
                        return;
                    }
                }

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) { //if driver moves, key yellow

                for(Marker markerIt : markerList1){ //all the markers that we have inside list
                    if(markerIt.getTag().equals(key)){ //not ot have the same markers
                        markerIt.setPosition(new LatLng(location.latitude, location.longitude)); //moving
                        //return;
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
}

}

