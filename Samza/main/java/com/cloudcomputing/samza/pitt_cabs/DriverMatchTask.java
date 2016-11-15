package com.cloudcomputing.samza.pitt_cabs;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.util.HashMap;

import java.lang.Math;

import org.apache.samza.config.Config;
import org.apache.samza.storage.kv.KeyValueIterator;
import org.apache.samza.storage.kv.KeyValueStore;
import org.apache.samza.system.IncomingMessageEnvelope;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.task.InitableTask;
import org.apache.samza.task.MessageCollector;
import org.apache.samza.task.StreamTask;
import org.apache.samza.task.TaskContext;
import org.apache.samza.task.TaskCoordinator;
import org.apache.samza.task.WindowableTask;
import org.apache.samza.storage.kv.Entry;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 * Consumes the stream of driver location updates and rider cab requests.
 * Outputs a stream which joins these 2 streams and gives a stream of rider
 * to driver matches.
 */
public class DriverMatchTask implements StreamTask, InitableTask, WindowableTask {

  //key of this store is driver id and value is inforamtion of this driver
  private KeyValueStore<String, String> driverList;
  private KeyValueStore<String, String> driverLoc;
  private KeyValueStore<String, String> driverNum;

  /* Define per task state here. (kv stores etc) */
  @Override
  @SuppressWarnings("unchecked")
  public void init(Config config, TaskContext context) throws Exception {
    driverLoc = (KeyValueStore<String, String>)context.getStore("driver-loc");
    driverList = (KeyValueStore<String, String>)context.getStore("driver-list");
    driverNum = (KeyValueStore<String, String>)context.getStore("driver-num");
  }

  @Override
  @SuppressWarnings("unchecked")
  public void process(IncomingMessageEnvelope envelope, MessageCollector collector, TaskCoordinator coordinator) {
    // The main part of your code. Remember that all the messages for a particular partition
    // come here (somewhat like MapReduce). So for task 1 messages for a blockId will arrive
    // at one task only, thereby enabling you to do stateful stream processing.
    String incomingStream = envelope.getSystemStreamPartition().getStream();
    if (incomingStream.equals(DriverMatchConfig.EVENT_STREAM.getStream())) {
      processEvent((Map<String, Object>)envelope.getMessage(), collector);
    } else if (incomingStream.equals(DriverMatchConfig.DRIVER_LOC_STREAM.getStream())) {
      processDriverLoc((Map<String, Object>)envelope.getMessage());
    } else {
      throw new IllegalStateException("Unexpected input stream: " + envelope.getSystemStreamPartition());
    }
  }

  private void processEvent(Map<String, Object> message, MessageCollector collector) {
    if (message.get("type").equals("DRIVER_LOCATION")) {
      throw new IllegalStateException("Unexpected event: " + message.get("type"));
    }
    
    if (message.get("type").equals("RIDE_REQUEST")) {
      processRequest(message, collector);
    } else if (message.get("type").equals("RIDE_COMPLETE")) {
      processComplete(message);
    } else if (message.get("type").equals("ENTERING_BLOCK")) {
      processEntering(message);
    } else {
      processLeaving(message);
    }
  }
  
