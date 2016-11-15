package teamtest.undertow;

import java.io.*;

public class Reducer {
	public static void main(String[] args) {
		try{

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String id = "";
			String currentId=null;
			String[] currentInf={};
			String input;
			// input format: tweet_id \t userId \t date \t tweet_content(censored)
			while((input=br.readLine())!=null)
			{
				String[] parts = input.split("\t");
				if(parts.length<4)
					continue;
				// tweet id
				id=parts[0];

				if(currentId!=null && currentId.equals(id))
				{
					continue;
				}
				else //Tweet has changed
				{
					if(currentId!=null)
					{
						StringBuilder sb=new StringBuilder();
						// output format: userid#date  text
						sb.append(currentInf[1]+"#");
						sb.append(currentInf[2]+"\t");
						sb.append(currentInf[3]);
						System.out.println(sb.toString());
					}
					currentId=id;
					currentInf=parts;
				}
			}
			if(currentId!=null && currentId.equals(id))
			{
				StringBuilder sb=new StringBuilder();
				sb.append(currentInf[1]+"#");
				sb.append(currentInf[2]+"\t");
				sb.append(currentInf[3]);
				System.out.println(sb.toString());
			}

		}catch(IOException io){
			io.printStackTrace();
		}
	}
}