/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package com.github.hennas.eisim.core.datacentersmanager;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.github.hennas.eisim.core.locationmanager.MobilityModel;
import com.github.hennas.eisim.core.scenariomanager.SimulationParameters;
import com.github.hennas.eisim.core.scenariomanager.SimulationParameters.TYPES;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager;
import com.github.hennas.eisim.core.taskgenerator.Task;

/**
 * This class is responsible for generating the computing resources from the
 * input files ( @see
 * com.github.hennas.eisim.core.simulationcore.SimulationAbstract#setCustomSettingsFolder(String))
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 1.0
 */
public abstract class ComputingNodesGenerator {

	/**
	 * The list that contains all orchestrators. It is used by the computing
	 * node. In this case, the tasks are sent over the network to one of the
	 * orchestrators to make decisions.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * @see com.github.hennas.eisim.core.simulationmanager.DefaultSimulationManager#sendTaskToOrchestrator(Task)
	 */
	protected List<ComputingNode> orchestratorsList;

	/**
	 * The simulation manager.
	 */
	protected SimulationManager simulationManager;

	/**
	 * The Mobility Model to be used in this scenario
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager)
	 */
	protected Class<? extends MobilityModel> mobilityModelClass;

	/**
	 * The Computing Node Class to be used in this scenario
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager)
	 */
	protected Class<? extends ComputingNode> computingNodeClass;

	/**
	 * A list that contains all edge devices including sensors (i.e., devices
	 * without computing capacities).
	 */
	protected List<ComputingNode> mistOnlyList;

	/**
	 * A list that contains all edge devices except sensors (i.e., devices
	 * without computing capacities).
	 * 
	 * @see com.github.hennas.eisim.core.taskorchestrator.Orchestrator#mistOnly()
	 */
	protected List<ComputingNode> mistOnlyListSensorsExcluded;

	/**
	 * A list that contains only edge data centers and servers.
	 * 
	 * @see com.github.hennas.eisim.core.taskorchestrator.Orchestrator#edgeOnly()
	 */
	protected List<ComputingNode> edgeOnlyList = new ArrayList<>(SimulationParameters.numberOfEdgeDataCenters);

	/**
	 * A list that contains only cloud data centers.
	 * 
	 * @see com.github.hennas.eisim.core.taskorchestrator.Orchestrator#cloudOnly()
	 */
	protected List<ComputingNode> cloudOnlyList = new ArrayList<>(SimulationParameters.numberOfCloudDataCenters);

	/**
	 * A list that contains cloud data centers and edge devices (except sensors).
	 * 
	 * @see com.github.hennas.eisim.core.taskorchestrator.Orchestrator#mistAndCloud()
	 */
	protected List<ComputingNode> mistAndCloudListSensorsExcluded;

	/**
	 * A list that contains cloud and edge data centers.
	 * 
	 * @see com.github.hennas.eisim.core.taskorchestrator.Orchestrator#edgeAndCloud()
	 */
	protected List<ComputingNode> edgeAndCloudList = new ArrayList<>(
			SimulationParameters.numberOfCloudDataCenters + SimulationParameters.numberOfEdgeDataCenters);

	/**
	 * A list that contains edge data centers and edge devices (except sensors).
	 * 
	 * @see com.github.hennas.eisim.core.taskorchestrator.Orchestrator#mistAndEdge()
	 */
	protected List<ComputingNode> mistAndEdgeListSensorsExcluded;

	/**
	 * A list that contains all generated nodes including sensors
	 */
	protected List<ComputingNode> allNodesList;

	/**
	 * A list that contains all generated nodes (sensors excluded)
	 * 
	 * @see com.github.hennas.eisim.core.taskorchestrator.Orchestrator#all()
	 */
	protected List<ComputingNode> allNodesListSensorsExcluded;

