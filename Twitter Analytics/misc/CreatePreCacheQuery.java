import java.io.*;
import java.util.*;

public class CreatePreCacheQuery {
	public static void main(String[] args) throws IOException {
		File file=new File("200w.sql");
		if (!file.exists()) {
			file.createNewFile();
		}
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		
		File user_id=new File("user_id");
		Scanner scan=new Scanner(user_id);
		int last=0;
		int temp=0;
		int count=0;
		while(scan.hasNextLine())
		{
			temp=scan.nextInt();
			if(temp==last)
				continue;
			count++;
			if(count==2000000)
				break;
			last=temp;
			out.append("SELECT density,time,tweet_id,censored,hashtag FROM tweets WHERE user_id="+temp+";\n");
		}
		
//		out.write(sb.toString());
		out.close();
	}
}