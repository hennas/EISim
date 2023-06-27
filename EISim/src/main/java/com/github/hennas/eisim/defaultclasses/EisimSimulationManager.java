package com.github.hennas.eisim.defaultclasses;

import java.util.Random;

import com.github.hennas.eisim.EisimSimulationParameters;
import com.github.hennas.eisim.core.scenariomanager.Scenario;
import com.github.hennas.eisim.core.simulationengine.PureEdgeSim;
import com.github.hennas.eisim.core.simulationmanager.DefaultSimulationManager;
import com.github.hennas.eisim.core.simulationmanager.SimLog;
import com.github.hennas.eisim.core.datacentersmanager.ComputingNode;
import com.github.hennas.eisim.core.network.NetworkModel;
import com.github.hennas.eisim.core.simulationengine.Event;
import com.github.hennas.eisim.core.taskgenerator.Task;

/**
 * Extends the {@link DefaultSimulationManager} class by adding in functionalities needed by the 
 * decentralized, hybrid and centralized simulation workflows.
 * <p>
 * In decentralized control topology, the workflow is as follows:
 * <ol>
 * 	<li>An edge device (user) generates a task</li>
 * 	<li>The user orchestrates its task by deciding the offloading destination (local execution or 
 * 		an edge server)</li>
 * 		<ul>
 * 		 <li>Each edge server decides its own price and calculates estimated queue time for the slot</li>
 *  	 <li>Using the price and estimated queue time information, as well as its own individual 
 *  		 weights factors, the user decides the edge server to which it offloads the task (or decides
 *  		 to execute the task locally)</li>
 * 		</ul>
 * 	<li>The task is sent to the chosen server</li>
 * 	<li>The server executes the task</li>
 * 	<li>Results are sent back to the user</li>
 * </ol>
 * <p>
 * In hybrid control topology, the workflow is as follows:
 * <ol>
 * 	<li>An edge device (user) generates a task</li>
 * 	<li>The user orchestrates its task by deciding the offloading destination (local execution or 
 * 		a cluster)</li>
 * 		<ul>
 * 		 <li>Each cluster head server decides the price for the cluster and calculates the estimated 
 * 			 queue time inside the cluster for the slot</li>
 *  	 <li>Using the price and estimated queue time information, as well as its own individual 
 *  		 weights factors, the user decides the cluster to which it offloads the task (or decides
 *  		 to execute the task locally)</li>
 * 		</ul>
 * 	<li>The task is sent to the chosen cluster head</li>
 * 	<li>The cluster head allocates (orchestrates) the task inside the cluster</li>
 *  <li>The task is sent to the chosen cluster server for execution (can be the cluster head itself)</li>
 *  <li>The server executes the task</li>
 * 	<li>Results are sent back to the user</li>
 * </ol>
 * <p>
 * In centralized control topology, the workflow is as follows:
 * <ol>
 * 	<li>An edge device (user) generates a task</li>
 * 	<li>The user orchestrates its task by deciding the whether it offloads or not</li>
 * 		<ul>
 * 		 <li>User sends an offloading request to the central orchestrator. The request has information
 *  		 about the task and the user, e.g., task length, file size and user's estimated task 
 *  		 transmission time</li>
 *  	 <li>Using the information from the user and edge servers, the central orchestrator decides
 *  		 the task execution server for the user, sending information about the chosen server and
 *  		 estimated execution delay to the user. The central orchestrator also decides the price 
 *  		 for execution on the platform</li>
 *  	 <li>Using the information from the central orchestrator, user decides whether it offloads to
 *  		 the given server or not</li>
 * 		</ul>
 * 	<li>The task is sent to the chosen server</li>
 *  <li>The server executes the task</li>
 * 	<li>Results are sent back to the user</li>
 * </ol>
 * <p>
 * Note that the sending of the offloading request and the corresponding response is not simulated for 
 * the centralized control topology due to the minuscule size of such requests and responses. 
 * Hence, the simulated workflows of centralized and decentralized topologies are the same, and these 
 * topologies can use the implementation of {@code DefaultSimulationManager} pretty much as is. 
 * This class mainly adds in the simulation events and event handling needed by the hybrid control 
 * topology, while ensuring that the same implementation can be used in decentralized and centralized 
 * control topologies.
 * 
 * @see DefaultSimulationManager
 * @see EisimOrchestrator
 * 
 * @author Henna Kokkonen
 *
 */
