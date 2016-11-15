package cc.cmu.edu.minisite;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.JSONArray;

import java.sql.*;


public class FollowerServlet extends HttpServlet {
    public static Configuration conf;
    public static HTableInterface hTable;

    public static HConnection conn;
    public static Connection conn1;
    public static Statement stmt;

    public FollowerServlet() {
        /*
            Your initialization code goes here
        */
        // connect to hbase database
        conf = HBaseConfiguration.create();
        conf.set("hbase.master", "172.31.61.208:60000");
        conf.set("hbase.zookeeper.quorum", "172.31.61.208");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        try{
            conn = HConnectionManager.createConnection(conf);
            hTable = conn.getTable(Bytes.toBytes("follow"));
        }catch(Exception e){
            e.printStackTrace();
        }
        
        // connect to mysql database
        try{
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn1 = DriverManager.getConnection("jdbc:mysql://mydbinstance.c9neqpbucnp6.us-east-1.rds.amazonaws.com:3306/mytestdb","awsuser","19920522");         
        }catch(SQLException se){
            System.out.println("MySql connection error!");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("id");
        JSONObject result = new JSONObject();
        JSONArray followers = new JSONArray();

        /*
            Task 2:
            Implement your logic to retrive the followers of this user. 
            You need to send back the Name and Profile Image URL of his/her Followers.

            You should sort the followers alphabetically in ascending order by Name. 
            If there is a tie in the followers name, 
	    sort alphabetically by their Profile Image URL in ascending order. 
        */
        // search by raw()
        Get g = new Get(id.getBytes());
        Result rs = hTable.get(g);
        String followerString = "";
        for(KeyValue kv: rs.raw()){
            if(new String(kv.getQualifier()).equals("follower")){
                followerString = new String(kv.getValue());
            }
        }
        List<field> list = new ArrayList<field>();
        try{
             String[] followerlist = followerString.split(" ");
             stmt = conn1.createStatement();
             // search each follower's profile
             for(String each : followerlist){
                String query = "select name, imageUrl from user_profile where userId=" + each;
                ResultSet rs1 = stmt.executeQuery(query);
                String name = "";
                String url = "";
                while(rs1.next()){
                    name = rs1.getString("name");
                    url = rs1.getString("imageUrl");
                }
                field f = new field(name, url);
                list.add(f);
             }
        }catch(SQLException sqe){
            sqe.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        // sort list
        Collections.sort(list);
        for(field f : list){
            JSONObject follower = new JSONObject();
            follower.put("name", f.name);
            follower.put("profile", f.url);
            followers.put(follower);
        }
        result.put("followers", followers);
        PrintWriter writer = response.getWriter();
        writer.write(String.format("returnRes(%s)", result.toString()));
        writer.close();
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    class field implements Comparable<field>{
        public String name;
        public String url;
        public field(String name, String url){
            this.name = name;
            this.url = url;
        }
        @Override
        public int compareTo(field f){
            if(this.name.equals(f.name)){
                return this.url.compareTo(f.url);
            }
            else{
                return this.name.compareTo(f.name);
            }
        }
    }
    
}


