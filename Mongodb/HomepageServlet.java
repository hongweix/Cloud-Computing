package cc.cmu.edu.minisite;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.*;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class HomepageServlet extends HttpServlet {
    
    private static MongoClient client;
    private static MongoDatabase db;
    public HomepageServlet() {
        /*
            Your initialization code goes here
        */
        // connect to mongodb
        client = new MongoClient("172.31.50.57", 27017);
        db = client.getDatabase("test");
    }

    @Override
    protected void doGet(final HttpServletRequest request, 
            final HttpServletResponse response) throws ServletException, IOException {

        String id = request.getParameter("id");
        JSONObject result = new JSONObject();
        JSONArray array = new JSONArray();
        /*
            Task 3:
            Implement your logic to return all the posts authored by this user.
            Return this posts as-is, but be cautious with the order.

            You will need to sort the posts by Timestamp in ascending order
	     (from the oldest to the latest one). 
        */
        // get all posts sort by timestamp and store in list
        List<Document> list = db.getCollection("posts").find(new Document("uid",Integer.parseInt(id))).sort(new Document("timestamp",1)).into(new ArrayList<Document>());
        for(Document dc : list){
            JSONObject post = new JSONObject(dc.toJson());
            array.put(post);
        }
        result.put("posts", array);
        PrintWriter writer = response.getWriter();          
        writer.write(String.format("returnRes(%s)", result.toString()));
        writer.close();
    }

    @Override
    protected void doPost(final HttpServletRequest request, 
            final HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}

