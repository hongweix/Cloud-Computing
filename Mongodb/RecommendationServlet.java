package cc.cmu.edu.minisite;

import java.io.IOException;
import java.io.PrintWriter;
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

import org.json.JSONObject;
import org.json.JSONArray;


public class RecommendationServlet extends HttpServlet {
	public static Configuration conf;
    public static HTableInterface hTable;

    public static HConnection conn;
    public static Connection conn1;
    public static Statement stmt;

	public RecommendationServlet () throws Exception {
        /*
        	Your initialization code goes here
         */
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

        try{
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn1 = DriverManager.getConnection("jdbc:mysql://mydbinstance.c9neqpbucnp6.us-east-1.rds.amazonaws.com:3306/mytestdb","awsuser","19920522");         
        }catch(SQLException se){
            System.out.println("MySql connection error!");
        }catch(Exception e){
            e.printStackTrace();
        }
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) 
			throws ServletException, IOException {

		JSONObject result = new JSONObject();
	    String id = request.getParameter("id");

		/**
		 * Bonus task:
		 * 
		 * Recommend at most 10 people to the given user with simple collaborative filtering.
		 * 
		 * Store your results in the result object in the following JSON format:
		 * recommendation: [
		 * 		{name:<name_1>, profile:<profile_1>}
		 * 		{name:<name_2>, profile:<profile_2>}
		 * 		{name:<name_3>, profile:<profile_3>}
		 * 		...
		 * 		{name:<name_10>, profile:<profile_10>}
		 * ]
		 * 
		 * Notice: make sure the input has no duplicate!
		 */
        // JSONArray recommendation = new JSONArray();
        // JSONObject recommended = new JSONObject();
        Get g = new Get(id.getBytes());
        Result rs2 = hTable.get(g);
        String followeeString1 = "";
        for(KeyValue kv: rs2.raw()){
        	if(new String(kv.getQualifier()).equals("followee")){
                followeeString1 = new String(kv.getValue());
            }
        }
        String[] firstfollowee = followeeString1.split(" ");
        HashMap<Integer, Integer> secondfollowee = new HashMap<Integer, Integer>();
        String folleeString2 = "";
        for(String eachid : firstfollowee){
            Get g1 = new Get(id.getBytes());
            Result rs3 = hTable.get(g1);
            String followeeString2 = "";
            for(KeyValue kv: rs3.raw()){
            	if(new String(kv.getQualifier()).equals("followee")){
                followeeString2 = new String(kv.getValue());
                }
            }
            String[] followeelist = followeeString2.split(" ");
            for(String each : followeelist){
                int eachin = Integer.parseInt(each);
                if(!each.equals(id)){
                	if(secondfollowee.containsKey(eachin)){
                       int num = 1 + secondfollowee.get(eachin);
                       secondfollowee.put(eachin, num);
                	}
                	else{
                       secondfollowee.put(eachin, 1);
                	}
                }
            }
        }
        List list1 = new LinkedList(secondfollowee.entrySet());
        Collections.sort(list1, new Comparator(){
    	    @Override
    	    public int compare(Object o1, Object o2){
    		   if(((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue())==0){
    			   return -((Comparable) ((Map.Entry) (o2)).getKey()).compareTo(((Map.Entry) (o1)).getKey());
    		   }
    		   return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
    	    }
        });
        Map result1 = new LinkedHashMap();
        for (Iterator it = list1.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result1.put(entry.getKey(), entry.getValue());
        }
        Map<Integer, Integer> sortedMap = result1;
        JSONArray topfollowees = new JSONArray();
        int n = 1;
        try{
        stmt = conn1.createStatement();
        	for(Integer key: sortedMap.keySet()){
        		if(n<=10){
                   String query = "select name, imageUrl from user_profile where userId=" + key;
                   ResultSet rs = stmt.executeQuery(query);
                   String name = "";
                   String url = "";
                   while(rs.next()){
                     name = rs.getString("name");
                     url = rs.getString("imageUrl");
                   }
                   JSONObject topfollowee = new JSONObject();
                   topfollowee.put("name", name);
                   topfollowee.put("profile", url);
                   topfollowees.put(topfollowee);
        		}
        		else{
        			break;
        		}
        		n++;
        	}
        result.put("recommendation", topfollowees);
        }catch(SQLException sqe){
            sqe.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        PrintWriter writer = response.getWriter();
        writer.write(String.format("returnRes(%s)", result.toString()));
        writer.close();

	}

	@Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}

