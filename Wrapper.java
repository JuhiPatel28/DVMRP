import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Wrapper 
{
	public static Boolean debugfileio = false;
	
	
	public static class LANinfo 
	{
		public int LANid;
		public File file = null;
		public long Pointer = 0;
		
		public LANinfo(int lanid)
		{
			this.LANid = lanid;
		}
		
		public String toString()
		{
			String str = "";
			str += LANid;
			return str;
		}
		
		public void startFile() throws IOException
		{
			String filename = "lan" + this.LANid + ".txt";
			File LANfile = new File(filename);
			if(LANfile.createNewFile())
			{
				if(debugfileio)
				{
					System.out.println("File " + filename + " created.");
				}
				
			}
			else
			{
				if(debugfileio)
				{
					System.out.println("File " + filename + " already exists.");
				}	
			}
			this.file = LANfile;
		}
	}
	
	public static class Hostinfo 
	{
		public int Hostid;
		public File infile = null;
		public File outfile = null;
		public long Pointer = 0;
		
		public Hostinfo(int Hostid)
		{
			this.Hostid = Hostid;
		}
		
		public String toString()
		{
			String str = "";
			str += Hostid;
			return str;
		}
		
		public void startoutFile() throws IOException
		{
			String filename = "hout" + this.Hostid + ".txt";
			File Hostfile = new File(filename);
			if(Hostfile.createNewFile())
			{
				if(debugfileio)
				{
					System.out.println("File " + filename + " created.");
				}
			}
			else
			{
				if(debugfileio)
				{
					System.out.println("File " + filename + " already exists.");
				}	
			}
			this.outfile = Hostfile;
		}
		
		public void startinFile() throws IOException
		{
			String filename = "hin" + this.Hostid + ".txt";
			File Hostfile = new File(filename);
			if(Hostfile.createNewFile())
			{
				if(debugfileio)
				{
					System.out.println("File " + filename + " created.");
				}
			}
			else
			{
				if(debugfileio)
				{
					System.out.println("File " + filename + " already exists.");
				}
			}
			this.infile = Hostfile;
		}
	}
	
	public static class Routinfo 
	{
		public int Routid;
		public File file = null;
		public long Pointer = 0;
		
		public Routinfo(int Routid)
		{
			this.Routid = Routid;
		}
		
		public String toString()
		{
			String str = "";
			str += Routid;
			return str;
		}
		
		public void startFile() throws IOException
		{
			String filename = "rout" + this.Routid + ".txt";
			File Routfile = new File(filename);
			if(Routfile.createNewFile())
			{
				if(debugfileio)
				{
					System.out.println("File " + filename + " created.");
				}
			}
			else
			{
				if(debugfileio)
				{
					System.out.println("File " + filename + " already exists.");
				}
			}
			this.file = Routfile;
		}
	}
	
	public static class NMRinfo
	{
		public int NMRreceiveTimer = 0;
		public int NMRsendTimer = 0;
		public int NMRstateExpiry = 0;
		public Boolean NMRstate = false;
		
		public NMRinfo() {}
		
		public void resetNMR()
		{
			NMRreceiveTimer = 0;
			NMRsendTimer = 0;
			NMRstateExpiry = 0;
			NMRstate = false;
		}
		
		public String toString()
		{
			String str = "";
			str += NMRstate;
			return str;
		}
		
		public String NMRstate(int i)
		{
			String str = "";
			str += "Attached Router: " + i + ": ";
			str += "NMRstateExpiry: " + NMRstateExpiry + ", ";
			str += "NMRreceiveTimer: " + NMRreceiveTimer + ", ";
			str += "NMRsendTimer: " + NMRsendTimer;
			return str;
		}
	}
	
	public static void writeFile(File file, String line) throws IOException
	{
		RandomAccessFile inputLAN = new RandomAccessFile(file, "rw");
		inputLAN.seek(inputLAN.length());
		inputLAN.writeBytes(line + "\n");
		inputLAN.close();
	}
	
}

