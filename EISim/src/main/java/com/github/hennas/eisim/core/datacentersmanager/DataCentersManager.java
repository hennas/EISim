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

import java.lang.reflect.Constructor;

import com.github.hennas.eisim.core.locationmanager.MobilityModel;
import com.github.hennas.eisim.core.network.InfrastructureGraph;
import com.github.hennas.eisim.core.simulationmanager.SimLog;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager; 

/**
 * The main class of the Data Centers Manager module, that manages the different
 * resources and generates the infrastructure topology.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */

public class DataCentersManager {
	/**
	 * The simulation manager.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.DefaultSimulationManager
	 */
	protected SimulationManager simulationManager;

	/**
	 * The computing nodes generator used to generate all resources from the xml
	 * files.
	 * 
	 * @see #generateComputingNodes()
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator
	 */
	protected ComputingNodesGenerator computingNodesGenerator;

	/**
	 * The topology creator that is used to generate the network topology.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator
	 */
	protected TopologyCreator topologyCreator;

	/**
	 * Initializes the DataCentersManager
	 *
	 * @param simulationManager  			The simulation manager
	 * @param mobilityModelClass 			The mobility model class that will be used in the
	 *                           			simulation
	 * @param computingNodeClass 			The computing node class that will be used to
	 *                           			generate computing resources
	 * @param computingNodesGeneratorClass	The computing node generator class that will be
	 * 										used to generate all resources from the XML
	 * 										files
	 * @param topologyCreatorClass			The topology creator class that will be used to
	 * 										generate the network topology
	 */
	public DataCentersManager(SimulationManager simulationManager, 
			Class<? extends MobilityModel> mobilityModelClass,
			Class<? extends ComputingNode> computingNodeClass, 
			Class<? extends ComputingNodesGenerator> computingNodesGeneratorClass,
			Class<? extends TopologyCreator> topologyCreatorClass) {
		this.simulationManager = simulationManager;
		// Add this to the simulation manager and submit computing nodes to broker
		simulationManager.setDataCentersManager(this);

		// Generate all data centers, servers, and devices
		generateComputingNodes(mobilityModelClass, computingNodeClass, computingNodesGeneratorClass);

		// Generate topology
		createTopology(topologyCreatorClass);
	}

	/**
	 * Generates all computing nodes.
	 * 
	 * @param mobilityModelClass
	 * @param computingNodeClass
	 * @param computingNodesGeneratorClass
	 */
	protected void generateComputingNodes(Class<? extends MobilityModel> mobilityModelClass,
			Class<? extends ComputingNode> computingNodeClass, 
			Class<? extends ComputingNodesGenerator> computingNodesGeneratorClass) {
		SimLog.println("%s - Generating computing nodes...",this.getClass().getSimpleName());
		Constructor<?> computingNodesGeneratorConstructor;
		try {
			computingNodesGeneratorConstructor = computingNodesGeneratorClass.getConstructor(SimulationManager.class,
					Class.class, Class.class);

			computingNodesGenerator = (ComputingNodesGenerator) computingNodesGeneratorConstructor.newInstance(simulationManager,
					mobilityModelClass, computingNodeClass);
		} catch (Exception e) {
			e.printStackTrace();
		}
		computingNodesGenerator.generateDatacentersAndDevices();
	}

	/**
	 * Creates the network topology.
	 * 
	 * @param topologyCreatorClass
	 */
	public void createTopology(Class<? extends TopologyCreator> topologyCreatorClass) {
		SimLog.println("%s - Creating the network topology...",this.getClass().getSimpleName());
		Constructor<?> topologyCreatorConstructor;
		try {
			topologyCreatorConstructor = topologyCreatorClass.getConstructor(SimulationManager.class,
					ComputingNodesGenerator.class);

			topologyCreator = (TopologyCreator) topologyCreatorConstructor.newInstance(simulationManager,
					computingNodesGenerator);
		} catch (Exception e) {
			e.printStackTrace();
		}
		topologyCreator.generateTopologyGraph();
	}

	/**
	 * Gets the infrastructure topology graph.
	 * 
	 * @return InfrastructureGraph the infrastructure graph.
	 */
	public InfrastructureGraph getTopology() {
		return topologyCreator.getTopology();
	}
	
	/**
	 * Gets the Metropolitan Area Network topology graph that only contains the links 
	 * between edge nodes (APs and edge servers).
	 * 
	 * @return InfrastructureGraph the MAN topology graph.
	 */
	public InfrastructureGraph getMANTopology() {
		return topologyCreator.getMANTopology();
	}

	/**
	 * Gets the computing nodes generator.
	 * 
	 * @return computingNodesGenerator the computing nodes generator..
	 */
	public ComputingNodesGenerator getComputingNodesGenerator() {
		return computingNodesGenerator;
	}

}
