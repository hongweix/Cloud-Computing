package team.undertow;

import io.undertow.Undertow;
import io.undertow.server.*;
import io.undertow.util.Headers;

import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.sql.*;
import java.beans.PropertyVetoException;
import java.util.TimeZone;

public class App {

//    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
//    private static final String DB_NAME = "twitter";
//    private static final String URL = "jdbc:mysql://localhost/" + DB_NAME;
//    private static final String DB_USER = "root";
//    private static final String DB_PWD = "wuroubuhuan";

    private static Connection conn;

    private static void initializeConnection() throws ClassNotFoundException, SQLException, IOException, PropertyVetoException {

//        Class.forName(JDBC_DRIVER);
//        conn = DriverManager.getConnection(URL, DB_USER, DB_PWD);

        //connection pool
        conn = DataSource.getInstance().getConnection();
    }

    public static void main(String[] args) {
        try {
            initializeConnection();

            Undertow server = Undertow.builder()
                    .setIoThreads(16)
                    .addHttpListener(80, "0.0.0.0")
                    .setHandler(new HttpHandler() {
                        public void handleRequest(final HttpServerExchange exchange) throws Exception {
                            if (exchange.isInIoThread()) {
                                exchange.dispatch(this);
                                return;
                            }

                            String type=exchange.getQueryString();
                            if(type==null)
                            {
                                return;
                            }

                            Map<String, Deque<String>> map = exchange.getQueryParameters();

                            if(type.contains("key="))
                            {
                                String key=map.get("key").getFirst();
                                String message=map.get("message").getFirst();
                                String decrypted = decrypt(key, message);
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                                String time = dateFormat.format(new Date());
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                                exchange.getResponseSender().send("Not_Happy_Without_Meat,735643720214\n"+time+"\n"+decrypted+"\n");
                            }
                            else if(type.contains("hashtag=")){
                                String userId = map.get("userid").getFirst();
                                String hashTag = map.get("hashtag").getFirst();
                                // get response and send it out
                                StringBuilder sb = new StringBuilder();
                                sb.append("Not_Happy_Without_Meat,735643720214\n");
                                accessDB2(sb, userId, hashTag);
                                sb.append("\n");
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                                exchange.getResponseSender().send(sb.toString());
                            }
                            else if(type.contains("start_date="))
                            {
                                String start_date = map.get("start_date").getFirst();
                                start_date = start_date.substring(0, 4) + start_date.substring(5, 7) + start_date.substring(8);

                                String end_date = map.get("end_date").getFirst();
                                end_date = end_date.substring(0, 4) + end_date.substring(5, 7) + end_date.substring(8);

                                String start_userid = map.get("start_userid").getFirst();
                                String end_userid = map.get("end_userid").getFirst();
                                String words = map.get("words").getFirst();

                                StringBuilder sb = new StringBuilder();
                                sb.append("Not_Happy_Without_Meat,735643720214\n");
                                accessDB3(sb, start_date, end_date, start_userid, end_userid, words);
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                                exchange.getResponseSender().send(sb.toString());
                            }
                        }
                    }).build();

            server.start();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    public static String decrypt(String key, String message){
        int []dx={0,1,0,-1};
        int []dy={1,0,-1,0};
        BigInteger X=new BigInteger("64266330917908644872330635228106713310880186591609208114244758680898150367880703152525200743234420230");
        BigInteger Y=new BigInteger(key);
        BigInteger Z=X.gcd(Y);

        int K = 1 + (Z.intValue() % 25);

        int size=(int) Math.sqrt(message.length());
        char[] result=new char[message.length()];

        int index=size-1;
        int x=0;
        int y=size-1;
        int d=1;

        for(int i=0;i<size;i++)
        {
            result[i]=(char) ((message.charAt(i) - 'A' - K + 26) % 26 + 'A');
        }

        for (int i = size - 1; i > 0; i--) {
            for (int k = 0; k < i; k++) {
                index = index + 1;
                x = x + dx[d];
                y = y + dy[d];
                result[index] = (char) ((message.charAt(x * size + y) - 'A' - K + 26) % 26 + 'A');
            }
            d = (d + 1) % 4;

            for (int k = 0; k < i; k++) {
                index = index + 1;
                x = x + dx[d];
                y = y + dy[d];
                result[index] = (char) ((message.charAt(x * size + y) - 'A' - K + 26) % 26 + 'A');
            }
            d = (d + 1) % 4;
        }
        return new String(result);
    }


    public static void accessDB2(StringBuilder sb, String userId, String hashTag) {

        Statement stmt;
        try {
            stmt = conn.createStatement();
            //MySQL query
            String sql = "SELECT density,time,tweet_id,censored,hashtag FROM tweets WHERE user_id=" + userId;
            ResultSet result = stmt.executeQuery(sql);
            boolean flag;

            while (result.next()) {
                flag = true;
                String[] parts = result.getString("hashtag").split("#");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals(hashTag))
                    {
                        flag = false;
                        break;
                    }
                }
                if (flag)
                {
                    continue;
                }
                // generate result
                sb.append(result.getString("density"));
                sb.append(":");
                sb.append(result.getString("time"));
                sb.append(":");
                sb.append(result.getString("tweet_id"));
                sb.append(":");
                sb.append(result.getString("censored"));
                sb.append("\n");
            }
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void accessDB3(StringBuilder sb, String start_date, String end_date, String start_userid, String end_userid, String words) {

        try {
            Statement stmt = conn.createStatement();

            String sql = "SELECT words FROM q3 WHERE (user_id BETWEEN " + start_userid + " AND " + end_userid + ") AND (date BETWEEN " + start_date + " AND " + end_date + ")";

            ResultSet result = stmt.executeQuery(sql);
            String[] parts = words.split(",");

            Map<String, Integer> map = new HashMap<String, Integer>();
            map.put(parts[0],0);
            map.put(parts[1],0);
            map.put(parts[2],0);

            while (result.next())
            {
                String line = " " +result.getString("words").toLowerCase();
                for (int i = 0; i < 3; i++)
                {
                    int index = line.indexOf(" "+parts[i] + ":");
                    while (index != -1)
                    {
                        int startIndex = index + parts[i].length() + 2;
                        int endIndex = line.indexOf(" ", startIndex);
                        int count = Integer.parseInt((line.substring(startIndex, endIndex)));
                        if (!map.containsKey(parts[i]))
                        {
                            map.put(parts[i], count);
                        }
                        else
                        {
                            map.put(parts[i], map.get(parts[i]) + count);
                        }
                        index = line.indexOf(" " + parts[i] + ":", endIndex);
                    }
                }
            }

            sb.append(parts[0] + ":" + map.get(parts[0]) + "\n");
            sb.append(parts[1] + ":" + map.get(parts[1]) + "\n");
            sb.append(parts[2] + ":" + map.get(parts[2]) + "\n");
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
