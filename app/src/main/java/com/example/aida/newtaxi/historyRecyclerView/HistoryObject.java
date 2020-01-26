package com.example.aida.newtaxi.historyRecyclerView;

public class HistoryObject {

    private String rideId;
    private String destination;
    private String time;


    public HistoryObject(String rideId, String time, String destination){

        this.rideId = rideId;
        this.time = time;
        this.destination = destination;
    }

    public String getRideId(){ return rideId;}
    public void setRideId(String rideId){ this.rideId = rideId; }

    public String getDestination(){ return destination;}
    public void setDestination(String destination){ this.destination = destination; }

    public String getTime(){ return time;}
    public void setTime(String time){ this.time = time; }



}
