package teamtest.undertow;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

// Mapper for query 3

public class Mapper {
    private static ArrayList<String> bannedWords = new ArrayList<String>();
    private static HashMap<String, Integer> scoreMap = new HashMap<String, Integer>();
    private static ArrayList<String> stopWords;
    private static HashMap<String, String> monthMap = new HashMap<String, String>();

    public static void main(String[] args) throws IOException {
        File stopFile = new File("/Users/Yujie/Desktop/test-server/src/stopwords.txt");
        File bannedFile = new File("/Users/Yujie/Desktop/test-server/src/banned.txt");

        try {
            Scanner scanner = new Scanner(stopFile);
            while (scanner.hasNextLine()) {
                String[] line = scanner.nextLine().split(",");
                stopWords = new ArrayList<String>(Arrays.asList(line));
            }

            scanner = new Scanner(bannedFile);
            while (scanner.hasNextLine()) {
                String newword = scanner.nextLine();
                String rottedword = "";
                for (int i = 0; i < newword.length(); i++) {
                    char c = newword.charAt(i);
                    if (!Character.isLetter(c)) {
                        rottedword += c;
                        continue;
                    } else if (c >= 'a' && c <= 'm') {
                        c += 13;
                    } else {
                        c -= 13;
                    }
                    rottedword += c;
                }
                bannedWords.add(rottedword);
            }

            monthMap.put("Jan", "01");
            monthMap.put("Feb", "02");
            monthMap.put("Mar", "03");
            monthMap.put("Apr", "04");
            monthMap.put("May", "05");
            monthMap.put("Jun", "06");
            monthMap.put("Jul", "07");
            monthMap.put("Aug", "08");
            monthMap.put("Sep", "09");
            monthMap.put("Oct", "10");
            monthMap.put("Nov", "11");
            monthMap.put("Dec", "12");

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }

        try {
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            String input;

            while ((input = br.readLine()) != null) {
                Gson gson = new Gson();
                try {
                    JsonObject jsonobj = gson.fromJson(input, JsonObject.class);
                    String lang = jsonobj.get("lang").getAsString();
                    if (!lang.equals("en")) {
                        continue;
                    }

                    String tweetid = jsonobj.get("id").getAsString();
                    String tweetidstr = jsonobj.get("id_str").getAsString();
                    SimpleDateFormat formt=new SimpleDateFormat();


                    if (tweetid.length() == 0 && tweetidstr.length() == 0) {
                        continue;
                    }

                    String createDate = jsonobj.get("created_at").getAsString();
                    if (createDate.length() == 0) {
                        continue;
                    }

                    String[] date = createDate.split(" ");
                    String realDate = date[5] + monthMap.get(date[1]) + date[2];
                    String tweetText = jsonobj.get("text").getAsString();
                    if (tweetText.length() == 0) {
                        continue;
                    }

                    tweetText = tweetText.replaceAll("(https?|ftp):\\/\\/[^\\s/$.?#][^\\s]*", "");

                    JsonObject entites = jsonobj.getAsJsonObject("entities");
                    if (entites.toString().equals("{}")) {
                        continue;
                    }

                    String userid = jsonobj.get("user").getAsJsonObject().get("id").getAsString();

                    String censored = doCensor(tweetText);

                    StringBuilder sb = new StringBuilder();

                    // output format: tweet_id \t user_id \t date \t text
                    sb.append(tweetid);
                    sb.append("\t");

                    sb.append(userid);
                    sb.append("\t");

                    sb.append(realDate);
                    sb.append("\t");

                    sb.append(censored);

                    System.out.println(sb.toString());

                } catch (NullPointerException e) {
                } catch (JsonSyntaxException e) {
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String doCensor(String text) {

        // remove stop words and banned words.
        String[] words = text.split("[^a-zA-Z0-9]");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (bannedWords.contains(words[i]) || stopWords.contains(words[i]) || words[i].equals("")) {
                continue;
            }
            sb.append(words[i]);
            sb.append(" ");
        }
        return sb.toString();
    }
}
