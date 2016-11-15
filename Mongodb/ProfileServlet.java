package cc.cmu.edu.minisite;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.JSONArray;

public class ProfileServlet extends HttpServlet {

    private static Connection conn;
    private static Statement stmt;

    public ProfileServlet() {
        /*
            Your initialization code goes here
        */
        // connect to mysql database
        try{
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection("jdbc:mysql://mydbinstance.c9neqpbucnp6.us-east-1.rds.amazonaws.com:3306/mytestdb","awsuser","19920522");
        }catch(SQLException sqe){
            System.out.println("Connection error!");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) 
            throws ServletException, IOException {
        JSONObject result = new JSONObject();

        String id = request.getParameter("id");
        String pwd = request.getParameter("pwd");

        /*
            Task 1:
            This query simulates the login process of a user, 
            and tests whether your backend system is functioning properly. 
            Your web application will receive a pair of UserID and Password, 
            and you need to check in your backend database to see if the 
	    UserID and Password is a valid pair. 
            You should construct your response accordingly:

            If YES, send back the user's Name and Profile Image URL.
            If NOT, set Name as "Unauthorized" and Profile Image URL as "#".
        */
        try{
            stmt = conn.createStatement();
            // query format
            String query = "select password from login_information where userId=" + id;
            ResultSet rs = stmt.executeQuery(query);
            String password = "";
            // get password
            while(rs.next()){
                password = rs.getString("password");
            }
            // if password is correct
            if(password.equals(pwd)){
                // select profile
                String query1 = "select name, imageUrl from user_profile where userId=" + id;
                ResultSet rs1 = stmt.executeQuery(query1);
                String name = "";
                String url = "";
                while(rs1.next()){
                    name = rs1.getString("name");
                    url = rs1.getString("imageUrl");
                }
                // construct the json object
                result.put("name", name);
                result.put("profile", url);
            }
            // else return
            else{
                String name = "Unauthorized";
                String url = "#";
                result.put("name", name);
                result.put("profile", url);
            }
            PrintWriter writer = response.getWriter();
            writer.write(String.format("returnRes(%s)", result.toString()));
            writer.close();
        }catch(SQLException sqe){
            sqe.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}
