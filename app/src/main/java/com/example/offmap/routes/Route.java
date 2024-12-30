package com.example.offmap.routes;

import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;
import java.util.List;


public class Route {
    private List<LatLong> points;
    private int estimatedTime;

    public Route() {
        this.points = new ArrayList<>();
        this.estimatedTime = 0;
    }

    public List<LatLong> getPoints(){
        return points;
    }
    public void setPoints(List<LatLong> points) {
        this.points = points;
    }

    public int getEstimatedTime (){
        return estimatedTime;
    }

    public void setEstimatedTime(int estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

}
