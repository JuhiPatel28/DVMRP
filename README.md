# DVMRP

Simulates the DVMRP Protocol using files that represent Hosts, LANs, and Routers. 

The "test.sh" files simulate a connected network of Hosts, LANs, and Routers in different configurations.

Hosts are either receivers or senders. Receivers advertise themselves on their LANs. Senders output data messages that attempt to find receiver routers.
Routers search the LANs they are connected to for messages to forward. Routers will find the best path from sending hosts to receiving hosts by pruning LANs with no receivers and pruning paths that are not optimal. 

After the initial setup period where data messages propagate throughout the simulated network, the routers can determine the optimal paths for each sending router to each receiving router.
