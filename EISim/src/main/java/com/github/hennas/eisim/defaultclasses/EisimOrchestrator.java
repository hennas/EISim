package com.github.hennas.eisim.defaultclasses;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;

import com.github.hennas.eisim.core.simulationengine.Event;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager;
import com.github.hennas.eisim.core.taskgenerator.Task;
import com.github.hennas.eisim.core.taskorchestrator.Orchestrator;
import com.github.hennas.eisim.core.datacentersmanager.ComputingNode;
import com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNode;
import com.github.hennas.eisim.core.network.InfrastructureGraph;
import com.github.hennas.eisim.core.network.NetworkLink;
import com.github.hennas.eisim.core.scenariomanager.SimulationParameters;
import com.github.hennas.eisim.core.simulationmanager.DefaultSimulationManager;

/**
 * Implements the default task orchestration logic used in EISim.
 * <p>
 * IESim supports three orchestration control topologies (algorithms) by default: CENTRALIZED, HYBRID, 
 * and DECENTRALIZED.
 * In the centralized control topology, there is one central orchestrator that decides the price for
 * the execution on the edge platform, as well as allocates servers for user tasks. Users (edge devices) 
 * only decide whether they offload or not to the allocated server. 
 * In the hybrid control topology, the edge servers are grouped into clusters with assigned cluster 
 * heads. The cluster heads decide the prices and allocate the tasks inside the clusters, while the 
 * users decide whether they offload and to which cluster.
 * In the decentralized control topology, each edge server decides its own price and users decide 
 * whether they offload and to which server.
 * <p>
 * The orchestration logic implemented in this class assumes that the only possible choices for 
 * task execution are the edge device that generates the task (local execution) and edge servers 
 * (offload task). It is also assumed that all the edge servers have the same specification, i.e., 
 * they are of homogeneous capacity. Hence, when using this class, the simulation_parameters.properties 
 * file should have orchestration_architectures=EDGE_ONLY and enable_orchestrators=false, and the 
 * edge_datacenters.xml file should have the same MIPS, cores, RAM and storage, as well as max and 
 * idle consumption for all servers.
 * 
 * @see EisimSimulationManager
 * 
 * @author Henna Kokkonen
 *
 */
public class EisimOrchestrator extends Orchestrator {

	protected EisimComputingNode centralOrchestrator; // Only needed when algorithm = CENTRALIZED
	protected Map<Long, Double> delayMap = new LinkedHashMap<>();

	public EisimOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		// Creating a map of delays between edge nodes
		saveMANdelaysToMap();
		
