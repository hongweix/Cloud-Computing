package team;

import java.io.*;

public class Reducer {
    public static void main(String[] args) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF8"));
            String id = "";
            String currentId = null;
            String[] currentInf = {};
            String input = "";

            //While we have input on stdin
            while ((input = br.readLine()) != null) {

                String[] parts = input.split("\t");
                id = parts[0];

                if (currentId != null && currentId.equals(id)) {
                    continue;
                } else //Tweet has changed
                {
                    if (currentId != null) //Is this the first id, if not output
                    {
                        StringBuilder sb = new StringBuilder();
                        sb.append(currentId + "\t");
                        for (int i = 1; i < currentInf.length; i++) {
                            sb.append(currentInf[i] + "\t");
                        }
                        sb.deleteCharAt(sb.length() - 1);
                        System.out.println(sb.toString());
                    }
                    currentId = id;
                    currentInf = parts;
                }
            }

            //Print out last word if missed
            if (currentId != null && currentId.equals(id)) {
                StringBuilder sb = new StringBuilder();
                sb.append(currentId + "\t");
                for (int i = 1; i < currentInf.length; i++) {
                    sb.append(currentInf[i] + "\t");
                }
                sb.deleteCharAt(sb.length() - 1);
                System.out.println(sb.toString());
            }

        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}
