package com.example.aida.newtaxi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.firebase.geofire.GeoFire;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ServiceConfigurationError;

public class onAppKilled extends Service {


   // @androidx.annotation.Nullable
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); //adds in firebase driversAvailable
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable"); //users id is stored in drivers avai;ab;e

        GeoFire geoFire = new GeoFire(refAvailable);
        geoFire.removeLocation(userId);
    }
    //when we kill the app, it will remove driver from available
}
