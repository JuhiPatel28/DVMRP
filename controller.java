import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;

public class controller
{
	public static Boolean debugfileio = false; 
	public static Boolean debugargs = false; 
	
	public int CONSTTIME = 120;
	
	public HashMap<Integer, Wrapper.LANinfo> LANs = new HashMap<Integer, Wrapper.LANinfo>();
	public HashMap<Integer, Wrapper.Hostinfo> Hosts = new HashMap<Integer, Wrapper.Hostinfo>();
	public HashMap<Integer, Wrapper.Routinfo> Routers = new HashMap<Integer, Wrapper.Routinfo>();
	public int Timer = 0;
	
	public controller() {}
	
	public void getArguments(String[] args) throws IOException
	{
		String whichList = "";
		if(args.length > 33 || args.length < 3)
		{
			System.out.println("There is something wrong with the controller arguments: ");
			System.out.println(Arrays.toString(args));
			System.exit(-1);
		}
		for(int i = 0; i < args.length; i++)
		{
			if(isInt(args[i]))
			{
				int id = Integer.parseInt(args[i]);
				switch (whichList) 
				{
					case "host":
						Wrapper.Hostinfo h = new Wrapper.Hostinfo(id);
						h.startoutFile();
						this.Hosts.put(id, h);
						break;
					case "router":
						Wrapper.Routinfo r = new Wrapper.Routinfo(id);
						r.startFile();
						this.Routers.put(id, r);
						break;
					case "lan":
						if(id > 9 || id < 0)
						{
							System.out.println("There is something wrong with the controller arguments: ");
							System.out.println(Arrays.toString(args));
							System.exit(-1);
						}
						Wrapper.LANinfo l = new Wrapper.LANinfo(id);
						l.startFile();
						
						this.LANs.put(id, l);
						break;
					default:
						System.out.println("There is something wrong with the controller arguments: ");
						System.out.println(Arrays.toString(args));
						System.exit(-1);
						break;
				}
			}
			else
			{
				whichList = args[i].toLowerCase();
			}
		}
	}
	
	public void readRouterFiles() throws IOException
	{
		for(int i : Routers.keySet())
		{
			RandomAccessFile readRouterFiles = new RandomAccessFile(this.Routers.get(i).file, "r");
			readRouterFiles.seek(this.Routers.get(i).Pointer);
			String line;
			while((line = readRouterFiles.readLine()) != null)
			{
				String[] readLine = line.split(" ");
				int lan = Integer.parseInt(readLine[1]);
				File f = LANs.get(lan).file;
				inputHandler(line, i, "Router", f, readLine[0] ,lan);
			}
			this.Routers.get(i).Pointer = readRouterFiles.getFilePointer();
			
			if(debugfileio)
			{
				System.out.println("File pointer for router " + i + " :" + this.Routers.get(i).Pointer);
				System.out.println("Last line read: " + line);
			}
	
			readRouterFiles.close();
		}
	}
	
	public void readHostFiles() throws IOException
	{
		for(int i : Hosts.keySet())
		{
			RandomAccessFile readHostFiles = new RandomAccessFile(this.Hosts.get(i).outfile, "r");
			readHostFiles.seek(this.Hosts.get(i).Pointer);
			String line;
			while((line = readHostFiles.readLine()) != null)
			{
				String[] readLine = line.split(" ");
				int lan = Integer.parseInt(readLine[1]);
				File f = LANs.get(lan).file;
				inputHandler(line, i, "Hosts", f, readLine[0] ,lan);
			}
			this.Hosts.get(i).Pointer = readHostFiles.getFilePointer();
			
			if(debugfileio)
			{
				System.out.println("File pointer for host " + i + " :" + this.Hosts.get(i).Pointer);
				System.out.println("Last line read: " + line);
			}
	
			readHostFiles.close();
		}
	}
	
	public void ControllerFunction() throws IOException, InterruptedException 
	{
		for(; Timer < CONSTTIME; Timer++)
		{
			readRouterFiles();
			
			readHostFiles();
			
			Thread.sleep(1000);
		}
	}
	
	public void inputHandler(String line, int i, String Filetype, File f, String msgType, int lan) throws NumberFormatException, IOException
	{
		switch (msgType.toLowerCase()) 
		{
			case "dv":
			{
				if(debugfileio)
				{
					System.out.println("Controller Found DV msg in " + Filetype + " "+ i + " " + line);
				}
				
				Wrapper.writeFile(f, line);
				break;
			}
			case "data":
			{
				if(debugfileio)
				{
					System.out.println("Controller Found data msg in " + Filetype + " " + i + " " + line);
				}
				
				Wrapper.writeFile(f, line);	
				break;
			}
			case "nmr":
			{
				if(debugfileio)
				{
					System.out.println("Controller Found NMR msg in " + Filetype + " " + i + " " + line);
				}
								
				Wrapper.writeFile(f, line);	
				break;
			}
			case "receiver":
			{
				if(debugfileio)
				{
					System.out.println("Controller Found receiver msg in " + Filetype + " " + i + " " + line);
				}
				
				Wrapper.writeFile(f, line);	
				break;
				
			}
			default:
			{
				System.out.println("There is something wrong with line: " + line + "in " + Filetype + " " + i);
				System.exit(-1);
				break;
			}
		}
	}
		
	public String toString()
	{
		String str = "";
		str += "Hosts: " + this.Hosts.keySet().toString() + "\n";
		str += "Routers: " + this.Routers.keySet().toString() + "\n";
		str += "LANs: " + this.LANs.keySet().toString() + "\n";
		return str;
	}
		
	public static void main(String[] args) throws IOException, InterruptedException
	{
		controller myController = new controller();
		myController.getArguments(args);
		
		if(debugargs)
		{
			System.out.println(myController.toString());
		}
		
		myController.ControllerFunction();
		System.out.println("CONTROLLER FINISHED");
		System.exit(0);
	}
	
	public static boolean isInt(String str) 
	{
	    try 
	    {	    	
	        Integer.parseInt(str);
	        return true;
	    } 
	    catch (NumberFormatException e) 
	    {
	        return false;
	    }
	}
}
