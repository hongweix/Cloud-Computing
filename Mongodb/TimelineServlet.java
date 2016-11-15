package cc.cmu.edu.minisite;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class TimelineServlet extends HttpServlet {
    public static Configuration conf;
    public static HTableInterface hTable;

    public static HConnection conn;
    public static Connection conn1;
    public static Statement stmt;
    private static MongoClient client;
    private static MongoDatabase db;

    public TimelineServlet() throws Exception {
        /*
            Your initialization code goes here
        */
        // connect to hbase
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
        // connect to mysql
        try{
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn1 = DriverManager.getConnection("jdbc:mysql://mydbinstance.c9neqpbucnp6.us-east-1.rds.amazonaws.com:3306/mytestdb","awsuser","19920522");         
        }catch(SQLException se){
            System.out.println("MySql connection error!");
        }catch(Exception e){
            e.printStackTrace();
        }
        // connect to mongodb
        client = new MongoClient("172.31.50.57", 27017);
        db = client.getDatabase("test");
    }

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException, IOException {

        JSONObject result = new JSONObject();
        String id = request.getParameter("id");

        /*
            Task 4 (1):
            Get the name and profile of the user as you did in Task 1
            Put them as fields in the result JSON object
        */
        try{
            stmt = conn1.createStatement();
            String query1 = "select name, imageUrl from user_profile where userId=" + id;
            ResultSet rs1 = stmt.executeQuery(query1);
            String name = "";
            String url = "";
            while(rs1.next()){
                name = rs1.getString("name");
                url = rs1.getString("imageUrl");
            }
            result.put("name", name);
            result.put("profile", url);
        }catch(SQLException sqe){
            sqe.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        /*
            Task 4 (2);
            Get the follower name and profiles as you did in Task 2
            Put them in the result JSON object as one array
        */
        JSONArray followers = new JSONArray();
        JSONArray followees = new JSONArray();
        Get g = new Get(id.getBytes());
        Result rs2 = hTable.get(g);
        String followerString = "";
        String followeeString = "";
        for(KeyValue kv: rs2.raw()){
            if(new String(kv.getQualifier()).equals("follower")){
                followerString = new String(kv.getValue());
            }
            else if(new String(kv.getQualifier()).equals("followee")){
                followeeString = new String(kv.getValue());
            }
        }
        List<field> list = new ArrayList<field>();
        try{
            String[] followerlist = followerString.split(" ");
            stmt = conn1.createStatement();
            for(String each : followerlist){
                String query = "select name, imageUrl from user_profile where userId=" + each;
                ResultSet rs3 = stmt.executeQuery(query);
                String name = "";
                String url = "";
                while(rs3.next()){
                    name = rs3.getString("name");
                    url = rs3.getString("imageUrl");
                }
                field f = new field(name, url);
                list.add(f);
            }
        }catch(SQLException sqe){
            sqe.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        Collections.sort(list);
        for(field f : list){
            JSONObject follower = new JSONObject();
            follower.put("name", f.name);
            follower.put("profile", f.url);
            followers.put(follower);
        }
        result.put("followers", followers);
        /*
            Task 4 (3):
            Get the 30 LATEST followee posts and put them in the
            result JSON object as one array.

            The posts should be sorted:
            First in ascending timestamp order
            Then numerically in ascending order by their PID (PostID) 
	    if there is a tie on timestamp
        */
        String[] followeelist = followeeString.split(" ");
        JSONArray array = new JSONArray();
        List<JSONObject> allpost = new ArrayList<JSONObject>();
        for(String each : followeelist){
            List<Document> list1 = db.getCollection("posts").find(new Document("uid",Integer.parseInt(each))).limit(30).sort(new Document("timestamp",1)).into(new ArrayList<Document>());
            for(Document dc : list1){
            JSONObject post = new JSONObject(dc.toJson());
            allpost.add(post);
            }
        }
        // sort first by timestamp, then by pid
        Collections.sort(allpost, new Comparator<JSONObject>(){
            @Override
            public int compare(JSONObject a, JSONObject b){
                if(a.get("timestamp").equals(b.get("timestamp"))){
                    return a.get("pid").toString().compareTo(b.get("pid").toString());
                }
                else{
                    return a.get("timestamp").toString().compareTo(b.get("timestamp").toString());
                }
            }
        });
        if(allpost.size()<=30){
            for(JSONObject jo : allpost){
                array.put(jo);
            }
        }
        else{
            for(int i = 0;i<allpost.size();i++){
                if(i>=allpost.size()-30){
                    array.put(allpost.get(i));
                }
            }
        }
        result.put("posts", array);
        PrintWriter out = response.getWriter();
        out.print(String.format("returnRes(%s)", result.toString()));
        out.close();
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
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

