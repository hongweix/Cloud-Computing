package team;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Mapper {
    private static ArrayList<String> bannedWords = new ArrayList<String>();
    private static HashMap<String, Integer> scoreMap = new HashMap<String, Integer>();
    private static ArrayList<String> stopWords;
    private static HashMap<String, Integer> monthMap = new HashMap<String, Integer>();

    public static void main(String[] args) throws UnsupportedEncodingException {
        Mapper temp = new Mapper();

        InputStream afinnFile = temp.getClass().getResourceAsStream("/afinn.txt");
        InputStream stopFile = temp.getClass().getResourceAsStream("/stopwords.txt");
        InputStream bannedFile = temp.getClass().getResourceAsStream("/banned.txt");
        Scanner scanner = new Scanner(afinnFile);
        while (scanner.hasNextLine()) {
            String[] line = scanner.nextLine().split("\t");
            scoreMap.put(line[0], Integer.parseInt(line[1]));
        }

        scanner = new Scanner(stopFile);
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
        monthMap.put("Jan", 0);
        monthMap.put("Feb", 1);
        monthMap.put("Mar", 2);
        monthMap.put("Apr", 3);
        monthMap.put("May", 4);
        monthMap.put("Jun", 5);
        monthMap.put("Jul", 6);
        monthMap.put("Aug", 7);
        monthMap.put("Sep", 8);
        monthMap.put("Oct", 9);
        monthMap.put("Nov", 10);
        monthMap.put("Dec", 11);

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF8"));//new FileInputStream(new File("/Users/Yujie/Desktop/dataset0"))
            String input;

            while ((input = br.readLine()) != null) {
                Gson gson = new Gson();
                try {
                    JsonObject jsonobj = gson.fromJson(input, JsonObject.class);

                    String tweetid = jsonobj.get("id").getAsString();
                    String tweetidstr = jsonobj.get("id_str").getAsString();

                    if (tweetid.length() == 0 && tweetidstr.length() == 0) {
                        continue;
                    }

                    String createDate = jsonobj.get("created_at").getAsString();
                    if (createDate.length() == 0) {
                        continue;
                    }
                    String[] date = createDate.split(" ");
                    String[] time = date[3].split(":");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
                    Date d = new Date(Integer.parseInt(date[5]) - 1900, monthMap.get(date[1]), Integer.parseInt(date[2]), Integer.parseInt(time[0]), Integer.parseInt(time[1]), Integer.parseInt(time[2]));
                    String datetime = sdf.format(d);

                    String tweetText = jsonobj.get("text").getAsString();
                    if (tweetText.length() == 0) {
                        continue;
                    }

                    JsonObject entites = jsonobj.getAsJsonObject("entities");
                    if (entites.toString().equals("{}")) {
                        continue;
                    }

                    JsonArray hashtags = entites.getAsJsonArray("hashtags");

                    if (hashtags.size() == 0)
                        continue;

                    ArrayList<String> hts = new ArrayList<String>();
                    Iterator it = hashtags.iterator();


                    while (it.hasNext()) {
                        JsonObject jp = (JsonObject) it.next();
                        hts.add(jp.get("text").getAsString());
                    }

                    String userid = jsonobj.get("user").getAsJsonObject().get("id").getAsString();

                    String density = calculateDensity(tweetText);

                    String censored = doCensor(tweetText);

                    censored = censored.replaceAll("\\\\", "\\\\\\\\");
                    censored = censored.replaceAll("\t", "\\\\t");
                    censored = censored.replaceAll("\n", "\\\\n");
                    censored = censored.replaceAll("\r", "\\\\r");
                    censored = censored.replaceAll("\"", "\\\\\"");

                    StringBuilder sb = new StringBuilder();
                    sb.append(tweetid);
                    sb.append("\t");
                    sb.append(userid);
                    sb.append("\t");
                    sb.append(datetime);
                    sb.append("\t");
                    sb.append(density);
                    sb.append("\t");
                    sb.append(censored);
                    sb.append("\t");
                    for (String element : hts) {
                        sb.append(element);
                        sb.append("#");
                    }
                    sb.deleteCharAt(sb.length() - 1);
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

    public static String calculateDensity(String text) {
        //text split
        int length = 0;
        String[] words = text.split("[^a-zA-Z0-9]");
        //sentiment score
        double score = 0;
        //effective word count
        double ewc = 0;

        for (int i = 0; i < words.length; i++) {
            if (!words[i].matches("[a-zA-Z0-9]+"))
                continue;
            length++;
            String low = words[i].toLowerCase();
            if (scoreMap.containsKey(low)) {
                score += scoreMap.get(low);
            }

            if (stopWords.contains(low)) {
                ewc++;
            }
        }

        ewc = length - ewc;

        //calculation
        if (ewc == 0)
            return "0.000";
        DecimalFormat df = new DecimalFormat("#0.000");
        df.setRoundingMode(RoundingMode.HALF_UP);
        Double temp = score / ewc;
        return df.format(temp);

    }

    public static String doCensor(String text) {
        //Method 1
//        text = "ta muito dificil de acordar nesse frio \\ud83d\\ude13\\ud83d\\udc94\\u2744\\ufe0f";
//        text += "a";
//        String[] words = text.split("[^a-zA-Z0-9]");
//        String censoredtext = "";
//        for (int i = 0; i < words.length; i++) {
//            if (bannedWords.contains(words[i].toLowerCase())) {
//                char begin = words[i].charAt(0);
//                char end = words[i].charAt(words[i].length() - 1);
//                String asterisks = "";
//                for (int j = 1; j < words[i].length() - 1; j++) {
//                    asterisks += "*";
//                }
//                words[i] = begin + asterisks + end;
//            }
//            censoredtext += (words[i] + " ");
//        }
//        censoredtext = censoredtext.substring(0, censoredtext.length() - 1);
//        String returntext = "";
//        char[] original = text.toCharArray();
//        char[] censored = censoredtext.toCharArray();
//        try {
//
//            for (int i = 0; i < original.length - 1; i++) {
//                if (censored[i] == '*') {
//                    returntext += censored[i];
//                } else {
//                    returntext += original[i];
//                }
//            }
//        } catch (ArrayIndexOutOfBoundsException e) {
//
//
////            try {
//////                PrintStream ps = new PrintStream(System.out, true, "UTF-8");
//////                ps.println("error: "+text);
////            } catch (UnsupportedEncodingException e1) {
////                e1.printStackTrace();
////            }
//
////            System.out.println("error: "+text);
//        }
//        return returntext;

        // Method 2
//        String[] words = text.split("[^a-zA-Z0-9]");
//        char[] textArray = text.toCharArray();
//        int left = 0;
//        for (int i = 0; i < words.length; i++) {
//            String temp = words[i];
//            left = text.indexOf(temp, left);
//            if (left == 0 && String.valueOf(text.charAt(left + temp.length())).matches("[^a-zA-Z0-9]")) {
//                if (bannedWords.contains(temp.toLowerCase())) {
//                    for (int j = left + 1; j < left + temp.length() - 1; j++) {
//                        textArray[j] = '*';
//                    }
//                }
//            } else if (left > 0) {
//                if (String.valueOf(text.charAt(left - 1)).matches("[^a-zA-Z0-9]") && String.valueOf(text.charAt(left + temp.length())).matches("[^a-zA-Z0-9]")) {
//                    if (bannedWords.contains(temp.toLowerCase())) {
//                        for (int j = left + 1; j < left + temp.length() - 1; j++) {
//                            textArray[j] = '*';
//                        }
//                    }
//                }
//            }
//        }
//        return new String(textArray);

        String[] words = text.split("[^a-zA-Z0-9]");
        char[] textArray = text.toCharArray();
        int left = 0;
        for (int i = 0; i < words.length; i++) {
            String temp = words[i];
            left = text.indexOf(temp, left);
            if (left == 0) {
                if ((left + temp.length()) <= (text.length() - 1)) {
                    if (String.valueOf(text.charAt(left + temp.length())).matches("[^a-zA-Z0-9]")) {
                        if (bannedWords.contains(temp.toLowerCase())) {
                            for (int j = left + 1; j < left + temp.length() - 1; j++) {
                                textArray[j] = '*';
                            }
                        }
                    }

                } else {

                    if (bannedWords.contains(temp.toLowerCase())) {
                        for (int j = left + 1; j < left + temp.length() - 1; j++) {
                            textArray[j] = '*';
                        }
                    }
                }
            } else if (String.valueOf(text.charAt(left - 1)).matches("[^a-zA-Z0-9]")) {
                if ((left + temp.length()) <= (text.length() - 1)) {
                    if (String.valueOf(text.charAt(left + temp.length())).matches("[^a-zA-Z0-9]")) {
                        if (bannedWords.contains(temp.toLowerCase())) {
                            for (int j = left + 1; j < left + temp.length() - 1; j++) {
                                textArray[j] = '*';
                            }
                        }
                    }
                } else {
                    if (bannedWords.contains(temp.toLowerCase())) {
                        for (int j = left + 1; j < left + temp.length() - 1; j++) {
                            textArray[j] = '*';
                        }
                    }
                }
            }
            left += words[i].length();
        }
        return new String(textArray);
    }
}
