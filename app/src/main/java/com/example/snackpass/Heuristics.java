package com.example.snackpass;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.snackpass.Product;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class Heuristics {
    //wrapper class for the Product Map. Performs all the heuristic calculations
    Map<String, Product> pMap;

    public Heuristics(Map<String, Product> pMap) {
        this.pMap = pMap;
    }

    //ouputs a score based natively on recency and amount
    @RequiresApi(api = Build.VERSION_CODES.O)
    public double nativeScore(String key) {
        Map<Integer, Integer> a = toPoints(key, this.pMap.get(recentHeuristicList().get(0)).latest());
        List<Integer> keyList = new ArrayList<Integer>(a.keySet());
        Collections.sort(keyList);
        double z = 0;
        for (int i : keyList) {
            z += expo(a.get(i), i);
        }
        //System.out.println(z);
        return z;
    }

    //maps a function to an exponential decay
    public double expo(int amount, int time) {
        return (1 * amount) * Math.pow(0.1, time);
    }

    //rounds everything to the nearest HOUR
    //(time, count) where time = hour away from time.now()
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Map<Integer, Integer> toPoints(String key, LocalDateTime curr) {
        Product p = this.pMap.get(key);
        Map<Integer, Integer> map = new HashMap<>();
        for (LocalDateTime t : p.timestamps) {
            long diff = ChronoUnit.HOURS.between(t, curr);
            //System.out.println(diff);
            map.put((int) diff, map.getOrDefault(diff, 0) + 1);
        }
        return map;
    }

    //heuristic sorting algorithms ---

    //sorts the items purely by their count
    public List<String> topHeuristicList() {
        List<String> list = new ArrayList<String>(this.pMap.keySet());
        Collections.sort(list, (a, b) -> count(b) - count(a));
        return list;
    }

    //sorts the items purely by their recency
    @RequiresApi(api = Build.VERSION_CODES.O)
    public List<String> recentHeuristicList() {
        List<String> list = new ArrayList<String>(this.pMap.keySet());
        Collections.sort(list, (a, b) -> (int) ChronoUnit.MINUTES.between(recent(a), recent(b)));
        return list;
    }

    //sorts the items through the native score (aka the area under the curve)
    @RequiresApi(api = Build.VERSION_CODES.O)
    public List<String> areaHeuristicList() {
        List<String> list = new ArrayList<String>(this.pMap.keySet());
        Collections.sort(list, (a, b) -> nativeScore(b) - nativeScore(a) > 0 ? 1 : -1);
        return list;
    }

    //sorts the items through their historic z-score comparisons
    @RequiresApi(api = Build.VERSION_CODES.O)
    public List<String> zScoreHeuristicList(Map<String,ZComp> mem, LocalDateTime t){
        List<String> list = new ArrayList<String>(this.pMap.keySet());
        int[] freq = frequency();
        Collections.sort(list, (a, b) -> zScore(b,freq,t,mem.get(b).mean,mem.get(b).var) - zScore(a,freq,t,mem.get(a).mean,mem.get(a).var) > 0 ? 1 : -1);
        return list;
    }

    //sorts the items through a composite combination of the areaScore + z-Score
    @RequiresApi(api = Build.VERSION_CODES.O)
    public List<String> compHeuristicList(Map<String, ZComp> mem, LocalDateTime t){
        List<String> list = new ArrayList<String>(this.pMap.keySet());
        int[] freq = frequency();
        Collections.sort(list, (a, b) ->
                ( zScore(b,freq,t,mem.get(b).mean,mem.get(b).var) * (0.5) + nativeScore(b) * 0.5) -
                ( zScore(a,freq,t,mem.get(a).mean,mem.get(a).var) * (0.5) + nativeScore(a) * 0.5)
                > 0 ? 1 : -1);

        return list;
    }

    //print function for the above algorithms
    public void print(List<String> lst) {
        for (String key : lst) {
            this.pMap.get(key).print();
        }
    }

    //score algorithms

    //counts all the occurrences of the item
    public int count(String key) {
        Product p = this.pMap.get(key);
        return p.size();
    }

    //returns the most recent occurrence of the item
    public LocalDateTime recent(String key) {
        Product p = this.pMap.get(key);
        return p.latest();
    }


    //returns the difference in ranking positions of the item
    public int rankedDiff(List<String> r1, List<String> r2, List<String> keySet) {
        int total = 0;
        for (String key : keySet) {
            total += Math.abs(r1.indexOf(key) - r2.indexOf(key));
        }
        return total;
    }

    public List<String> keySet() {
        return new ArrayList<String>(this.pMap.keySet());
    }

    //gets the current observed value to be used in the z score calculation
    @RequiresApi(api = Build.VERSION_CODES.O)
    public int getObValue(String key, int[] freq, LocalDateTime t){
        Product p = this.pMap.get(key);
        int y = mapToDay(t,freq);
        y = y == 0 ? 143 : y-1;
        //System.out.println(y);
        int count = 0;
        for(LocalDateTime ti : p.timestamps){
            if(mapToDay(ti,freq) == y){
                count++;
            }
        }
        return count;
    }

    //returns the Z-Score of the Observed Value
    @RequiresApi(api = Build.VERSION_CODES.O)
    public double zScore(String key, int[] freq, LocalDateTime t, double[] mean, double[] variance){
        int ob = getObValue(key,freq,t);
        int y = mapToDay(t, freq);
        y = y == 0 ? 143 : y - 1;

        if(Math.sqrt(variance[y]) == 0){
            return 0;
        }
        return (ob - mean[y]) / Math.sqrt(variance[y]);
    }

    //maps current time into corresponding "density unit" value
    @RequiresApi(api = Build.VERSION_CODES.O)
    public int mapToDay(LocalDateTime t, int[] freq){
        return freq[t.getHour() * 60 + t.getMinute()];
    }

    //arr that dictates the size of each bins for the observed values
    public int[] frequency() {
        int[] minutes = new int[1440];

        //uniform distribution for testing purposes
        int cur = 0;
        for(int i = 0; i <minutes.length; i++){
            minutes[i] = cur;
            if( (i + 1) % 10 == 0){
                cur++;
            }
        }

        return minutes;
    }

    //divides the timestamps into units of "days" using the frequency array.
    //List contains int[] which holds the # of orders per "density unit" of ALL products in each day
    @RequiresApi(api = Build.VERSION_CODES.O)
    public List<int[]> getDayList(String key, int[] freq) {
        Product p = this.pMap.get(key);
        List<int[]> dayList = new ArrayList<int[]>();

        int[] curr = new int[144];
        LocalDateTime prev = p.timestamps.get(0);

        for(LocalDateTime t : p.timestamps) {
            if(!sameDay(prev, t)){
                dayList.add(curr);
                curr = new int[144];
                prev = t;
            }
            curr[mapToDay(t, freq)]++;
        }
        dayList.add(curr);

        return dayList;
    }

    //helper function: finds the mean of the dayList
    public double[] dayMean(List<int[]> dayList){
        double[] sum = new double[dayList.get(0).length];
        for(int[] l : dayList){
            for(int i = 0 ; i < l.length; i++){
                sum[i] += l[i];
            }
        }
        for(int i = 0; i < sum.length; i++){
            sum[i] = sum[i] / dayList.size();
        }
        return sum;
    }

    //helper function: finds the variance of the dayList
    public double[] dayVariance(List<int[]> dayList, double[] dayMean){
        double[] sum = new double[dayMean.length];
        for(int[] l : dayList){
            for(int i = 0; i < l.length; i++){
                sum[i] += Math.pow((l[i] - dayMean[i]),2);
            }
        }
        for(int i = 0; i < sum.length; i++){
            sum[i] = sum[i] / dayList.size();
        }
        return sum;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean sameDay(LocalDateTime a, LocalDateTime b) {
        return a.getYear() == b.getYear() && a.getDayOfYear() == b.getDayOfYear();
    }

    //Composit function to create a ZScore Composition Mapping obj (ZComp)
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Map<String, ZComp> createMemory(List<String> keys){
        Map<String,ZComp> map = new HashMap<>();
        int[] freq = frequency();

        for(String key : keys){
            List<int[]> dayList = getDayList(key, freq);
            double[] dayMean = dayMean(dayList);
            double[] dayVar = dayVariance(dayList,dayMean);
            map.put(key, new ZComp(dayMean, dayVar));
        }

        return map;
    }

    //helper function to read the ZComp Memory from the JSONArraay
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Map<String,ZComp> readMemFromJSON(JSONArray arr){
        Map<String,ZComp> map = new HashMap<>();
        arr.forEach(product -> parseMem((JSONObject) product, map));
        return map;

    }

    //helper function for the readMemFromJSON function
    public static void parseMem(JSONObject obj, Map<String,ZComp> pMap){

        String key = (String) obj.get("name");
        JSONArray meanArr = (JSONArray) obj.get("mean");
        JSONArray varArr = (JSONArray) obj.get("var");
        Iterator m = meanArr.iterator();
        Iterator v = varArr.iterator();

        List<Double> mList = new ArrayList<>();
        List<Double> vList = new ArrayList<>();
        while(m.hasNext()){
            mList.add((double) m.next());
        }
        while(v.hasNext()){
            vList.add((double) v.next());
        }

        double[] mean = new double[mList.size()];
        for(int i = 0; i < mList.size(); i++){
            mean[i] = mList.get(i);
        }

        double[] var = new double[vList.size()];
        for(int i = 0; i < vList.size(); i++){
            var[i] = vList.get(i);
        }

        pMap.put(key, new ZComp(mean,var));
    }

}

