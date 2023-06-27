package com.github.hennas.eisim.core.datacentersmanager;

import com.github.hennas.eisim.core.simulationmanager.SimulationManager;

public class Router extends DefaultComputingNode {

	public Router(SimulationManager simulationManager) {
		super(simulationManager, 0, 0, 0, 0);
	}

	@Override
	public void startInternal() {
		// Do nothing
	}

	@Override
	public void setApplicationPlacementLocation(ComputingNode node) {
		// Do nothing
	}
}