	/**
	 * Initializes the Computing nodes generator.
	 *
	 * @param simulationManager  The simulation Manager
	 * @param mobilityModelClass The mobility model that will be used in the
	 *                           simulation
	 * @param computingNodeClass The computing node class that will be used to
	 *                           generate computing resources
	 */
	public ComputingNodesGenerator(SimulationManager simulationManager,
			Class<? extends MobilityModel> mobilityModelClass, Class<? extends ComputingNode> computingNodeClass) {
		this.mobilityModelClass = mobilityModelClass;
		this.computingNodeClass = computingNodeClass;
		this.simulationManager = simulationManager;
		orchestratorsList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		mistOnlyList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		mistOnlyListSensorsExcluded = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		mistAndCloudListSensorsExcluded = new ArrayList<>(
				simulationManager.getScenario().getDevicesCount() + SimulationParameters.numberOfCloudDataCenters);
		mistAndEdgeListSensorsExcluded = new ArrayList<>(
				simulationManager.getScenario().getDevicesCount() + SimulationParameters.numberOfEdgeDataCenters);
		allNodesList = new ArrayList<>(simulationManager.getScenario().getDevicesCount()
				+ SimulationParameters.numberOfEdgeDataCenters + SimulationParameters.numberOfCloudDataCenters);
		allNodesListSensorsExcluded = new ArrayList<>(simulationManager.getScenario().getDevicesCount()
				+ SimulationParameters.numberOfEdgeDataCenters + SimulationParameters.numberOfCloudDataCenters);
	}

	/**
	 * Generates all computing nodes, including the Cloud data centers, the edge
	 * ones, and the edge devices.
	 */
	public abstract void generateDatacentersAndDevices();
	
	/**
	 * Returns the list containing computing nodes that have been selected as
	 * orchestrators (i.e. to make offloading decisions).
	 * 
	 * @return The list of orchestrators
	 */
	public List<ComputingNode> getOrchestratorsList() {
		return orchestratorsList;
	}

	/**
	 * Returns the simulation Manager.
	 * 
	 * @return The simulation manager
	 */
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	/**
	 * Gets the list containing all generated computing nodes.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDatacentersAndDevices()
	 * 
	 * @return the list containing all generated computing nodes.
	 */
	public List<ComputingNode> getAllNodesList() {
		return this.allNodesList;
	}

	/**
	 * Gets the list containing all generated edge devices including sensors
	 * (i.e., devices with no computing resources).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDevicesInstances(Element)
	 * 
	 * @return the list containing all edge devices including sensors.
	 */
	public List<ComputingNode> getMistOnlyList() {
		return this.mistOnlyList;
	}

	/**
	 * Gets the list containing all generated edge data centers / servers.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * 
	 * @return the list containing all edge data centers and servers.
	 */
	public List<ComputingNode> getEdgeOnlyList() {
		return this.edgeOnlyList;
	}

	/**
	 * Gets the list containing only cloud data centers.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * 
	 * @return the list containing all generated cloud data centers.
	 */
	public List<ComputingNode> getCloudOnlyList() {
		return this.cloudOnlyList;
	}

	/**
	 * Gets the list containing cloud data centers and edge devices (except
	 * sensors).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDevicesInstances(Element)
	 * 
	 * @return the list containing cloud data centers and edge devices.
	 */
	public List<ComputingNode> getMistAndCloudListSensorsExcluded() {
		return this.mistAndCloudListSensorsExcluded;
	}

	/**
	 * Gets the list containing cloud and edge data centers.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * 
	 * @return the list containing cloud and edge data centers.
	 */
	public List<ComputingNode> getEdgeAndCloudList() {
		return this.edgeAndCloudList;
	}

	/**
	 * Gets the list containing edge data centers and edge devices (except
	 * sensors).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDevicesInstances(Element)
	 * 
	 * @return the list containing edge data centers and edge devices.
	 */
	public List<ComputingNode> getMistAndEdgeListSensorsExcluded() {
		return this.mistAndEdgeListSensorsExcluded;
	}

	/**
	 * Gets the list containing all generated edge devices except sensors (i.e.,
	 * devices with no computing resources).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDevicesInstances(Element)
	 * 
	 * @return the list containing all edge devices except sensors.
	 */
	public List<ComputingNode> getMistOnlyListSensorsExcluded() {
		return this.mistOnlyListSensorsExcluded;
	}

	/**
	 * Gets the list containing all computing nodes (except sensors).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDevicesInstances(Element)
	 * 
	 * @return the list containing all data centers and devices except sensors.
	 */
	public List<ComputingNode> getAllNodesListSensorsExcluded() {
		return this.allNodesListSensorsExcluded;
	}

}
