import java.util.*;
import java.io.*;

class CompareWithReference {
	public static void main(String[] args) throws FileNotFoundException{
		File mine=new File("reduceresult");
		File reference=new File("reference-sorted");
		Scanner scanner=new Scanner(mine);
		Scanner ref=new Scanner(reference);
		int count=0;
		while(ref.hasNextLine())
		{
			String temp=scanner.nextLine();
			String temp2=ref.nextLine();

			if(temp2.equals(""))
			{
				continue;
			}
			
			String den=temp.split("\t")[0];
			String den2=temp2.split("\t")[0];
			byte[] b1=den.getBytes();
			byte[] b2=den2.getBytes();
			
			for(int i=0;i<b1.length;i++)
			{
				if(b1[i]!=b2[i])
				{
					System.out.println(temp);
					System.out.println(temp2);
					break;
				}
			}
//			if(!den.equals(den2))
//			{
//				System.out.println(temp);
//			}
		}
	}
}