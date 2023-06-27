package com.github.hennas.eisim.defaultclasses;

import com.github.hennas.eisim.core.datacentersmanager.ComputingNodesGenerator;
import com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager;

import org.w3c.dom.Element;

import com.github.hennas.eisim.core.datacentersmanager.ComputingNode;
import com.github.hennas.eisim.core.network.NetworkLinkMan;
import com.github.hennas.eisim.core.network.NetworkLink.NetworkLinkTypes;
import com.github.hennas.eisim.core.scenariomanager.SimulationParameters;

/**
 * Creates the topology between edge nodes and connects each device to the closest peripheral 
 * edge node.
 * 
 * @author Henna Kokkonen
 *
 */
public class EisimTopologyCreator extends DefaultTopologyCreator {

	public EisimTopologyCreator(SimulationManager simulationManager,
			ComputingNodesGenerator computingNodesGenerator) {
		super(simulationManager, computingNodesGenerator);
	}

	@Override
	public void generateTopologyGraph() {
		// Link edge nodes together (links are defined in edge_datacenters.xml file)
		generateTopologyFromXmlFile();

		// Link edge devices with the closest edge data center
		for (ComputingNode device : computingNodesGenerator.getMistOnlyList()) {
			double range = SimulationParameters.edgeDataCentersRange;
			ComputingNode closestDC = ComputingNode.NULL;
			for (ComputingNode edgeDC : computingNodesGenerator.getEdgeOnlyList()) {
				if (device.getMobilityModel().distanceTo(edgeDC) <= range && edgeDC.isPeripheral()) {
					range = device.getMobilityModel().distanceTo(edgeDC);
					closestDC = edgeDC;
				}
			}
			// Notice that this link is given the LAN tag. When mobile devices change their
			// location, they will automatically connect with the closest peripheral edge
			// data node.
			connect(device, closestDC, NetworkLinkTypes.LAN);
		}

		infrastructureTopology.savePathsToMap(
				simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeAndCloudList());
	}
	
	@Override
	protected void createNetworkLink(Element networkLinkElement) {
		ComputingNode dcFrom = getDataCenterByName(
				networkLinkElement.getElementsByTagName("from").item(0).getTextContent());
		ComputingNode dcTo = getDataCenterByName(
				networkLinkElement.getElementsByTagName("to").item(0).getTextContent());
		infrastructureTopology.addLink(new NetworkLinkMan(dcFrom, dcTo, simulationManager, NetworkLinkTypes.MAN));
		infrastructureTopology.addLink(new NetworkLinkMan(dcTo, dcFrom, simulationManager, NetworkLinkTypes.MAN));
		MANTopology.addLink(new NetworkLinkMan(dcFrom, dcTo, simulationManager, NetworkLinkTypes.MAN));
		MANTopology.addLink(new NetworkLinkMan(dcTo, dcFrom, simulationManager, NetworkLinkTypes.MAN));
	}
}
