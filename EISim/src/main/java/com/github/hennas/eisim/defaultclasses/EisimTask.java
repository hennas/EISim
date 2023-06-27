package com.github.hennas.eisim.defaultclasses;

import com.github.hennas.eisim.core.taskgenerator.DefaultTask;
import com.github.hennas.eisim.core.datacentersmanager.ComputingNode;

/**
 * Extends the {@link DefaultTask} class by adding information about the task's intermediate offloading 
 * destination (which must be a cluster head). This information is mainly relevant for hybrid control 
 * topology, where the cluster head orchestrates the task inside a cluster.
 * 
 * @see EisimSimulationManager
 * @see EisimOrchestrator
 * 
 * @author Henna Kokkonen
 *
 */
public class EisimTask extends DefaultTask {

	protected ComputingNode intermediateComputingNode = ComputingNode.NULL;
	
	public EisimTask(int id) {
		super(id);
	}
	
	public ComputingNode getIntermediateOffloadingDestination() {
		return intermediateComputingNode;
	}

	public void setIntermediateOffloadingDestination(ComputingNode intermediatePlacementLocation) {
		this.intermediateComputingNode = intermediatePlacementLocation;
	}
}
