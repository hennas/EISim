package com.github.hennas.eisim.defaultclasses;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.github.hennas.eisim.agents.PricingAgent;
import com.github.hennas.eisim.agents.ddpg.DdpgPricingAgent;
import com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNode;
import com.github.hennas.eisim.core.datacentersmanager.ComputingNode;
import com.github.hennas.eisim.core.scenariomanager.SimulationParameters;
import com.github.hennas.eisim.core.simulationengine.Event;
import com.github.hennas.eisim.core.simulationmanager.SimLog;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager;
import com.github.hennas.eisim.core.taskgenerator.Task;
import com.github.hennas.eisim.EisimSimulationParameters;
import com.github.hennas.eisim.helpers.PriceLogger;

/**
 * Extends the {@link DefaultComputingNode} class by adding in clustering and pricing capabilities 
 * for the edge servers.
 * <p>
 * The edge server clusters are determined offline. Cluster information must be provided in the edge 
 * server setting file (edge_datacenters.xml). 
 * The implementation also assumes that all the edge servers have the same specification, that is, 
 * edge_datacenters.xml file should have the same MIPS, cores, RAM and storage, as well as max and idle 
 * consumption for all servers.
 * <p>
 * Only the edge servers that are also cluster heads function as pricing agents. 
 * For these agents, the simulation time is divided into equal sized slots. 
 * At the beginning of each slot, an agent observes the current environment state and makes a price 
 * decision that determines the price for task execution at the cluster for the duration of the slot.
 * During the slot, {@link EisimOrchestrator} records the tasks that are offloaded to the cluster under 
 * the current price. At the end of the slot, the agent calculates and logs the profit it gained with 
 * its price decision. The recorded information about the offloaded tasks is used to calculate the total 
 * revenue and the energy costs of task execution. 
 * The profit (revenue - energy costs) is scaled to get the reward that the agent gets for its price 
 * decision. For the new slot, the agent observes the new state of the environment and makes a price 
 * decision.
 * <p>
 * If the simulation is run in the training mode as specified by {@link EisimSimulationParameters#train}, 
 * at the end of a slot each agent records its experience in the form of a tuple 
 * (state, action, reward, newState) into an experience replay and updates its models with a minibatch 
 * sampled from the experience replay.
 * 
 * @see DefaultComputingNode
 * @see EisimOrchestrator
 * @see PricingAgent
 * 
 * @author Henna Kokkonen
 *
 */
public class EisimComputingNode extends DefaultComputingNode {
	// The name of the currently simulated orchestration algorithm
	protected String algorithm;
	
	// The following are only used for EDGE_DEVICE type nodes
	protected Random random; // Used to generate random weight values for edge devices
	protected List<Double> deviceWeights;
	
	// The following are only used for EDGE_DATACENTER type nodes
	protected boolean isAP = false;
	protected int cluster = -1;
	protected boolean clusterHead = false;
	protected List<EisimComputingNode> clusterMembers = new ArrayList<>();
	protected int clusterSize;
	
	// The following are only used for EDGE_DATACENTER type nodes that are also cluster heads
	protected static final int PRICE_UPDATE_INTERVAL = 5; // the length of a slot in seconds
	protected static final int PRICE_UPDATE = 3; // price update event
	protected static final int RECORD_QUEUE_DELAY_ESTIMATE = 4; // Event for edge servers 
																// (needed only in centralized control topology)
	protected static float maxPrice = 1f;
	protected static float minPrice = 0f;
	protected static double energyCostCoefficient = 1e-3; // Determines a cost per joule, 
														  // which is used to calculate the energy cost in reward calculation
	protected static int stateSpaceDim = 2;
	
	protected static Class<? extends PricingAgent> pricingAgentClass;
	protected PricingAgent agent; // Encapsulates the neural networks and experience memory, 
								  // carries out training and makes action decisions
	protected PriceLogger priceLog; // Used for logging prices and profits
	protected int pricingSteps = 0; // Records how many pricing decisions have been made since the beginning of the simulation
	protected float rewardScale; // Used to scale the profit in order to produce reward for the agents
	protected float queueScale; // Used to scale the avg queue length state variable
	protected float arrivalRateScale; // Used to scale the avg arrival rate state variable
	protected float currentPrice; // The effective price during a slot for the whole cluster
	protected double currentQueueTimeEstimate; // An estimate of queue time for whole cluster during a slot 
											   // (needed in decentralized and hybrid control topologies)
	protected double queueDelayEstimate; // An estimate of a queue time for one edge server (needed in centralized control topology)
	protected INDArray previousState; // Records the previous state, which is needed during training at the beginning of a new slot 
									  // to save the experience tuple (state, action, reward, newState)
	// Updating the following total counts is the responsibility of the EisimOrchestrator
	protected int totalTasksArrivedInSlot = 0; // Needed when observing the new state at the beginning of a slot (see getNewState())
	protected int totalMIsInSlot = 0; // Needed when calculating the profit for a slot (see getProfit())
	
