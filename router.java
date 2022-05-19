import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class router
{
	public static Boolean debugfileio = false; 
	public static Boolean debugrouting = false; 
	public static Boolean debugargs = false; 
	public static Boolean debugnmr = false;
	public static Boolean debugdata = false;
	
	public int CONSTTIME = 120;
	
	public int RouterID;	
	public File routerFile;
	public RoutingTable[] rTable = new RoutingTable[10];
	public HashMap<Integer, Wrapper.LANinfo> LANs = new HashMap<Integer, Wrapper.LANinfo>();
	public int Timer = 0;
	public ArrayList<String> SentbyRouter = new ArrayList<String>();
	public int NMRExpiry = 0;
	
	public HashMap<Integer, Integer> incomingData = new HashMap<Integer, Integer>();
	public HashMap<Integer, ArrayList<Integer>> alldatastreams = new HashMap<Integer, ArrayList<Integer>>();
	
	public router() {}
	
	public void getArguments(String[] args) throws IOException
	{
		if(args.length < 2 || args.length > 11)
		{
			System.out.println("There is something wrong with the router arguments: ");
			System.out.println(Arrays.toString(args));
			System.exit(-1);
		}
		
		this.RouterID = Integer.parseInt(args[0]);
		
		for(int i = 0; i < rTable.length; i++)
		{
			this.rTable[i] = new RoutingTable();
			this.rTable[i].LANid = i; 
		}
		
		
		for(int i = 1; i < args.length; i++)
		{
			int LANid = Integer.parseInt(args[i]);
			
			if(LANid > 9 || LANid < 0)
			{
				System.out.println("There is something wrong with the router arguments: ");
				System.out.println(Arrays.toString(args));
				System.exit(-1);
			}

			Wrapper.LANinfo lan = new Wrapper.LANinfo(LANid);
			lan.startFile();
			this.LANs.put(LANid, lan);
			this.rTable[LANid].distancetoLAN = 0;
			this.rTable[LANid].nextHopRouter = this.RouterID;
			Wrapper.NMRinfo nmr = new Wrapper.NMRinfo();
			this.rTable[LANid].attatchedRouters.put(RouterID, nmr);
		}
		
		this.routerFile = new File("rout" + this.RouterID + ".txt");
	}
	
	public String toString()
	{
		String str = "";
		str += "RouterID: " + this.RouterID + "\n";
		str += "LANs: " + this.LANs.keySet().toString() + "\n";
		return str;
	}
	
	public String DVmsgBuilder(int LANid)
	{
		String str = "DV ";
		str += LANid + " ";
		str += RouterID + " ";
		
		for(int i = 0; i < rTable.length; i++)
		{
			str += rTable[i].distancetoLAN + " " + rTable[i].nextHopRouter + " ";
		}
		
		return str.trim();
	}
			
	public void readFile() throws IOException
	{
		for(int i : LANs.keySet())
		{
			RandomAccessFile readFiles = new RandomAccessFile(this.LANs.get(i).file, "r");
			readFiles.seek(this.LANs.get(i).Pointer);
			String line;
			while((line = readFiles.readLine()) != null)
			{
				if(this.SentbyRouter.remove(line + "\n"))
				{
					if(debugfileio)
					{
						System.out.println("This msg was sent by the router " + this.RouterID + " to LAN " + i + " " + line);
					}
					continue;
				}
				
				if(debugfileio)
				{
					System.out.println("Router " + RouterID + " READING from LAN " + i + " at timer: " + Timer + " " + line);
				}
				
				String[] readLine = line.split(" ");
				int lan = Integer.parseInt(readLine[1]);
				inputHandler(line, lan, "LAN", readLine[0]);
			}
			this.LANs.get(i).Pointer = readFiles.getFilePointer();
			readFiles.close();
		}
	}
	
	public void sendDVs() throws IOException
	{
		RandomAccessFile outputDV = new RandomAccessFile(this.routerFile, "rw");
		for(int i : LANs.keySet())
		{
			outputDV.seek(outputDV.length());
			String output = this.DVmsgBuilder(i) + "\n";
			outputDV.writeBytes(output);
			this.SentbyRouter.add(output);
			
			if(debugfileio)
			{
				System.out.println("Router " + this.RouterID + " writing " + this.DVmsgBuilder(i) + " to LAN " + i);
			}
		}
		outputDV.close();
	}
	
	public void RouterFunction() throws InterruptedException, IOException
	{
		for(; Timer < CONSTTIME; Timer++)
		{
			readFile();
			
			if(Timer % 5 == 0)
			{
				sendDVs();
			}
			
			handleSendNMR();
			
			Thread.sleep(1000);	
		}
	}
	
	public void inputHandler(String line, int lan, String Filetype, String msgType) throws NumberFormatException, IOException
	{
		switch (msgType.toLowerCase()) 
		{
			case "dv":
			{
				if(debugfileio)
				{
					System.out.println("Router " + this.RouterID + " Found DV msg in " + Filetype + " "+ lan + " " + line);
				}
				
				updateRoutingTable(line);
				break;
			}
			case "data":
			{
				if(debugfileio || debugdata)
				{
					System.out.println("Router " + this.RouterID + " Found data msg in " + Filetype + " " + lan + " " + line);
				}
				handleData(line);
				break;
			}
			case "nmr":
			{
				if(debugfileio || debugnmr)
				{
					System.out.println("Router " + this.RouterID + " Found NMR msg in " + Filetype + " " + lan + " " + line);
				}
				
				handleRecNMR(line);
				break;
			}
			case "receiver":
			{
				if(debugfileio)
				{
					System.out.println("Router " + this.RouterID + " Found receiver msg in " + Filetype + " " + lan + " " + line);
				}
				
				if(this.rTable[lan].pruneLAN == lan)
				{
					this.rTable[lan].pruneLoop = false;
				}
				this.rTable[lan].resetNMR(this.RouterID);	
				this.rTable[lan].Receiver = true;	
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
		String[] data = line.split(" ");
		int incominglan = Integer.parseInt(data[1]);
		int hostlan = Integer.parseInt(data[2]);
		
		if(this.alldatastreams.containsKey(hostlan))
		{
			if(this.alldatastreams.get(hostlan).contains(incominglan) == false)
			{
				this.alldatastreams.get(hostlan).add(incominglan);
			}
		}
		else
		{
			this.alldatastreams.put(hostlan, new ArrayList<Integer>());
			this.alldatastreams.get(hostlan).add(incominglan);
		}
		
		
		if(this.incomingData.containsKey(hostlan))
		{
			if(this.incomingData.get(hostlan) == incominglan)
			{
				sendData(incominglan, hostlan);
			}
			else
			{
				if(debugdata)
				{
					System.out.println("idk how to prune this lol");
				}

				if(debugdata)
				{
					System.out.println("Data collide in Router: " + this.RouterID + " on LAN: " + incominglan);
				}
				
				if(this.alldatastreams.get(hostlan).size() == this.rTable[incominglan].attatchedRouters.size())
				{
					Boolean higherID = false;
					if(this.rTable[incominglan].Receiver)
					{
						for(int x : this.rTable[incominglan].attatchedRouters.keySet())
						{
							if(x == this.RouterID)
							{
								continue;
							}
							
							if(x < this.RouterID)
							{
								higherID = true;
							}
						}
						
						if(higherID == false)
						{
							return;
						}
					}
		
					if(this.rTable[incominglan].pruneLoop == false)
					{
						this.rTable[incominglan].pruneLAN = incominglan;
						this.rTable[incominglan].pruneLoop = true;
						for(int x : rTable[incominglan].attatchedRouters.keySet())
						{
							this.rTable[incominglan].attatchedRouters.get(x).NMRreceiveTimer = 20;
							this.rTable[incominglan].attatchedRouters.get(x).NMRsendTimer = 0;
							this.rTable[incominglan].attatchedRouters.get(x).NMRstateExpiry = 0;
							this.rTable[incominglan].attatchedRouters.get(x).NMRstate = true;
						}
						this.rTable[incominglan].NMR = true;
						this.rTable[incominglan].Receiver = false;
						sendNMRs(incominglan);
						this.rTable[incominglan].attatchedRouters.get(this.RouterID).NMRsendTimer++;
					}
				}
				
			}
		}
		else
		{
			this.incomingData.put(hostlan, incominglan);
			sendData(incominglan, hostlan);
		}	
	}
	
	public void sendData(int incominglan, int hostlan) throws IOException
	{
		RandomAccessFile outputData = new RandomAccessFile(this.routerFile, "rw");
		for(int i : LANs.keySet())
		{
			if(i == incominglan)
			{
				continue;
			}
			
			if(i == hostlan)
			{
				continue;
			}
			
			if(this.rTable[i].NMR == false)
			{
				outputData.seek(outputData.length());
				String output = "data " + i + " " + hostlan + "\n";
				outputData.writeBytes(output);
				this.SentbyRouter.add(output);
				
				if(debugfileio || debugdata)
				{
					System.out.println("Router " + this.RouterID + " writing data" + output + " to LAN " + i);
				}
			}
		}
		outputData.close();
	}
	
	public String NMRmsgBuilder(int LANid, int hostLAN)
	{
		String str = "NMR ";
		str += LANid + " ";
		str += RouterID + " ";
		str += hostLAN;
		
		return str.trim();
	}
	
	public void sendNMRs(int hostLAN) throws IOException
	{
		RandomAccessFile outputNMR = new RandomAccessFile(this.routerFile, "rw");
		for(int i : LANs.keySet())
		{
			if(this.rTable[i].attatchedRouters.size() > 1)
			{
				this.rTable[i].checkNMR(RouterID);
				if(i == hostLAN)
				{
					continue;
				}
				outputNMR.seek(outputNMR.length());
				String output = this.NMRmsgBuilder(i, hostLAN) + "\n";
				outputNMR.writeBytes(output);
				this.SentbyRouter.add(output);
				
				if(debugfileio || debugnmr)
				{
					System.out.println("Router " + this.RouterID + " writing " + output + " to LAN " + i);
				}
			}
		}
		outputNMR.close();
	}
	
	public void handleSendNMR() throws IOException
	{
		for(int i : LANs.keySet())
		{
			if(this.rTable[i].attatchedRouters.size() > 1 && this.rTable[i].pruneLoop == false)
			{
				for(int x : rTable[i].attatchedRouters.keySet())
				{					
					//this.rTable[i].attatchedRouters.get(x).NMRreceiveTimer = -1;
					//this.rTable[i].attatchedRouters.get(x).NMRsendTimer = -1;
					if(this.rTable[i].attatchedRouters.get(x).NMRstateExpiry == 20)
					{
						this.rTable[i].resetNMR(x);
						if(debugnmr)
						{
							System.out.println("Router " + this.RouterID + " LAN " + i + " IN LOOP THAT NMRstateExpiry IS 20");
						}
					}
					
					if(this.rTable[i].attatchedRouters.get(x).NMRstate == true)
					{
						this.rTable[i].attatchedRouters.get(x).NMRstateExpiry++;
					}
				}
			}
			
			if(this.rTable[i].attatchedRouters.size() > 1 && this.rTable[i].NMR == false)
			{
				if(debugnmr)
				{
					for(int x : rTable[i].attatchedRouters.keySet())
					{
						System.out.println("Router " + this.RouterID + " LAN " + i + " NMR state: " + this.rTable[i].attatchedRouters.get(x).NMRstate(x));
					}
				}
			}
			
			if(this.rTable[i].attatchedRouters.size() > 1 && this.rTable[i].NMR == true)
			{
				if(debugnmr)
				{
					for(int x : rTable[i].attatchedRouters.keySet())
					{
						System.out.println("Router " + this.RouterID + " LAN " + i + " NMR state: " + this.rTable[i].attatchedRouters.get(x).NMRstate(x));
				
					}
				}
			}
			
			if(this.rTable[i].attatchedRouters.size() == 1 || this.rTable[i].pruneLoop)
			{		
				for(int x : rTable[i].attatchedRouters.keySet())
				{
					if(debugnmr)
					{
						System.out.println("PRUNELOOP Router " + this.RouterID + " LAN " + i + " NMR state: " + this.rTable[i].attatchedRouters.get(x).NMRstate(x));
					}
					
					if(this.rTable[i].attatchedRouters.get(x).NMRreceiveTimer == 20)
					{
						this.rTable[i].NMR = true;
						this.rTable[i].Receiver = false;
					}
					
					if(this.rTable[i].attatchedRouters.get(x).NMRreceiveTimer < 20)
					{
						this.rTable[i].attatchedRouters.get(x).NMRreceiveTimer++;
						this.rTable[i].NMR = false;
						if(debugnmr)
						{
							System.out.println("Router " + this.RouterID + " LAN " + i + " IN LOOP THAT NMRTIMER IS < 20");
						}
					}
				}
				
				if(this.rTable[i].NMR)
				{
					if(debugnmr)
					{
						for(int x : rTable[i].attatchedRouters.keySet())
						{
							System.out.println("INCREMENTING SEND TIMER/SENDING NMR Router " + this.RouterID + " LAN " + i + " NMR state: " + this.rTable[i].attatchedRouters.get(x).NMRstate(x));
						}
					}
					
					if(this.rTable[i].attatchedRouters.get(this.RouterID).NMRsendTimer % 10 == 0)
					{
						sendNMRs(i);
					}
					this.rTable[i].attatchedRouters.get(this.RouterID).NMRsendTimer++;
				}
				
			}			
		}		
	}
	
	public void forwardNMRs(int LANfwd, int hostLAN) throws IOException 
	{
		RandomAccessFile outputNMR = new RandomAccessFile(this.routerFile, "rw");
		for(int i : LANs.keySet())
		{
			this.rTable[i].checkNMR(RouterID);
			
			if(i == LANfwd)
			{
				continue;
			}
			
			if(this.rTable[i].NMR == false)
			{
				outputNMR.seek(outputNMR.length());
				String output = this.NMRmsgBuilder(i, hostLAN) + "\n";
				
				outputNMR.writeBytes(output);
				this.SentbyRouter.add(output);
				
				if(debugfileio || debugnmr)
				{
					System.out.println("Router " + this.RouterID + " forwarding " + this.NMRmsgBuilder(i, hostLAN) + " to LAN " + i);
				}
			}
			else
			{
				if(debugnmr)
				{
					System.out.println("Router " + this.RouterID + " NOT FORWARDING FOR NMR TO LAN: " + i);
				}
			}
		}
		outputNMR.close();
	}
	
	public void handleRecNMR(String l) throws IOException
	{
		String[] data = l.split(" ");
		
		int lan = Integer.parseInt(data[1]);
		int sendingrouter = Integer.parseInt(data[2]);
		int hostlan = Integer.parseInt(data[3]);
		
		this.rTable[lan].setNMRvalues(sendingrouter);
		this.rTable[lan].checkNMR(RouterID);
		
		if(debugnmr)
		{
			System.out.println("Router " + this.RouterID + " " + this.rTable[lan].toString());
			System.out.println("Router: " + RouterID + " router table: \n" + Arrays.toString(rTable));
		}	
		
		if(this.rTable[lan].NMR)
		{
			if(debugnmr)
			{
				System.out.println("Router " + this.RouterID + " forwarding NMRS");
			}
			forwardNMRs(lan, hostlan);
		}
		else
		{
			if(debugnmr)
			{
				System.out.println("Router " + this.RouterID + " NOT forwarding NMRS");
			}
		}
	}
	
	public void updateRoutingTable(String l)
	{
		String[] data = l.split(" ");
		int routernexthop = Integer.parseInt(data[2]);
		int index = 0;
		for(int i = 3; i < data.length/2; i+=2)
		{
			int numofhops = Integer.parseInt(data[i]);
			
			if(numofhops >= 10)
			{
				index++;
				continue;
			}		
			
			RoutingTable row = this.rTable[index];			
			
			if(this.LANs.containsKey(index) && numofhops == 0)
			{
				if(debugfileio)
				{
					System.out.println("Router " + this.RouterID + 
							" LAN " + index + " attached routers " 
							+ this.rTable[index].attatchedRouters.keySet().toString() 
							+ " router next hop: " + routernexthop 
							+ " numhops: " + numofhops + " " + l);
				}
				
				if(!row.attatchedRouters.containsKey(routernexthop))
				{
					Wrapper.NMRinfo nmr = new Wrapper.NMRinfo();
					row.attatchedRouters.put(routernexthop, nmr);
				}
			
				if(row.distancetoLAN == 0)
				{
					index++;
					continue;
				}
			}
			
			if(row.distancetoLAN > numofhops + 1)
			{
				row.distancetoLAN = numofhops + 1;
				row.nextHopRouter = routernexthop;
			}
			else if(this.rTable[index].distancetoLAN == numofhops + 1)
			{					
				if(routernexthop < row.nextHopRouter)
				{
					row.distancetoLAN = numofhops + 1;
					row.nextHopRouter = routernexthop;
				}
			}
			this.rTable[index] = row;
			index++;
		}
		
		if(debugrouting)
		{
			System.out.println("Router: " + this.RouterID + " router table: \n" + Arrays.toString(this.rTable));
		}
		
	}
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		
		router myRouter = new router();
		myRouter.getArguments(args);
		
		if(debugargs)
		{
			System.out.println(myRouter.toString());
		}
		
		myRouter.RouterFunction();
		//System.out.println("Router: " + myRouter.RouterID + " router table: \n" + Arrays.toString(myRouter.rTable));
		System.out.println("Router " + myRouter.RouterID + " FINISHED!");
		System.exit(0);
	}
	
	public class RoutingTable
	{
		public int nextHopRouter = -1;
		public int distancetoLAN = 10;
		public int LANid = -1;
		public Boolean NMR = false;
		public HashMap<Integer, Wrapper.NMRinfo> attatchedRouters = new HashMap<Integer, Wrapper.NMRinfo>();
		public Boolean Receiver = false;
		public Boolean pruneLoop = false;
		public int pruneLAN = -1;
		
		public RoutingTable() {}
		
		
		public String toString()
		{
			String str = "";
			str += "LANid: " + this.LANid + ", ";
			str += "NextHopRouter: " + this.nextHopRouter + ", ";
			str += "DistancetoLAN: " + this.distancetoLAN + ", ";
			str += "NMR value: " + this.NMR + ", ";
			str += "Routers on LAN: " + this.attatchedRouters.keySet().toString() + ", ";
			str += "NMR Value of routers: " + this.attatchedRouters.values(). toString() + "\n";
			return str;
		}
				
		public void resetNMR(int i)
		{
			this.attatchedRouters.get(i).resetNMR();
			this.NMR = false;
		}
		
		public void setNMRvalues(int routerid)
		{
			this.attatchedRouters.get(routerid).NMRstate = true;
			this.attatchedRouters.get(routerid).NMRstateExpiry = 0;
		}
		
		public void checkNMR(int i)
		{
			Boolean flag = true;
			for(int x : this.attatchedRouters.keySet())
			{
				if(x == RouterID)
				{
					continue;
				}
				if(this.attatchedRouters.get(x).NMRstate == false)
				{
					if(debugnmr)
					{
						System.out.println("Router " + x + " NMR STATE IS FALSE ");
					}
					flag = false;
				}
			}
			this.NMR = flag;
			if(this.Receiver == true)
			{
				if(debugnmr)
				{
					System.out.println("Router " + RouterID + " HAS A RECEIVER ON LAN " + LANid);
				}
				this.NMR = false;
			}
			if(this.NMR)
			{
				setNMRvalues(RouterID);
			}
		}
	}
	
	
}
