import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;

public class host
{
	public static Boolean debugfileio = false;
	public static Boolean debugargs = false;
	
	public int CONSTTIME = 120;
	
	public int HostID;
	public Wrapper.LANinfo LAN;
	public String Type;	
	public int Timer = 0;
	public int TimetoStart = -1;
	public int Period = -1;
	
	public File Hostin;
	public File Hostout;
	
	public ArrayList<String> SentbyHost = new ArrayList<String>();
		
	public host() {}

	public void getArguments(String[] args) throws IOException
	{
		if(args.length > 5 || args.length < 3)
		{
			System.out.println("There is something wrong with the host arguments: ");
			System.out.println(Arrays.toString(args));
			System.exit(-1);
		}
		
		this.HostID = Integer.parseInt(args[0]);
		int id = Integer.parseInt(args[1]);
		if(id > 9 || id < 0)
		{
			System.out.println("There is something wrong with the host arguments: ");
			System.out.println(Arrays.toString(args));
			System.exit(-1);
		}
		
		this.LAN = new Wrapper.LANinfo(id);
		this.LAN.startFile();
		this.Type = args[2];
	
		
		if(this.Type.equalsIgnoreCase("sender"))
		{
			this.TimetoStart = Integer.parseInt(args[3]);
			this.Period = Integer.parseInt(args[4]);
		}
		else
		{
			String filename = "hin" + this.HostID + ".txt";
			File Hinfile = new File(filename);
			if(Hinfile.createNewFile())
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
			this.Hostin = Hinfile;
		}
		this.Hostout = new File("hout" + this.HostID + ".txt");
	}
	
	public String toString()
	{
		String str = "";
		str += "HostID: " + this.HostID + "\n";
		str += "LAN: " + this.LAN + "\n";
		str += "Type: " + this.Type + "\n";
		str += "TimetoStart: " + this.TimetoStart + "\n";
		str += "Period: " + this.Period + "\n";
		return str;
	}
	
	public void sendReceiver() throws IOException
	{
		RandomAccessFile outputRec = new RandomAccessFile(this.Hostout, "rw");
		
		outputRec.seek(outputRec.length());
		String output = "receiver " + this.LAN.LANid + "\n";
		outputRec.writeBytes(output);
		this.SentbyHost.add(output);		
		
		if(debugfileio)
		{
			System.out.println("Host " + this.HostID + " sending receiver to LAN " + LAN.LANid + " " + output);
		}
		
		outputRec.close();		
	}
	
	public void readFile() throws IOException
	{
		RandomAccessFile readFiles = new RandomAccessFile(this.LAN.file, "r");
		readFiles.seek(this.LAN.Pointer);
		String line;
		if(debugfileio)
		{
			System.out.println("Host " + this.HostID + " READING from LAN " + LAN.LANid + " at timer: " + Timer);
		}
		while((line = readFiles.readLine()) != null)
		{
			if(this.SentbyHost.remove(line + "\n"))
			{
				if(debugfileio)
				{
					System.out.println("This msg was sent by the host " + this.HostID + " to LAN " + LAN.LANid + " " + line);
				}
				continue;
			}
			
			if(debugfileio)
			{
				System.out.println("Host " + this.HostID + " READING from LAN " + LAN.LANid + " at timer: " + Timer + " " + line);
			}
			
			String[] readLine = line.split(" ");
			int lan = Integer.parseInt(readLine[1]);
			inputHandler(line, lan, "LAN", readLine[0]);
		}
		this.LAN.Pointer = readFiles.getFilePointer();
		readFiles.close();
	}
	
	public void inputHandler(String line, int lan, String Filetype, String msgType) throws NumberFormatException, IOException
	{
		switch (msgType.toLowerCase()) 
		{
			case "dv":
			{
				if(debugfileio)
				{
					System.out.println("Host " + this.HostID + " Found DV msg in " + Filetype + " "+ lan + " " + line);
				}
				
				break;
			}
			case "data":
			{
				if(debugfileio)
				{
					System.out.println("Host " + this.HostID + " Found data msg in " + Filetype + " " + lan + " " + line);
				}
				
				handleData(line);
				break;
			}
			case "nmr":
			{
				if(debugfileio)
				{
					System.out.println("Host " + this.HostID + " Found NMR msg in " + Filetype + " " + lan + " " + line);
				}
				
				break;
			}
			case "receiver":
			{
				if(debugfileio)
				{
					System.out.println("Host " + this.HostID + " Found receiver msg in " + Filetype + " " + lan + " " + line);
				}
				
				break;
			}
			default:
			{
				if(debugfileio)
				{
					System.out.println("There is something wrong with line: " + line + "in " + Filetype + " " + lan);
				}
				
				System.exit(-1);
				break;
			}
		}
	}
	
	public void handleData(String line) throws IOException
	{
		RandomAccessFile outputData = new RandomAccessFile(this.Hostin, "rw");
		outputData.seek(outputData.length());
		outputData.writeBytes(line + "\n");
		
		if(debugfileio)
		{
			System.out.println("Host " + this.HostID + " found data in LAN " + LAN.LANid + " " + line);
		}
		
		outputData.close();
	}
	
	public void sendData() throws IOException
	{
		RandomAccessFile outputData = new RandomAccessFile(this.Hostout, "rw");
		outputData.seek(outputData.length());
		String line = "data " + LAN.LANid + " " + LAN.LANid + "\n";
		outputData.writeBytes(line);
		outputData.close();
		
		if(debugfileio)
		{
			System.out.println("Host " + this.HostID + " sending data to LAN " + LAN.LANid + " " + line);
		}
	}
	
	public void HostFunction() throws InterruptedException, IOException
	{
		if(this.Type.equalsIgnoreCase("sender"))
		{
			int counter = 1;
			Thread.sleep((TimetoStart*1000));
			for(Timer += TimetoStart ; Timer < CONSTTIME; Timer++)
			{
				if(counter == Period)
				{
					counter = 0;
					sendData();
				}
				counter++;
				Thread.sleep(1000);
			}
		}
		else
		{
			for(; Timer < CONSTTIME; Timer++)
			{
				readFile();
				
				if(Timer % 10 == 0)
				{
					sendReceiver();
				}
				
				Thread.sleep(1000);
			}
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		host myHost = new host();
		myHost.getArguments(args);
		
		if(debugargs)
		{
			System.out.println(myHost.toString());
		}
		
		myHost.HostFunction();
		System.out.println("HOST " + myHost.HostID + " FINISHED");
		System.exit(0);
		
	}
}
