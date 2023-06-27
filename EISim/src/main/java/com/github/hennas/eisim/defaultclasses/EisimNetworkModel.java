package com.github.hennas.eisim.defaultclasses;

import com.github.hennas.eisim.core.network.DefaultNetworkModel;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager;
import com.github.hennas.eisim.core.datacentersmanager.ComputingNode;
import com.github.hennas.eisim.core.network.TransferProgress;
import com.github.hennas.eisim.core.simulationengine.Event;
import com.github.hennas.eisim.core.taskgenerator.Task;

/**
 * Extends the {@link DefaultNetworkModel} class by adding in event handling needed by the hybrid
 * control topology.
 * <p>
 * The hybrid control topology requires two-phase orchestration, where the device first sends the task
 * to the cluster head, after which the cluster head decides which cluster node executes the task and 
 * sends the task to the chosen cluster node. This class adds in the network events and event handling 
 * needed by this control topology while ensuring that the same implementation can be used in 
 * decentralized and centralized control topologies.
 * 
 * @see DefaultNetworkModel
 * @see EisimSimulationManager
 * 
 * @author Henna Kokkonen
 *
 */
public class EisimNetworkModel extends DefaultNetworkModel {

	public static final int SEND_REQUEST_FROM_CLUSTER_HEAD_TO_DESTINATION = 8;

	public EisimNetworkModel(SimulationManager simulationManager) {
		super(simulationManager);
	}
	
	@Override
	public void processEvent(Event e) {
		switch (e.getTag()) {
		case SEND_REQUEST_FROM_CLUSTER_HEAD_TO_DESTINATION:
			sendRequestFromClusterHeadToDest((EisimTask) e.getData());
			break;			
		default:
			super.processEvent(e);
			break;
		}

	}
	
	@Override
	public void sendRequestFromOrchToDest(Task task) {
		EisimTask cTask = (EisimTask) task;
		if (cTask.getOrchestrator() != cTask.getIntermediateOffloadingDestination()
				&& cTask.getIntermediateOffloadingDestination() != cTask.getEdgeDevice())

			send(cTask.getOrchestrator(), cTask.getIntermediateOffloadingDestination(), cTask, cTask.getFileSizeInBits(),
					TransferProgress.Type.TASK);
		else // The device will execute the task locally
			executeTaskOrDownloadContainer(
					new TransferProgress(cTask, cTask.getFileSizeInBits(), TransferProgress.Type.TASK));
	}
	
	public void sendRequestFromClusterHeadToDest(EisimTask task) {
		if (task.getIntermediateOffloadingDestination() != task.getOffloadingDestination()) {
			send(task.getIntermediateOffloadingDestination(), task.getOffloadingDestination(), task, 
					task.getFileSizeInBits(), TransferProgress.Type.TASK);
		} else { // Cluster head executes the task
			executeTaskOrDownloadContainer(
					new TransferProgress(task, task.getFileSizeInBits(), TransferProgress.Type.TASK));
		}
	}
	
	@Override
	protected void transferFinished(TransferProgress transfer) {
		if (transfer.getTransferType() == TransferProgress.Type.TASK) {
			EisimTask task = (EisimTask) transfer.getTask();
			
			// in case the task was sent from device to the intermediate offloading destination
			if (transfer.getVertexList().get(0) == task.getIntermediateOffloadingDestination()) {
				updateEdgeDevicesRemainingEnergy(transfer, task.getEdgeDevice(),
						task.getIntermediateOffloadingDestination());
			}
			
			// If the final offloading destination is null, the task must be orchestrated inside the cluster (hybrid)
			if (task.getOffloadingDestination() == ComputingNode.NULL) {
				taskReceivedByClusterHead(transfer);
			} else { // otherwise there is no two-phase orchestration (decentralized or centralized) 
				     // or the task has been orchestrated and sent inside the cluster (hybrid)
				executeTaskOrDownloadContainer(transfer);
			}
			
		} else { // Use the default implementation to handle everything else
			super.transferFinished(transfer);
		}
	}
	
	protected void taskReceivedByClusterHead(TransferProgress transfer) {
		scheduleNow(simulationManager, EisimSimulationManager.SEND_TASK_FROM_CLUSTER_HEAD_TO_DESTINATION, transfer.getTask());
	}

}
