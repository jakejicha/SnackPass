package com.example.snackpass;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Product {

    //uniquely identifies a product
    String name;

    //holds all the timestamps of orders made on the product within 48 hour frame
    List<LocalDateTime> timestamps;

    //default constructor
    public Product(String name, List<LocalDateTime> timestamps){
        this.name = name;
        this.timestamps = timestamps;
    }

    public Product(String name){
        this.name = name;
        this.timestamps = new ArrayList<LocalDateTime>();
    }

    //returns the number of products ordered within 48 hour frame
    public int size(){
        return this.timestamps.size();
    }

    //returns the date of the latest order of this product
    public LocalDateTime latest(){
        return this.timestamps.get(0);
    }

    //adds a new timestamp to the list of timestamps
    public void addTimeStamp(LocalDateTime date, long amount){
        for(int i = 0; i < amount ; i++){
            this.timestamps.add(date);
        }
    }

    public void print(){
        System.out.println("Name: " + this.name + " | Size: " + this.size() + " | Latest: " + this.latest());
    }

    public String getName(){
        return this.name;
    }

    public String getMeta(){
        return "Count: " + size() + " ---- " + "Recent: " + latest();
    }
}
