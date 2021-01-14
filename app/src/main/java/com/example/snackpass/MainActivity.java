package com.example.snackpass;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerViewAdapter recyclerViewAdapter;

    //Action button to trigger the simulation
    private Button simulate;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Load previous items_list
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Sample Product object for testing
        Product[] arr = new Product[1];
        arr[0] = new Product("Press the button repeatedly", Collections.singletonList(LocalDateTime.now()));

        //Load from previous trending list. Uncomment for use
//        if(preferences.contains("data")){
//            String json = preferences.getString("data", "");
//            Gson gson = new Gson();
//            arr = gson.fromJson(json, Product[].class);
//        }

        //play button for simulation
        simulate = findViewById(R.id.playButton);

        //setup the recyclerview adapter for infinite scroll
        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerViewAdapter = new RecyclerViewAdapter(arr);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setHasFixedSize(true);

        //firebase database configurations
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference();
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            //runs on start & on database updating
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    //read JSON from FIREBASE
                    Object object = snapshot.getValue(Object.class);
                    String JSON = new Gson().toJson(object);
                    JSONParser p = new JSONParser();
                    try {
                        //parse JSON into JAVA OBJECTS
                        JSONObject o = (JSONObject) p.parse(JSON);
                        //contains the heuristic memory for z-score calculation
                        JSONArray mem = (JSONArray) o.get("mem");
                        //contains the order information for the previous 48 hours
                        JSONArray log = (JSONArray) o.get("logs");

                        Map<String, Product> pMap = getProductMap(log);
                        Heuristics h = new Heuristics(pMap);
                        int[] freq = h.frequency();

                        //grabs the most recent time from the database
                        LocalDateTime t = h.pMap.get(h.recentHeuristicList().get(0)).latest();

                        //creates a list of product rankings (simulation purposes)
                        List<Product[]> plst = simulate(mem,log,t,recyclerViewAdapter);

                        Iterator i = plst.iterator();

                        //initializes an onclick-listener that forwards 10 minute in time (simulated)
                        simulate.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if(i.hasNext()){
                                    Product[] lst = (Product[]) i.next();
                                    recyclerViewAdapter.updateList(lst);

                                    //save to preferences
                                    SharedPreferences.Editor editor = preferences.edit();
                                    Gson gson = new Gson();
                                    editor.putString("data",gson.toJson(lst));
                                    editor.apply();
                                }
                            }
                        });


                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //simple function to parse the date of the given string
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static LocalDateTime formatDate(String date){
        int day = Integer.parseInt(date.substring(0, 2));
        int month = Integer.parseInt(date.substring(3, 5));
        int year = Integer.parseInt(date.substring(6, 10));

        int hour = -1;
        int minute = -1;

        if(date.length() == 16) {
            hour = Integer.parseInt(date.substring(11, 13));
            minute = Integer.parseInt(date.substring(14, 16));
        } else {
            hour = Integer.parseInt(date.substring(11,12));
            minute = Integer.parseInt(date.substring(13,15));
        }

        return LocalDateTime.of(year, month, day, hour, minute);
    }

    //creates a map of products given a JSONArray)
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Map<String, Product> getProductMap(JSONArray list){
        Map<String, Product> pMap = new HashMap<String, Product>();
        list.forEach(product -> parseProduct((JSONObject) product, pMap));

        return pMap;
    }

    //helper function to create Product object from the JSON file
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void parseProduct(JSONObject obj, Map<String,Product> pMap){

        long amount = (long) obj.get("Quantity");
        LocalDateTime date = formatDate((String) obj.get("Order Date"));
        String food = (String) obj.get("Item Name");

        //System.out.println(amount + " " + date + " " + food);

        //creates a new product class if it does not exist
        if(!pMap.containsKey(food)){
            pMap.put(food, new Product(food));
        }

        Product p = pMap.get(food);
        p.addTimeStamp(date, amount);
    }

    //debug printing method
    public static void print_pMap(Map<String, Product> pMap){
        for(String key : pMap.keySet()){
            Product p = pMap.get(key);
            System.out.println("Name: " + p.name + " Size: " + p.timestamps.size());
        }
    }

    //filters the JSONArray
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static JSONArray filter(JSONArray arr, LocalDateTime t){
        JSONArray list = new JSONArray();
        arr.forEach(product -> filterArray((JSONObject) product, list, t));
        return list;
    }

    //helper function for filter
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void filterArray(JSONObject product, JSONArray list, LocalDateTime t){
        double diff = ChronoUnit.HOURS.between(formatDate((String)product.get("Order Date")),t);
        if(diff > 0 && diff <= (24 * 1)){
            list.add(product);
        } else {
            return;
        }
    }

    //simulates progression of time onClick
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static List<Product[]> simulate(JSONArray mem, JSONArray log, LocalDateTime t, RecyclerViewAdapter r){
        List<Product[]> plst = new ArrayList<Product[]>();
        Map<String,ZComp> mmap = Heuristics.readMemFromJSON(mem);
        for(int i = 0; i < 50; i++){
            t = t.minusMinutes(10);
            JSONArray filtered = filter(log, t);
            Map<String, Product> fpMap = getProductMap(filtered);
            Heuristics fh = new Heuristics(fpMap);
            List<String> keys = fh.compHeuristicList(mmap, t);
            Product[] pr = new Product[keys.size()];
            for(int j = 0; j < pr.length; j++){
                pr[j] = fh.pMap.get(keys.get(j));
            }
            plst.add(pr);
        }
        return plst;
    }

}



