package com.github.hennas.eisim.core.network;

import com.github.hennas.eisim.core.datacentersmanager.ComputingNode;
import com.github.hennas.eisim.core.energy.EnergyModelNetworkLink; 

public class NetworkLinkNull extends NetworkLink {

	public double getLatency() {
		return 0;
	}

	public ComputingNode getSrc() {
		return ComputingNode.NULL;
	}

	public ComputingNode getDst() {
		return ComputingNode.NULL;
	}

	protected double getBandwidth(double remainingTasksCount) {
		return 0;
	}

	public double getUsedBandwidth() {
		return 0;
	}
	
	public EnergyModelNetworkLink getEnergyModel() {
		return EnergyModelNetworkLink.NULL;
	}

	public double getTotalTransferredData() {
		return 0;
	}
}