	static {
		// Set your own custom pricing agent class here
		setCustomPricingAgentClass(DdpgPricingAgent.class);
		// Another option for setting custom pricing agent class is to extend this class and
		// set the custom class in the static block of the child class (no need to modify this)
	}
	
	/**
	 * Initialize an access point without any computational capabilities.
	 * 
	 * @param simulationManager The simulation manager that links between the different modules.
	 */
	public EisimComputingNode(SimulationManager simulationManager) {
		this(simulationManager, 0, 0, 0, 0);
		this.setAsAP(true);
		// The constructor of super class assigns the node as sensor when 
		// it has no computational capabilities. 
		// The following line reverses this.
		this.setAsSensor(false); 
	}

	/**
	 * Initialize an edge server with cluster information.
	 * 
	 * @param simulationManager The simulation manager that links between the different modules
	 * @param mipsPerCore		The processing capacity of one CPU core in Million Instructions Per Second
	 * @param numberOfCPUCores	The total number of CPU cores
	 * @param storage 			The total amount of storage in Megabytes
	 * @param ram				The total amount of RAM in Megabytes
	 * @param cluster			The cluster of this server (non-negative integer)
	 * @param clusterHead		Whether this server is a cluster head or not
	 */
	public EisimComputingNode(SimulationManager simulationManager, double mipsPerCore, int numberOfCPUCores,
			double storage, double ram, int cluster, boolean clusterHead) {
		this(simulationManager, mipsPerCore, numberOfCPUCores, storage, ram);
		this.setCluster(cluster);
		this.setAsClusterHead(clusterHead);
	}
	
	/**
	 * Initialize other types of nodes (cloud data centers and edge devices, edge servers 
	 * with no cluster information).
	 * 
	 * @param simulationManager The simulation manager that links between the different modules
	 * @param mipsPerCore		The processing capacity of one CPU core in Million Instructions Per Second
	 * @param numberOfCPUCores	The total number of CPU cores
	 * @param storage 			The total amount of storage in Megabytes
	 * @param ram				The total amount of RAM in Megabytes
	 */
	public EisimComputingNode(SimulationManager simulationManager, double mipsPerCore, int numberOfCPUCores,
			double storage, double ram) {
		super(simulationManager, mipsPerCore, numberOfCPUCores, storage, ram);
		this.algorithm = simulationManager.getScenario().getStringOrchAlgorithm();
		this.random = new Random();
		this.random.setSeed(((EisimSimulationManager) simulationManager).seedGenerator.nextLong());
	}
	
	/**
	 * Allows to use a custom pricing agent class in the simulation. The class must extend
	 * the {@link PricingAgent} class.
	 * 
	 * @param pricingAgentClass The custom pricing agent class to use
	 */
	public static void setCustomPricingAgentClass(Class<? extends PricingAgent> agentClass) {
		pricingAgentClass = agentClass;
	}
	
	@Override
	public void startInternal() {
		// Only for computing nodes that are not APs
		if (!this.isAP()) {
			super.startInternal(); // Starts the UPDATE_STATUS event (for static energy consumption measurement and mobility updates), 
								   // generates the mobility path
			
			if (this.getType() == SimulationParameters.TYPES.EDGE_DATACENTER) {
				// Updating the cluster member list
				this.simulationManager.getSimulationLogger().deepLog("Finding cluster members for edge server " + this.getName());
				findEdgeClusterMembers();
				
				/* 
				 * If the orchestration algorithm is CENTRALIZED, each edge server will save their queue delay estimate
				 * at the beginning of every price slot. This information is used by the central orchestrator when it 
				 * allocates tasks. The idea here is to mimic the fact that central orchestrator will collect status 
				 * information from the edge servers only at the beginning of each price slot, and then use this 
				 * information when it decides task execution locations.
				 */
				if ("CENTRALIZED".equals(this.algorithm)) {
					scheduleNow(this, RECORD_QUEUE_DELAY_ESTIMATE);
				}
				
				// If the server is a cluster head, it must initialize a pricing agent and start the PRICE_UPDATE event
				if (this.isClusterHead()) {
					this.simulationManager.getSimulationLogger().deepLog("Starting price updates for edge server " + this.getName());
					initializeAgent();
					scheduleNow(this, PRICE_UPDATE);
				}
			}
			
			// If the node is an end device, it must generate its individual weights for making offloading decisions 
			if (this.getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
				this.simulationManager.getSimulationLogger().deepLog("Generating device weights for edge device " + this.getId());
				generateDeviceWeights();
			}
		}
	}
	
