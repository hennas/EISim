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
package com.github.hennas.eisim.core.simulationmanager;

import com.github.hennas.eisim.core.datacentersmanager.ComputingNode;
import com.github.hennas.eisim.core.datacentersmanager.ComputingNodesGenerator;
import com.github.hennas.eisim.core.datacentersmanager.TopologyCreator;
import com.github.hennas.eisim.core.locationmanager.DefaultMobilityModel;
import com.github.hennas.eisim.core.locationmanager.MobilityModel;
import com.github.hennas.eisim.core.network.NetworkModel;
import com.github.hennas.eisim.core.scenariomanager.SimulationParameters;
import com.github.hennas.eisim.core.taskgenerator.TaskGenerator;
import com.github.hennas.eisim.core.taskorchestrator.Orchestrator;
import com.github.hennas.eisim.defaultclasses.EisimComputingNode;
import com.github.hennas.eisim.defaultclasses.EisimComputingNodesGenerator;
import com.github.hennas.eisim.defaultclasses.EisimNetworkModel;
import com.github.hennas.eisim.defaultclasses.EisimOrchestrator;
import com.github.hennas.eisim.defaultclasses.EisimSimulationManager;
import com.github.hennas.eisim.defaultclasses.EisimTaskGenerator;
import com.github.hennas.eisim.defaultclasses.EisimTopologyCreator;

