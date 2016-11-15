package teamtest.undertow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Reducer2 {
	public static void main(String[] args) {
		try{
			// sort before loading
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String id = "";
			String currentId=null;
			String[] currentInf={};
			String input;
			HashMap<String, Integer> map = new HashMap<String, Integer>();

			//While we have input on stdin
			while((input=br.readLine())!=null){

				//input format: userId#date \t text

				String[] parts = input.split("\t");

				// userId#date
				id=parts[0];

				if(currentId!=null && currentId.equals(id))
				{
					String[] text = parts[1].split(" ");
					for(String s: text){
						if(!map.containsKey(s)){
							map.put(s, 1);
						}else{
							map.put(s, map.get(s)+1);
						}
					}
				}
				else //userId#date has changed
				{
					if(currentId!=null)
					{
						String[] temp = currentId.split("#");
						StringBuilder sb=new StringBuilder();
						sb.append(temp[0]+"\t");
						sb.append(temp[1]+"\t");
						Iterator it = map.entrySet().iterator();
						while(it.hasNext()){
							Map.Entry entry = (Map.Entry)it.next();
							sb.append(entry.getKey() + ":" + entry.getValue() + " ");
						}

						System.out.println(sb.toString());
					}
					map.clear();
					String[] text=parts[1].split(" ");
					for(String s:text)
					{
						if(!map.containsKey(s)){
							map.put(s, 1);
						}
						else{
							map.put(s,map.get(s)+1);
						}
					}
					currentId=id;
					currentInf=parts;

				}
			}

			//Print out last word if missed
			if(currentId!=null && currentId.equals(id))
			{
				String[] temp = currentId.split("#");
				StringBuilder sb=new StringBuilder();
				sb.append(temp[0]+"\t");
				sb.append(temp[1]+"\t");

				Iterator it = map.entrySet().iterator();
				while(it.hasNext()){
					Map.Entry entry = (Map.Entry)it.next();
					sb.append("" + entry.getKey() + ":" + entry.getValue() + " ");
				}
				System.out.println(sb.toString());
			}

		}catch(IOException io){
			io.printStackTrace();
		}
	}
}