	@Override
	public void processEvent(Event e) {
		switch (e.getTag()) {
		case PRICE_UPDATE:
			updatePrice();
			// No need to do price updates anymore if we are just waiting for the tasks to finish (simClock > simDuration)
			if (this.simulationManager.getSimulation().clock() < SimulationParameters.simulationDuration) {
				schedule(this, PRICE_UPDATE_INTERVAL, PRICE_UPDATE);
			}
			break;
		case RECORD_QUEUE_DELAY_ESTIMATE:
			this.setQueueDelay(calculateQueueDelayEstimateForServer());
			if (this.simulationManager.getSimulation().clock() < SimulationParameters.simulationDuration) {
				// Happens at the same interval as the PRICE_UPDATE event
				schedule(this, PRICE_UPDATE_INTERVAL, RECORD_QUEUE_DELAY_ESTIMATE);
			}
			break;
		default:
			super.processEvent(e);
			break;
		}
	}
	
	@Override
	public void onSimulationEnd() {
		// Save the price log, models and experience replay for the edge servers that are also cluster heads
		if (this.getType() == SimulationParameters.TYPES.EDGE_DATACENTER && this.isClusterHead()) {
			SimLog.println(this.getClass().getSimpleName() 
					+ " - Saving price log and agent state for edge server " + this.getName());
			try {
				this.priceLog.saveLog();
				this.agent.saveAgentState();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
	}
	
	/**
	 * Returns true if the node is an AP.
	 * 
	 * @return boolean: whether the node is an AP or not
	 */
	public boolean isAP() {
		return this.isAP;
	}
	
	/**
	 * Sets whether the node is an access point or not. 
	 * Setting with {@code true} should only be done for EDGE_DATACENTER type nodes.
	 * 
	 * @param isAP True if the node is an AP, otherwise false
	 */
	public void setAsAP(boolean isAP) {
		this.isAP = isAP;
	}
	
	/**
	 * Sets the cluster of the node.
	 * 
	 * @param cluster Non-negative integer that determines the cluster of the node
	 */
	public void setCluster(int cluster) {
		this.cluster = cluster;
	}
	
	/**
	 * Get the cluster of the node.
	 * 
	 * @return int: integer that determines the cluster of the node
	 */
	public int getCluster() {
		return this.cluster;
	}
	
	/**
	 * Returns true if the node is a cluster head.
	 * 
	 * @return boolean: whether the node is a cluster head or not
	 */
	public boolean isClusterHead() {
		return this.clusterHead;
	}
	
	/**
	 * Sets whether the node is a cluster head or not.
	 * 
	 * @param clusterHead True if the node is a cluster head, otherwise false
	 */
	public void setAsClusterHead(boolean clusterHead) {
		this.clusterHead = clusterHead;
	}
	
	/**
	 * Gets the list of the node's cluster members. 
	 * This list does not include the node itself by default.
	 * 
	 * @return List<EisimComputingNode>: List of the cluster members
	 */
	public List<EisimComputingNode> getClusterMembers() {
		return this.clusterMembers;
	}
	
	/**
	 * Adds the given computing node into this node's cluster members.
	 * 
	 * @param node The computing node to be added
	 */
	public void addClusterMember(EisimComputingNode node) {
		this.clusterMembers.add(node);
	}
	
	/**
	 * Removes the given computing node from this node's cluster member list.
	 * Note that only the first occurrence is removed.
	 * Returns true if the list contained the given node, otherwise returns false.
	 * 
	 * @param node 		The computing node to be removed
	 * @return boolean: True if the cluster member list contained the given computing node and it was removed. 
	 * 					False if the list did not contain the given computing node.
	 */
	public boolean removeClusterMember(EisimComputingNode node) {
		return this.clusterMembers.remove(node);
	}
	
	/**
	 * Gets an edge device's weights for offloading decision making.
	 * 
	 * @return List<Double>: A list of weight values
	 */
	public List<Double> getDeviceWeights() {
		return this.deviceWeights;
	}
	
	/**
	 * Gets the current price for task execution in the cluster. Should be only used for cluster heads.
	 * 
	 * @return float: The current price per MI
	 */
	public float getPrice() {
		return this.currentPrice;
	}
	
	/**
	 * Sets the current price for task execution in the cluster. Should be only used for cluster heads.
	 * 
	 * @param price The new price per MI
	 */
	public void setPrice(float price) {
		this.currentPrice = price;
	}
	
	/**
	 * Gets the current queue time estimate inside a cluster for the duration of the slot. 
	 * Should be only used for cluster heads in decentralized and hybrid control topologies. 
	 * (Edge devices use this information when making offloading decisions)
	 * 
	 * @return double: A (rough) estimate of the queue time at the cluster
	 */
	public double getQueueTimeEstimate() {
		return this.currentQueueTimeEstimate;
	}
	
	/**
	 * Sets the current queue time estimate inside a cluster for the duration of the slot. 
	 * Should be only used for cluster heads in decentralized and hybrid control topologies.
	 * 
	 * @param estimate A (rough) estimate of the queue time at the cluster
	 */
	public void setQueueTimeEstimate(double estimate) {
		this.currentQueueTimeEstimate = estimate;
	}
	
	/**
	 * Gets the current queue delay estimate at the edge server for the duration of the slot. 
	 * Should be only used for edge servers in centralized control topology. 
	 * (Central orchestator uses this information when making task allocation decisions)
	 * 
	 * @return double: A (rough) estimate of the queue time at the server
	 */
	public double getQueueDelay() {
		return this.queueDelayEstimate;
	}
	
	/**
	 * Sets the current queue delay estimate at the edge server for the duration of the slot. 
	 * Should be only used for edge servers in centralized control topology. 
	 * 
	 * @param estimate A (rough) estimate of the queue time at the server
	 */
	public void setQueueDelay(double estimate) {
		this.queueDelayEstimate = estimate;
	}
	
	/**
	 * Adds information about the tasks that are offloaded to a cluster during a slot. 
	 * Should be only used for cluster heads that make the price decisions.
	 * 
	 * @param task The offloaded task
	 */
	public void addTaskArrivalInSlot(Task task) {
		this.totalTasksArrivedInSlot++;
		this.totalMIsInSlot += task.getLength();
	}
	
	/**
	 * Finds the cluster members of an edge server node and adds them to the cluster member list. 
	 * Also records the size of the cluster. 
	 * The created list does not include the edge server itself, but the recorded cluster size does.
	 */
	protected void findEdgeClusterMembers() {
		// Looking for cluster members only if this node has an assigned cluster (non-negative cluster integer)
		if (this.getCluster() > -1) {
			// List of all edge nodes (includes APs)
			List<ComputingNode> edgeNodes = this.simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeOnlyList();
			for (int i = 0; i < edgeNodes.size(); i++) {
				EisimComputingNode node = (EisimComputingNode) edgeNodes.get(i);
				if (node.getName() != this.getName() && node.getCluster() == this.getCluster()) {
					this.addClusterMember(node);
				}
			}
			// Saving the size of the cluster, including the node itself
			this.clusterSize = this.getClusterMembers().size() + 1;
		}
	}
	
	/**
	 * Generates the importance weights for edge devices. These are used in the orchestration of the 
	 * tasks.
	 * <p>
	 * Each edge device needs three weights: the first weight corresponds to the importance of latency, 
	 * the second to the importance of energy consumption, 
	 * and the third is the importance of the payment in the offloading decision.
	 * <p>
	 * The weights add up to one. 
	 * Hence, the possible weight values lie on a triangle formed by the points (1,0,0), (0,1,0) and 
	 * (0,0,1) inside a unit cube. 
	 * The coordinates of each point on the triangle correspond to the weight values. 
	 * This method generates the weight values by randomly sampling a point from the triangle.
	 */
	protected void generateDeviceWeights() {
		this.deviceWeights = new ArrayList<>(3);
		
		double[] randomValues = {this.random.nextDouble(), this.random.nextDouble()}; // Two random values both uniformly sampled from [0,1)
        Arrays.sort(randomValues); // Sorting the random values in ascending order
        
        double weight1 = randomValues[0]; // The smaller of the two random values is set as the first weight
        double weight2 = 1 - randomValues[1]; // The second weight is the larger random value subtracted from 1
        double weight3 = randomValues[1] - randomValues[0]; // The final weight is the difference between the larger and smaller random values
		
		this.deviceWeights.add(weight1); // Weight for latency
		this.deviceWeights.add(weight2); // Weight for energy consumption
		this.deviceWeights.add(weight3); // Weight for payment
	}
	
	/**
	 * Initializes the {@link PricingAgent} instance and the {@link PriceLogger} instance for 
	 * EDGE_DATACENTER type nodes that are also cluster heads. 
	 * Also the scales for the reward and state variables are initialized according to the used 
	 * orchestration algorithm.
	 */
	protected void initializeAgent() {
		// Creating the agent instance
		Constructor<?> pricingAgentConstructor;
		try {
			pricingAgentConstructor = pricingAgentClass.getConstructor(String.class, int.class, 
					float.class, float.class, SimulationManager.class);

			this.agent = (PricingAgent) pricingAgentConstructor.newInstance(this.getName(), 
					stateSpaceDim, minPrice, maxPrice, this.simulationManager);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//this.agent = new PricingAgent(this.getName(), stateSpaceDim, minPrice, maxPrice, this.simulationManager);
		
		// Creating the price logger for the agent
		this.priceLog = new PriceLogger(this.simulationManager, this.getName());
		
		// Initialize scales based on the algorithm
		if ("DECENTRALIZED".equals(this.algorithm)) {
			this.rewardScale = 1e-3f;
			this.queueScale = 1e-3f;
			this.arrivalRateScale = 1e-2f;
		} else if ("HYBRID".equals(this.algorithm)) {
			this.rewardScale = 1e-4f;
			this.queueScale = 1e-2f;
			this.arrivalRateScale = 1e-3f;
		} else if ("CENTRALIZED".equals(this.algorithm)) {
			this.rewardScale = 1e-4f;
			this.queueScale = 1e-1f;
			this.arrivalRateScale = 1e-3f;
		}
	}
	
	/**
	 * Updates the price at the beginning of a new slot.
	 * <p>
	 * This method is called at the beginning of every new slot when handling the {@code PRICE_UPDATE} 
	 * event (only edge servers that are also cluster head execute this).
	 */
	protected void updatePrice() {
		// Get the profit for the previous slot
		double profit = getProfit(); 
		
		// Log the price, profit and state of the previous slot
		this.priceLog.addLine(this.simulationManager.getSimulation().clock(), this.getPrice(), profit, this.previousState); 
		
		//float reward = (float) (Math.signum(profit)*Math.log(1 + Math.abs(profit)));
		float reward = (float) (this.rewardScale * profit);
		
		// Observe the new state at the beginning of a new slot
		INDArray newState = getNewState();
		
		// If the simulation is run in training mode, the experience tuple is saved and the models updated
		if (EisimSimulationParameters.train && this.pricingSteps > 0) {
			agent.learn(this.previousState, this.getPrice(), reward, newState);
		}
		
		// Make the price decision for the new slot
		float newPrice = this.pricingSteps >= EisimSimulationParameters.randomDecisionSteps // If the number of random pricing steps 
																							 // specified in the settings has been exceeded
				? agent.act(newState) // then get the new price by inputting the state into actor network
				: agent.act(); // otherwise get a price that is uniformly sampled from the price range.
		this.setPrice(newPrice); // Set the decision as the current price
		this.pricingSteps++; // Record the number of pricing steps done
		
		// Store the current state for the next slot
		this.previousState = newState; 
		
		// Calculate and set the queue time estimate for the new slot 
		// (only needed if the orchestration algorithm is DECENTRALIZED or HYBRID)
		if ("DECENTRALIZED".equals(this.algorithm) || "HYBRID".equals(this.algorithm)) {
			this.setQueueTimeEstimate(this.calculateQueueTimeEstimateForSlot()); 
		}
		
		// Reset the task arrival related counts for the new slot
		this.totalTasksArrivedInSlot = 0; 
		this.totalMIsInSlot = 0; 
	}
	
	/**
	 * Gets the new state at the beginning of a slot. This state is used to make the next price decision.
	 * 
	 * @return INDArray: INDArray presentation of the state
	 */
	protected INDArray getNewState() {
		float[][] state = new float[1][stateSpaceDim];
		
		int totalQueueLength = 0;
		for (EisimComputingNode node : this.getClusterMembers()) {
			totalQueueLength += node.getTasksQueue().size();
		}
		
		// Average CPU utilization? this.totalMIsInSlot / (this.getTotalMipsCapacity() * this.clusterSize * PRICE_UPDATE_INTERVAL)
		double avgTaskQueueLength = (double) (totalQueueLength + this.getTasksQueue().size()) / this.clusterSize; // Average task queue length in the cluster
		double avgArrivalRate = (double) this.totalTasksArrivedInSlot / PRICE_UPDATE_INTERVAL; // Average arrival rate of tasks
		state[0][0] = (float) (this.queueScale * avgTaskQueueLength);
		//state[0][1] = this.getPrice(); // Pricing decision from previous slot
		state[0][1] = (float) (this.arrivalRateScale * avgArrivalRate);
		return Nd4j.create(state);
	}
	
	/**
	 * Calculates the profit for the previous slot at the start of the new slot.
	 * <p>
	 * The profit of the cluster = total revenue - varying energy cost - fixed energy cost.
	 * 
	 * @return double: Scalar profit value
	 */
	protected double getProfit() {
		double maxConsumption = this.getEnergyModel().getMaxActiveConsumption();
		double idleConsumption = this.getEnergyModel().getIdleConsumption();
		
		double revenue = this.getPrice() * this.totalMIsInSlot; // price per MI * total MIs offloaded to this cluster during slot
		
		// Varying energy cost: cost per J * (CPU power consumption at 100% - idle consumption) (W; cluster's total) * 
		// how long it takes to process all offloaded tasks with cluster's max capacity (s)
		// Note that the calculation below assumes that all the servers in a cluster have the same specification
		double varyingEnergyCost = energyCostCoefficient * (maxConsumption - idleConsumption) * this.totalMIsInSlot / this.getTotalMipsCapacity();
		// Fixed energy cost: cost per J * idle energy consumption in the cluster during the price slot
		double fixedEnergyCost = energyCostCoefficient * idleConsumption * this.clusterSize * PRICE_UPDATE_INTERVAL ;
		double profit = revenue - varyingEnergyCost - fixedEnergyCost; // The profit gained as a result of the price decision
		return profit;
	}
	
	/**
	 * Calculates a rough estimate of the queuing time inside a cluster for the new slot. 
	 * Edge devices use this information in their offloading decisions in decentralized and hybrid 
	 * control topologies.
	 * <p>
	 * The queue time estimate is the total number of MIs summed over all task queues and tasks in the 
	 * cluster divided by the total processing capacity (MIPS) of the cluster.
	 * <p>
	 * This method is called only for edge servers that are also cluster heads during the handling of 
	 * {@code PRICE_UPDATE} event.
	 * 
	 * @return double: A rough estimate of the queuing time at the cluster during the next slot
	 */
	protected double calculateQueueTimeEstimateForSlot() {
		int totalMIs = 0;
		// Summing task lengths over the task queues and tasks of cluster members
		for (EisimComputingNode node : this.getClusterMembers()) {
			for (Task task : node.getTasksQueue()) {
				totalMIs += task.getLength();
			}
		}
		// Adding the task lengths of this node
		for (Task task : this.getTasksQueue()) {
			totalMIs += task.getLength();
		}
		double estimate = totalMIs / (this.clusterSize * this.getTotalMipsCapacity()); // servers are homogeneous in capacity
		return estimate;
	}
	
	/**
	 * Calculates a rough estimate of the queuing time at an edge server for the new slot. 
	 * The central orchestrator uses this information when allocating tasks in centralized control 
	 * topology.
	 * <p>
	 * This method is called for edge servers at the beginning of every new price slot when handling the 
	 * {@code RECORD_QUEUE_DELAY_ESTIMATE} event.
	 * 
	 * @return double: A rough estimate of the queuing delay at the edge server during the next slot
	 */
	protected double calculateQueueDelayEstimateForServer() {
		int taskQueueLengthInMIs = 0; 
		for (Task task : this.getTasksQueue()) {
			taskQueueLengthInMIs += task.getLength();
		}
		double queuingDelay = taskQueueLengthInMIs / this.getTotalMipsCapacity();
		return queuingDelay;
	}

}