  private void processRequest(Map<String, Object> message, MessageCollector collector){
      //get client info
      String blockId = Integer.toString((int)message.get("blockId"));
      String clientId = Integer.toString((int)message.get("clientId"));
      int client_lat = (int)message.get("latitude");
      int client_long = (int)message.get("longitude");
      String genderPrefer = (String)message.get("gender_preference");
      //get iterator to iterate through whole map
      KeyValueIterator<String, String> alldriver = driverList.all();
      double max = 0.0;
      String selectedDriver = "";
      //count the available driver in this block
      int num = 0;
      try {
        while (alldriver.hasNext()) {
          Entry<String, String> driver = alldriver.next();
          String block = driver.getKey().split(":")[0];        
          if(blockId.equals(block)){
            String[] driverinfo = driver.getValue().split("#");
            String[] location = driverLoc.get(driver.getKey()).split("#");
            //get driver information
            String gender = driverinfo[0];
            String rating = driverinfo[1];
            String salary = driverinfo[2];
            //get driver location
            double latitude = Double.parseDouble(location[0]);
            double longtitude = Double.parseDouble(location[1]);
            //calculate distance
            double distance = Math.sqrt(Math.pow((client_long - longtitude), 2) + Math.pow((client_lat - latitude) , 2));
            //gender_score
            double gender_score = 0.0;
            // if gender is not null and gender preference is null or equals to driver's gender, get gender_score 1
            if (gender != null && genderPrefer.equals("N")) {
              gender_score = 1.0;
            } else if (gender != null && genderPrefer.equals(gender)) {
              gender_score = 1.0;
            }
            //weight these score to standard value
            double dist_score = 1 - distance / (500 * Math.sqrt(2));
            double rating_score = Double.parseDouble(rating) / 5.0;
            double salary_score = 1 - Double.parseDouble(salary) / 100.0;
            double match_score = dist_score * 0.4 + gender_score * 0.2 + rating_score * 0.2 + salary_score * 0.2;
            //find the max match_score and related driver key
            if (match_score > max) {
             selectedDriver = driver.getKey();
             max = match_score;
             }
          }
          num++;
        }
        //if exists, output the message
        if (!selectedDriver.equals("")) {
          // initiate SPF
          double SPF = 1.0;
          String averageDri = driverNum.get(blockId);
          // if averageDri is null, put it is the driverNum map
          if(averageDri == null){
            driverNum.put(blockId, num + "");
          }
          // else
          else{
            // split averageDri by ":" and get each request time's number of driver
            String[] eachReqNum = averageDri.split(":");
            // if the eachReqNum list is smaller than 4, then add the current num in the list
            // and put back in driverNum map
            if(eachReqNum.length < 4){
              averageDri += ":" + num;
              driverNum.put(blockId, averageDri);
            }
            // else
            else{
              // sum the past 5 request num
              int sum = num;
              for(int i = 1;i<=4;i++){
                sum += Integer.parseInt(eachReqNum[eachReqNum.length-i]);
              }
              // average the sum get A
              double A = sum / 5.0;
              // if A is less than 3.6, calculate as indicated in writeup
              if(A < 3.6){
                double SF = (4 * (3.6 - A)/(1.8 - 1));
                SPF = 1.0 + SF;
              }
              // add the current num in the list and put back in driverNum map
              averageDri += ":" + num;
              driverNum.put(blockId, averageDri); 
            }
          }
          Map<String, Object> output = new HashMap<String, Object>();
          output.put("clientId", clientId);
          output.put("driverId", selectedDriver.split(":")[1]);
          output.put("priceFactor", Double.toString(SPF));
          driverLoc.delete(selectedDriver);
          driverList.delete(selectedDriver);
          collector.send(new OutgoingMessageEnvelope(DriverMatchConfig.MATCH_STREAM, output));
        }
      } finally {
        alldriver.close();
      }
  }
  
  private void processComplete(Map<String, Object> message){
        String blockId = Integer.toString((int)message.get("blockId"));
        String driverId = Integer.toString((int)message.get("driverId"));
        //update available driver list info when entering
        driverList.put(blockId + ":" + driverId, (String)message.get("gender") + "#" + Double.toString((double)message.get("rating")) + "#" + Integer.toString((int)message.get("salary")));
        //update avaible driver location info when entering
        driverLoc.put(blockId + ":" + driverId, Integer.toString((int)message.get("latitude")) + "#" + Integer.toString((int)message.get("longitude")));
  }

  private void processEntering(Map<String, Object> message){
    if (((String)message.get("status")).equals("AVAILABLE")) {
        String blockId = Integer.toString((int)message.get("blockId"));
        String driverId = Integer.toString((int)message.get("driverId"));
        //update available driver list info when entering
        driverList.put(blockId + ":" + driverId, (String)message.get("gender") + "#" + Double.toString((double)message.get("rating")) + "#" + Integer.toString((int)message.get("salary")));
        //update avaible driver location info when entering
        driverLoc.put(blockId + ":" + driverId, Integer.toString((int)message.get("latitude")) + "#" + Integer.toString((int)message.get("longitude")));
      }
  }

  private void processLeaving(Map<String, Object> message){
    //remove driver from driver location and list when leaving
      String blockId = Integer.toString((int)message.get("blockId"));
      String driverId = Integer.toString((int)message.get("driverId"));
      if (driverLoc.get(blockId + ":" + driverId) != null) {
        driverLoc.delete(blockId + ":" + driverId);
      }
      if (driverList.get(blockId + ":" + driverId) != null) {
        driverList.delete(blockId + ":" + driverId);
      }
  }

  private void processDriverLoc(Map<String, Object> message) {
    if (!message.get("type").equals("DRIVER_LOCATION")) {
      throw new IllegalStateException("Unexpected location " + message.get("type"));
    }
    String blockId = Integer.toString((int)message.get("blockId"));
    String driverId = Integer.toString((int)message.get("driverId"));
    String latitude = Integer.toString((int)message.get("latitude"));
    String longtitude = Integer.toString((int)message.get("longitude"));
    driverLoc.put(blockId + ":" + driverId, latitude + "#" + longtitude);
  }

  @Override
  public void window(MessageCollector collector, TaskCoordinator coordinator) {
    //this function is called at regular intervals, not required for this project
  }
}