public class EisimSimulationManager extends DefaultSimulationManager {

	public static final int SEND_TASK_FROM_CLUSTER_HEAD_TO_DESTINATION = 10;
	
	/**
	 * Seeds all the random number generators used in the simulation.
	 */
	public Random seedGenerator;
	
	/**
	 * Initializes the EISim simulation manager.
	 * 
	 * @param simLog       The simulation logger
	 * @param pureEdgeSim  The simulation engine
	 * @param simulationId The simulation ID
	 * @param iteration    Which simulation run
	 * @param scenario     The scenario is composed of the algorithm and architecture that are being used, 
	 * 					   and the number of edge devices.
	 */
	public EisimSimulationManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration,
			Scenario scenario) {
		super(simLog, pureEdgeSim, simulationId, iteration, scenario);
		// Initialize the seed generator
		seedGenerator = new Random();
		if (EisimSimulationParameters.useSeed) {
			seedGenerator.setSeed(EisimSimulationParameters.seed);
		}
	}
	
	@Override
	public void processEvent(Event e) {
		EisimTask task = (EisimTask) e.getData();
		switch (e.getTag()) {
		case SEND_TASK_FROM_CLUSTER_HEAD_TO_DESTINATION:
			sendFromClusterHeadToDestination(task);
			break;
		default:
			super.processEvent(e);
			break;
		}

	}
	
	@Override
	public boolean taskFailed(Task task, int phase) {
		if (phase == 0 && task.getEdgeDevice().isDead()) {
			simLog.incrementNotGeneratedBeacuseDeviceDead();
			task.setFailureReason(Task.FailureReason.NOT_GENERATED_BECAUSE_DEVICE_DEAD);
			return setFailed(task, phase);
		} else { // Use the default implementation to handle everything else
			return super.taskFailed(task, phase);
		}
	}
	
	@Override
	protected void sendFromOrchToDestination(Task task) {
		if (taskFailed(task, 1))
			return;
		
		// Find the best resource node for executing the task.
		edgeOrchestrator.orchestrate(task);

		// Stop if no resource is available for this task, the offloading is failed.
		if (((EisimTask) task).getIntermediateOffloadingDestination() == ComputingNode.NULL) {

			task.setFailureReason(Task.FailureReason.NO_OFFLOADING_DESTINATIONS);
			simLog.incrementTasksFailedLackOfRessources(task);
			tasksCount++;
			return;
		}
		
		if (task.getOffloadingDestination() != ComputingNode.NULL) {
			simLog.taskSentFromOrchToDest(task);
		}
		
		// Send the task from the orchestrator to the destination
		scheduleNow(getNetworkModel(), NetworkModel.SEND_REQUEST_FROM_ORCH_TO_DESTINATION, task);

	}
	
	protected void sendFromClusterHeadToDestination(EisimTask task) {
		EisimComputingNode offloadingDest = (EisimComputingNode) task.getIntermediateOffloadingDestination();
		
		if (offloadingDest.isClusterHead() && task.getOffloadingDestination() == ComputingNode.NULL) {
			((EisimOrchestrator) edgeOrchestrator).orchestrateInsideCluster(task, offloadingDest);
		} else {
			String msg = getClass().getSimpleName() + " - The forbidden happened! SEND_TASK_FROM_CLUSTER_HEAD_TO_DESTINATION"
					+ " event was scheduled for a task under the following illegal conditions:\n";
			
			if (!offloadingDest.isClusterHead()) {
				msg += "-The task did not arrive at a cluster head\n";
			}
			
			if (task.getOffloadingDestination() != ComputingNode.NULL) {
				msg += "-The task already had a final offloading destination assigned\n";
			}
			
			msg += "(Task with id" + task.getId() + " arrived at " + offloadingDest.getType() + " with id " 
					+ offloadingDest.getId() + " under the \"" + getScenario().getStringOrchAlgorithm() 
					+ "\" orchestration algorithm, when the event was illegally scheduled.)";
					
			throw new IllegalArgumentException(msg);
		}

		simLog.taskSentFromOrchToDest(task);
		
		// Send the task from the cluster head to the destination
		scheduleNow(getNetworkModel(), EisimNetworkModel.SEND_REQUEST_FROM_CLUSTER_HEAD_TO_DESTINATION, task);

	}

}