/**
 * An abstract class that represents the simulation environment
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public abstract class SimulationAbstract {

	public enum Files {
		SIMULATION_PARAMETERS, APPLICATIONS_FILE, EDGE_DATACENTERS_FILE, EDGE_DEVICES_FILE, CLOUD_FILE
	}

	/**
	 * The Mobility Model class that is used in the simulation.
	 * 
	 * @see #setCustomMobilityModel(Class)
	 */
	protected Class<? extends MobilityModel> mobilityModel = DefaultMobilityModel.class;

	/**
	 * The Computing Node class that is used in the simulation.
	 * 
	 * @see #setCustomComputingNode(Class)
	 */
	protected Class<? extends ComputingNode> computingNode = EisimComputingNode.class;

	/**
	 * The Tasks Generator class that is used in the simulation.
	 * 
	 * @see #setCustomTaskGenerator(Class)
	 */
	protected Class<? extends TaskGenerator> tasksGenerator = EisimTaskGenerator.class;

	/**
	 * The Orchestrator class that is used in the simulation.
	 * 
	 * @see #setCustomEdgeOrchestrator(Class)
	 */
	protected Class<? extends Orchestrator> orchestrator = EisimOrchestrator.class;
	
	/**
	 * The Network Model class that is used in the simulation.
	 * 
	 * @see #setCustomNetworkModel(Class)
	 */
	protected Class<? extends NetworkModel> networkModel = EisimNetworkModel.class;

	/**
	 * The Simulation Manager class that is used in the simulation.
	 * 
	 * @see #setCustomSimulationManager(Class)
	 */
	protected Class<? extends SimulationManager> simulationManager = EisimSimulationManager.class;
	
	/**
	 * The Topology Creator class that is used in the simulation.
	 * 
	 * @see #setCustomTopologyCreator(Class)
	 */
	protected Class<? extends TopologyCreator> topologyCreator = EisimTopologyCreator.class;
	
	/**
	 * The Computing Nodes Generator class that is used in the simulation.
	 * 
	 * @see #setCustomComputingNodesGenerator(Class)
	 */
	protected Class<? extends ComputingNodesGenerator> computingNodesGenerator = EisimComputingNodesGenerator.class;
	
	/**
	 * Allows to use a custom computing node class in the simulation. The class must
	 * extend the {@link ComputingNode} provided by PureEdgeSim.
	 * 
	 * @param customComputingNode the custom class to use.
	 */
	public void setCustomComputingNode(Class<? extends ComputingNode> customComputingNode) {
		this.computingNode = customComputingNode;
	}

	/**
	 * Allows to use a custom tasks generator class in the simulation. The class
	 * must extend the {@link TaskGenerator} provided by PureEdgeSim.
	 * 
	 * @param taskGenerator the custom task generator class to use.
	 */
	public void setCustomTaskGenerator(Class<? extends TaskGenerator> taskGenerator) {
		this.tasksGenerator = taskGenerator;
	}

	/**
	 * Allows to use a custom orchestrator class in the simulation. The class must
	 * extend the {@link Orchestrator} provided by PureEdgeSim.
	 * 
	 * @param orchestrator the custom orchestrator class to use.
	 */
	public void setCustomEdgeOrchestrator(Class<? extends Orchestrator> orchestrator) {
		this.orchestrator = orchestrator;
	} 
	

	/**
	 * Allows to use a custom mobility model in the simulation. The class must
	 * extend the {@link MobilityModel} provided in PureEdgeSim.
	 * 
	 * @param mobilityModel the custom mobility model class to use.
	 */
	public void setCustomMobilityModel(Class<? extends MobilityModel> mobilityModel) {
		this.mobilityModel = mobilityModel;
	}

	/**
	 * Allows to use a custom network model in the simulation. The class must extend
	 * the {@link NetworkModel} provided by PureEdgeSim.
	 * 
	 * @param networkModel the custom network model class to use.
	 */
	public void setCustomNetworkModel(Class<? extends NetworkModel> networkModel) {
		this.networkModel = networkModel;
	}

	/**
	 * Allows to use a custom topology creator in the simulation. The class must extend
	 * the {@link TopologyCreator} provided by PureEdgeSim.
	 * 
	 * @param topologyCreator the custom topology creator class to use.
	 */
	public void setCustomTopologyCreator(Class<? extends TopologyCreator> topologyCreator) {
		this.topologyCreator = topologyCreator;
	}
	
	/**
	 * Allows to use a custom simulation manager class in the simulation. The class
	 * must extend the {@link DefaultSimulationManager} provided by PureEdgeSim.
	 * 
	 * @param simulationManager the custom simulation manager class to use.
	 */
	public void setCustomSimulationManager(Class<? extends SimulationManager> simulationManager) {
		this.simulationManager = simulationManager;
	}
	
	/**
	 * Allows to use a custom computing nodes generator in the simulation. The class must extend
	 * the {@link ComputingNodesGenerator} provided by PureEdgeSim.
	 * 
	 * @param computingNodesGenerator the custom computing nodes generator class to use.
	 */
	public void setCustomComputingNodesGenerator(Class<? extends ComputingNodesGenerator> computingNodesGenerator) {
		this.computingNodesGenerator = computingNodesGenerator;
	}

	/**
	 * Allows to change the output folder, in which the simulation results will be
	 * saved.
	 * 
	 * @param path the output folder to set.
	 */
	public void setCustomOutputFolder(String path) {
		SimulationParameters.outputFolder = path;
	}

	/**
	 * Allows to change the location from which the configuration files are loaded.
	 * 
	 * @param settingsFolder the new settings folder to use.
	 */
	public void setCustomSettingsFolder(String settingsFolder) {
		setCustomFilePath(settingsFolder + "simulation_parameters.properties", Files.SIMULATION_PARAMETERS);
		setCustomFilePath(settingsFolder + "applications.xml", Files.APPLICATIONS_FILE);
		setCustomFilePath(settingsFolder + "edge_datacenters.xml", Files.EDGE_DATACENTERS_FILE);
		setCustomFilePath(settingsFolder + "edge_devices.xml", Files.EDGE_DEVICES_FILE);
		setCustomFilePath(settingsFolder + "cloud.xml", Files.CLOUD_FILE);
	}

	/**
	 * Allows to set a custom path of each configuration file individually.
	 * 
	 * @param path the path from which the file will be loaded
	 * @param file the nature of the file to load
	 */
	public void setCustomFilePath(String path, Files file) {
		switch (file) {
		case SIMULATION_PARAMETERS:
			SimulationParameters.simulationParametersFile = path;
			break;
		case APPLICATIONS_FILE:
			SimulationParameters.applicationFile = path;
			break;
		case EDGE_DATACENTERS_FILE:
			SimulationParameters.edgeDataCentersFile = path;
			break;
		case EDGE_DEVICES_FILE:
			SimulationParameters.edgeDevicesFile = path;
			break;
		case CLOUD_FILE:
			SimulationParameters.cloudDataCentersFile = path;
			break;
		default:
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown file type");
		}
	}

}