		if ("CENTRALIZED".equals(this.algorithmName)) {
			/* 
			 * If the algorithm in this simulation scenario is CENTRALIZED, it is assumed that the edge platform 
			 * has only one edge server for which isClusterHead == true. This server is the central orchestrator 
			 * of the whole edge platform. If for some reason there are more than one server with isClusterHead == true, 
			 * the following code line assigns only the first found cluster head server as the central orchestrator.
			 */
			centralOrchestrator = (EisimComputingNode) this.nodeList.stream()
					.filter(e -> ((EisimComputingNode) e).isClusterHead())
					.findFirst().get();
		}
	}
	
	/**
	 * Allocates a task inside a cluster when it arrives at the cluster head. 
	 * Needed in hybrid control topology. 
	 * <p>
	 * This method is called when an offloaded task has arrived at the cluster head. 
	 * The cluster head allocates the task on the cluster node that has the shortest queue length. 
	 * (Bottom-up allocation strategy)
	 * 
	 * @param task			The task being orchestrated
	 * @param clusterHead	The head of the cluster at which the task has arrived
	 */
	public void orchestrateInsideCluster(EisimTask task, EisimComputingNode clusterHead) {
		List<EisimComputingNode> clusterMembers = clusterHead.getClusterMembers();
		
		// Here the queue length is defined as the number of tasks in the queue; The queue length 
		// could also be defined as the total number of MIs summed over the tasks in the queue
		int minQueueLength = clusterHead.getTasksQueue().size();
		EisimComputingNode selected = clusterHead;
		
		for (EisimComputingNode member : clusterMembers) {
			if (member.getTasksQueue().size() < minQueueLength) {
				minQueueLength = member.getTasksQueue().size();
				selected = member;
			}
		}
		
		task.setOffloadingDestination(selected);
	}
	
	/**
	 * Sets the selected execution location as the task's offloading destination. 
	 * <p>
	 * The default implementation of {@code Orchestrator} class is replaced with a new default 
	 * implementation that also accounts for the situation where the local edge device is
	 * chosen as the task execution location (local device is not in the nodesList). 
	 * Further, calling {@code setApplicationPlacementLocation} for the edge device is removed 
	 * in this new default implementation, as the idea in this implementation is to choose the execution 
	 * location for each task independently, not to assign all the tasks to go to the same server 
	 * until a task fails.
	 * 
	 * @see #findComputingNode(String[], Task)
	 * @see Orchestrator#orchestrate(Task)
	 * @see DefaultComputingNode#setApplicationPlacementLocation(ComputingNode)
	 * @see DefaultSimulationManager#setFailed(Task, int)
	 */
	@Override
	protected void assignTaskToComputingNode(Task task, String[] architectureLayers) {

		int nodeIndex = findComputingNode(architectureLayers, task);

		if (nodeIndex == this.nodeList.size()) {
			// The task is locally computed by the edge device
			((EisimTask) task).setIntermediateOffloadingDestination(task.getEdgeDevice());
			task.setOffloadingDestination(task.getEdgeDevice());
			
			simLog.deepLog(simulationManager.getSimulation().clock() + ": " + this.getClass() + " Task: " + task.getId()
			+ " assigned to be locally computed by EDGE_DEVICE " + task.getEdgeDevice().getId());
			
		} else if (nodeIndex != -1) {
			// The task is computed by an edge server
			EisimComputingNode node = (EisimComputingNode) this.nodeList.get(nodeIndex);
			try {
				checkComputingNode(node);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Send this task to this computing node
			((EisimTask) task).setIntermediateOffloadingDestination(node);
			
			/*
			 * Record task arrival for the cluster head (needed in price updates)
			 * + set the final offloading destination for decentralized and centralized topologies
			 * 
			 * (In the current implementation, intermediate offloading destination is only needed in 
			 * hybrid control topology, because it is the only topology that simulates two-phase 
			 * orchestration, where the edge device first orchestrates the task by choosing a destination 
			 * cluster, and after the task has arrived at the cluster head, the head orchestrates it 
			 * inside the cluster)
			 */
			
			// In decentralized and hybrid control topology, the offloading destination server chosen at 
			// this phase is the cluster head for which the task arrival must be recorded
			if ("DECENTRALIZED".equals(this.algorithmName)) {
				
				node.addTaskArrivalInSlot(task);
				
				// Setting the final offloading destination to be the same as the chosen node
				task.setOffloadingDestination(node);
				
			} else if ("HYBRID".equals(this.algorithmName)) {
				
				node.addTaskArrivalInSlot(task);
				
			} else if ("CENTRALIZED".equals(this.algorithmName)) {
				// In centralized control topology, the task arrivals are recorded for the central orchestrator
				this.centralOrchestrator.addTaskArrivalInSlot(task);
				
				// Setting the final offloading destination to be the same as the chosen node
				task.setOffloadingDestination(node);
			}
			
			simLog.deepLog(simulationManager.getSimulation().clock() + ": " + this.getClass() + " Task: " + task.getId()
			+ " assigned to " + node.getType() + " Computing Node: " + node.getId() 
			+ " Distance from device to server: " + task.getEdgeDevice().getMobilityModel().distanceTo(node));
		}
	}

	/**
	 * Checks whether the offloading destination chosen by the orchestration logic is valid.
	 */
	protected void checkComputingNode(EisimComputingNode computingNode) {
		super.checkComputingNode(computingNode);
		if (computingNode.isAP()) {
			throw new IllegalArgumentException(
					getClass().getSimpleName() + " - The forbidden happened! The orchestration "
							+ "algorithm \"" + this.algorithmName + "\" has selected an access "
							+ "point as an offloading destination. Kindly check it.");
		}
	}
	
	/**
	 * Finds the task execution location for an edge device's task. The logic for finding the location depends 
	 * on the algorithm defined in the simulation_parameters.properties file.
	 */
	@Override
	protected int findComputingNode(String[] architectureLayers, Task task) {
		if ("DECENTRALIZED".equals(this.algorithmName) || "HYBRID".equals(this.algorithmName)) {
			return decentralizedAndHybridOrchestration(task);
		} else if ("CENTRALIZED".equals(this.algorithmName)) {
			return centralizedOrchestration(task);
		} else {
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ this.algorithmName + "', please check the simulation parameters file.");
		}
	}
	
	/**
	 * Makes the task offloading decision for an edge device in decentralized and hybrid control 
	 * topologies.
	 * <p>
	 * The possible task execution locations are the local edge device and edge servers that are 
	 * also cluster heads. The method calculates the cost for each option and the location with the
	 * smallest cost is chosen, provided that constraint on energy consumption is satisfied. The 
	 * calculated cost is the weighted sum of delay cost, energy cost and monetary cost. 
	 * <p> 
	 * It is assumed that the nodeList contains only edge nodes (i.e., the simulation is 
	 * run with EDGE_ONLY architecture). The returned integer value determines the offloading 
	 * decision. It can be an integer in range [0, nodeList.size() - 1], giving the index of the 
	 * chosen edge node, or nodeList.size(), which indicates that local edge device is chosen, or 
	 * -1 if no option satisfies the constraints, in which case the task will be dropped.
	 * 
	 * @param task			The task being orchestrated
	 * @return int:			The offloading decision
	 */
	protected int decentralizedAndHybridOrchestration(Task task) {
		EisimComputingNode device = (EisimComputingNode) task.getEdgeDevice();
		List<Double> deviceWeights = device.getDeviceWeights();
		double minCost = getLocalExecutionCost(task, device, deviceWeights);
		int selected = minCost < Double.POSITIVE_INFINITY
				? this.nodeList.size()
				: -1;
		
		List<Double> networkParams = getNetworkParamsForDevice(device);
		double rate = networkParams.get(0); // bits per second
		double transmissionEnergyPerBit = networkParams.get(1); // joules per bit
		double receptionEnergyPerBit = networkParams.get(2); // joules per bit
		
		/****OFFLOADING ENERGY COST****/
		// Note that energy cost is the same for every server, as it only depends on the attributes of 
		// the device and the task
		double energyConsumption = transmissionEnergyPerBit * task.getFileSizeInBits() 
				+ receptionEnergyPerBit * task.getOutputSizeInBits();
		
		double delayConstraint = task.getMaxLatency();
		double energyConstraint = device.getEnergyModel().getBatteryLevelWattHour() * 3600; // Battery level in joules
		
		for (int i = 0; i < this.nodeList.size(); i++) {
			EisimComputingNode server = (EisimComputingNode) this.nodeList.get(i);
			
			// Only checking edge nodes that are servers and cluster heads
			if (!server.isAP() && server.isClusterHead()) {
				/****OFFLOADING DELAY COST****/
				// transmissionDelay = input data size / uplink rate + output data size / downlink rate
				// (Uplink and downlink rates have the same value)
				double transmissionDelay = task.getFileSizeInBits() / rate 
						+ task.getOutputSizeInBits() / rate;
				// Two-way propagation delay between the edge device and server i
				double propagationDelay = calculatePropagationDelay(device, server);
				double processingDelay = task.getLength() / server.getMipsPerCore();
				double queuingDelay = server.getQueueTimeEstimate();
				double taskExecutionDelay = transmissionDelay + propagationDelay + processingDelay + queuingDelay;
				
				/****MONETARY COST****/
				// payment = price per MI * task length in MIs
				//double payment = server.getPrice() * task.getLength();
				
				// The total cost of task execution at the server i is: 
				// delay weight * task execution delay + energy weight * energy consumption + price weight * payment
				double totalCost = deviceWeights.get(0) * taskExecutionDelay / delayConstraint
						+ deviceWeights.get(1) * energyConsumption / energyConstraint
						+ deviceWeights.get(2) * server.getPrice() / 0.01; // payment / (0.01 * task.getLength()); 
																		  // Importance is relative to how much user prefers 
																		  // to pay per MI, here 0.01 per MI for all users
				// Unit agreement; taskExecutionDelay is divided with delayConstraint and energyConsumption with
				// energyConstraint. For payment, there is no strict constraint that should be satisfied, but it 
				// can be assumed that the importance of payment is relative to how much the user prefers to pay per MI.
				// (Importance of delay value is relative to the delay constraint and importance of energy value is 
				// relative to the energy constraint. If the values are not divided by the constraints, importance of
				// delay would be relative to 1 second and importance of energy would be relative to 1 joule)
				
				// If the totalCost is smaller than the current minCost and this option satisfies the constraint,
				// server i is selected as the offloading destination
				if (totalCost < minCost && energyConsumption <= energyConstraint) {
					minCost = totalCost;
					selected = i;
				}
				
			}
		}
		
		return selected;
	}
	
	/**
	 * Makes the task offloading decision for an edge device in centralized control topology.
	 * <p>
	 * The possible task execution locations are the local edge device and any of the edge servers.  
	 * The method calculates the cost for each option and the location with the
	 * smallest cost is chosen, provided that constraint on energy consumption is satisfied. The 
	 * calculated cost is the weighted sum of delay cost, energy cost and monetary cost. 
	 * <p> 
	 * It is assumed that the nodeList contains only edge nodes (i.e., the simulation is 
	 * run with EDGE_ONLY architecture). The returned integer value determines the offloading 
	 * decision. It can be an integer in range [0, nodeList.size() - 1], giving the index of the 
	 * chosen edge node, or nodeList.size(), which indicates that local edge device is chosen, or 
	 * -1 if no option satisfies the constraints, in which case the task will be dropped.
	 * 
	 * @param task			The task being orchestrated
	 * @return int:			The offloading decision
	 */
	protected int centralizedOrchestration(Task task) {
		EisimComputingNode device = (EisimComputingNode) task.getEdgeDevice();
		List<Double> deviceWeights = device.getDeviceWeights();
		double minCost = getLocalExecutionCost(task, device, deviceWeights);
		int selected = minCost < Double.POSITIVE_INFINITY
				? this.nodeList.size()
				: -1;
		
		double delayConstraint = task.getMaxLatency();
		double energyConstraint = device.getEnergyModel().getBatteryLevelWattHour() * 3600; // Battery level in joules
		
		List<Double> networkParams = getNetworkParamsForDevice(device);
		double rate = networkParams.get(0); // bits per second
		double transmissionEnergyPerBit = networkParams.get(1); // joules per bit
		double receptionEnergyPerBit = networkParams.get(2); // joules per bit
		
		/****OFFLOADING ENERGY COST****/
		// Energy cost only depends on the attributes of the device and the task
		double energyConsumption = transmissionEnergyPerBit * task.getFileSizeInBits() 
				+ receptionEnergyPerBit * task.getOutputSizeInBits();
		
		/****MONETARY COST****/
		// payment = price per MI * task length in MIs
		// In centralized control topology, the central orchestrator sets the price for whole edge platform
		//double payment = this.centralOrchestrator.getPrice() * task.getLength();
		
		// Adding energy and monetary cost into total cost
		double partialCost = deviceWeights.get(1) * energyConsumption / energyConstraint 
				+ deviceWeights.get(2) * this.centralOrchestrator.getPrice() / 0.01; // payment / (0.01 * task.getLength());
		
		// If the partialCost in this stage is already larger than the minCost (cost of local execution),
		// there is no need to check the delay cost for offloading as it only adds to the totalCost
		// OR if the estimated energy consumption of offloading is larger than energy constraint, the task
		// cannot be offloaded
		// Hence, the selected execution location is either the local edge device or the task is dropped
		if (partialCost >= minCost || energyConsumption > energyConstraint) {
			return selected;
		}
		
		/****OFFLOADING DELAY COST****/
		// Finding the task execution location (server) for the device's task (would be done by the central orchestrator)
		// For this, the central orchestrator needs to collect information from the servers (e.g., current queue length),
		// and edge device needs to send information to the orchestrator (e.g., task size, delay constraint)
		
		// User can estimate and report this to the central orchestrator
		double transmissionDelay = task.getFileSizeInBits() / rate 
				+ task.getOutputSizeInBits() / rate; 
		
		// Central orchestrator can calculate this as the servers are homogeneous
		double processingDelay = task.getLength() / centralOrchestrator.getMipsPerCore(); 
		
		double partialTaskExecutionDelay = transmissionDelay + processingDelay;
		
		for (int i = 0; i < this.nodeList.size(); i++) {
			EisimComputingNode server = (EisimComputingNode) this.nodeList.get(i);
			
			// Only checking edge nodes that are servers
			if (!server.isAP()) {
				// User could estimate and report its two-way latency to the AP, 
				// central orchestrator could estimate delays inside the platform
				double propagationDelay = calculatePropagationDelay(device, server);
				
				// Central orchestrator uses the queue delays calculated at the beginning of the current price slot 
				// as estimates of the queuing time at the server
				double queuingDelay = server.getQueueDelay();
					
				// Central orchestrator can calculate the total delay for this server option
				double totalTaskExecutionDelay = partialTaskExecutionDelay + propagationDelay + queuingDelay;
					
				double totalCost = partialCost + deviceWeights.get(0) * totalTaskExecutionDelay / delayConstraint;
				// If the totalCost is smaller than the current minCost
				if (totalCost < minCost) {
					minCost = totalCost;
					selected = i;
				}
				// Note that in practice the idea here would be that the central orchestrator returns the
				// server that has the lowest estimated task execution delay along with the estimated
				// delay so the orchestrator does not need to know the device weights of the user
				// (after receiving the delay and server info, the user can check by itself whether the
				// cost of the offloading is larger than that of local execution)
			}
				
		}
		
		return selected;
	}
	
	/**
	 * Calculates the local task execution cost for an edge device.
	 * <p>
	 * The total cost of local task execution is the weighted sum of delay cost and energy cost. 
	 * The method returns this cost if the constraint on energy consumption is satisfied, otherwise 
	 * Double.POSITIVE_INFINITY is returned.
	 * 
	 * @param task				The task being orchestrated
	 * @param device			The edge device to which the task belongs
	 * @param deviceWeights		A list of importance weights for the edge device
	 * @return double:			The cost of the local task execution
	 */
	protected double getLocalExecutionCost(Task task, EisimComputingNode device, List<Double> deviceWeights) {
		double delayConstraint = task.getMaxLatency();
		double energyConstraint = device.getEnergyModel().getBatteryLevelWattHour() * 3600;
		
		/****LOCAL DELAY COST****/
		// Calculating the total sum of task lengths (MIs) over all the tasks currently in the device's queue
		int totalMIs = 0;
		for (Task t : device.getTasksQueue()) {
			totalMIs += t.getLength();
		}
		// local task execution delay = time it takes to execute the task + estimated queue time
		double localTaskExecutionDelay = task.getLength() / device.getMipsPerCore() + totalMIs / device.getTotalMipsCapacity();
		
		/****LOCAL ENERGY COST****/
		// local energy consumption = 
		// max power consumption of the CPU * time it would take to process the task if the whole processing capacity was used
		double localEnergyConsumption = device.getEnergyModel().getMaxActiveConsumption() * task.getLength() / device.getTotalMipsCapacity();
		
		// The monetary cost of local execution is zero
		// Hence, the total cost of local execution is delay weight * local task execution delay + energy weight * local energy consumption
		double localCost = deviceWeights.get(0) * localTaskExecutionDelay / delayConstraint
				+ deviceWeights.get(1) * localEnergyConsumption / energyConstraint;
		
		// If the constraint is satisfied, return localCost; 
		// otherwise the task cannot be processed locally and hence the cost of local execution is infinity (as the task would be dropped)
		return  localEnergyConsumption <= energyConstraint 
				? localCost
				: Double.POSITIVE_INFINITY;
	}
	
	/**
	 * Gets a list of network parameters for an edge device.
	 * <p>
	 * The returned list of network parameters contains three parameters in the following order:
	 * <ol>
	 * 	<li>The data rate in bits per second</li>
	 *  <li>The energy consumed when transmitting data in joules per bit</li>
	 *  <li>The energy consumed when receiving data in joules per bit</li>
	 * </ol>
	 * The parameter values depend on the connectivity type of the edge device (cellular, wifi or ethernet).
	 * 
	 * @param device		 An edge device
	 * @return List<Double>: The network parameters for the edge device
	 */
	protected List<Double> getNetworkParamsForDevice(EisimComputingNode device) {
		String connectivity = device.getEnergyModel().getConnectivityType();
		List<Double> networkParams = new ArrayList<>(3);
		
		if ("cellular".equals(connectivity)) {
			networkParams.add(SimulationParameters.cellularBandwidthBitsPerSecond);
			networkParams.add(SimulationParameters.cellularDeviceTransmissionWattHourPerBit * 3600);
			networkParams.add(SimulationParameters.cellularDeviceReceptionWattHourPerBit * 3600);
		} else if ("wifi".equals(connectivity)) {
			networkParams.add(SimulationParameters.wifiBandwidthBitsPerSecond);
			networkParams.add(SimulationParameters.wifiDeviceTransmissionWattHourPerBit * 3600);
			networkParams.add(SimulationParameters.wifiDeviceReceptionWattHourPerBit * 3600);
		} else {
			networkParams.add(SimulationParameters.ethernetBandwidthBitsPerSecond);
			networkParams.add(SimulationParameters.ethernetWattHourPerBit / 2 * 3600);
			networkParams.add(SimulationParameters.ethernetWattHourPerBit / 2 * 3600);
		}
		return networkParams;
	}
	
	/**
	 * Calculates the propagation delay between an end device and an edge server. 
	 * Note that the returned delay is the two-way delay between the device and the server, 
	 * accounting for both sending the task and receiving the result.
	 * 
	 * @param device	An edge device
	 * @param server	An edge server
	 * @return double:  The two-way latency between the edge device and the edge server.
	 */
	protected double calculatePropagationDelay(EisimComputingNode device, EisimComputingNode server) {
		ComputingNode ap = device.getCurrentUpLink().getDst();
		long id = this.simulationManager.getDataCentersManager().getTopology().getUniqueId(ap.getId(), server.getId()); 
		double manLatency = this.delayMap.get(id);
		double oneWayLatency = manLatency + device.getCurrentUpLink().getLatency();
		return 2 * oneWayLatency;
	}
	
	/**
	 * Saves the delays of the shortest paths between edge nodes (APs and edge servers) in a map 
	 * to use them during orchestration.
	 */
	protected void saveMANdelaysToMap() {
		List<ComputingNode> edgeList = this.simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getEdgeOnlyList(); // A list of edge nodes (APs and edge servers)
		InfrastructureGraph graph = this.simulationManager.getDataCentersManager().getMANTopology();
		FloydWarshallShortestPaths<ComputingNode, NetworkLink> FWalgorithm = new FloydWarshallShortestPaths<>(graph.getGraph());
		for (int i = 0; i < edgeList.size(); i++) {
			ComputingNode from = edgeList.get(i);
			for (int j = 0; j < edgeList.size(); j++) { 
				ComputingNode to = edgeList.get(j);
				double delay = FWalgorithm.getPathWeight(from, to);
				this.delayMap.put(graph.getUniqueId(from.getId(), to.getId()), delay);
			}
		}
	}
	
	@Override
	public void resultsReturned(Task task) {
		// Can be used to do something with the task that has been finished
		// Not needed in this implementation

	}

	@Override
	protected void startInternal() {
		// Called when the simulation starts (e.g., scheduling events for orchestrator)
		// Not needed in this implementation

	}

	@Override
	protected void onSimulationEnd() {
		// Called when the simulation finishes
		// Not needed in this implementation

	}

	@Override
	protected void processEvent(Event e) {
		// Process the scheduled events, if any
		// Not needed in this implementation

	}

}
