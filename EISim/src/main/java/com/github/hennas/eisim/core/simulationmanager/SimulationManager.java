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

import java.util.ArrayList;
import java.util.List;

import com.github.hennas.eisim.core.datacentersmanager.DataCentersManager;
import com.github.hennas.eisim.core.network.NetworkModel;
import com.github.hennas.eisim.core.scenariomanager.Scenario;
import com.github.hennas.eisim.core.simulationengine.FutureQueue;
import com.github.hennas.eisim.core.simulationengine.PureEdgeSim;
import com.github.hennas.eisim.core.simulationengine.SimEntity;
import com.github.hennas.eisim.core.simulationvisualizer.SimulationVisualizer;
import com.github.hennas.eisim.core.taskgenerator.Task;
import com.github.hennas.eisim.core.taskorchestrator.Orchestrator;

/**
 * The abstract class that is extended by the default simulation manager.
 * 
 * @see com.github.hennas.eisim.core.simulationmanager.DefaultSimulationManager
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 1.0
 */
public abstract class SimulationManager extends SimEntity {

	/**
	 * Simulation manager tags.
	 * 
	 * @see com.github.hennas.eisim.core.simulationengine.SimEntity#schedule(SimEntity
	 *      simEntity, Double time, int tag)
	 **/
	protected static final int PRINT_LOG = 1;
	protected static final int SHOW_PROGRESS = 2;
	public static final int EXECUTE_TASK = 3;
	public static final int TRANSFER_RESULTS_TO_ORCH = 4;
	public static final int RESULT_RETURN_FINISHED = 5;
	public static final int SEND_TO_ORCH = 6;
	public static final int UPDATE_REAL_TIME_CHARTS = 7;
	public static final int SEND_TASK_FROM_ORCH_TO_DESTINATION = 8;
	protected static final int NEXT_BATCH = 9; 

	protected Orchestrator edgeOrchestrator;
	protected DataCentersManager dataCentersManager;
	protected SimulationVisualizer simulationVisualizer;
	protected PureEdgeSim simulation;
	protected int simulationId;
	protected int iteration;
	protected SimLog simLog;
	protected NetworkModel networkModel;
	protected List<Task> finishedTasks = new ArrayList<>();
	protected Scenario scenario;
	protected FutureQueue<Task> taskList;

	/**
	 * Initializes the simulation manager.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#startSimulation()
	 * 
	 * @param simLog       The simulation logger
	 * @param pureEdgeSim  The CloudSim simulation engine.
	 * @param simulationId The simulation ID
	 * @param iteration    Which simulation run
	 * @param scenario     The scenario is composed of the algorithm and
	 *                     architecture that are being used, and the number of edge
	 *                     devices.
	 */
	public SimulationManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration,
			Scenario scenario) {
		super(pureEdgeSim);
		this.simulation = pureEdgeSim;
		this.simLog = simLog;
		this.scenario = scenario;
		this.simulationId = simulationId;
		this.iteration = iteration;

	}

	/**
	 * Starts PureEdgeSim simulation engine
	 * 
	 * @see com.github.hennas.eisim.core.simulationengine.PureEdgeSim#start()
	 */
	public abstract void startSimulation();

	/**
	 * Sets the data centers manager.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @param dataCentersManager The data centers manager that is used in this
	 *                           simulation
	 */
	public void setDataCentersManager(DataCentersManager dataCentersManager) {
		this.dataCentersManager = dataCentersManager; 
	}
	
	/**
	 * Sets the list of ordered offlaoding requests
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @param taskList the ordered list of offlaoding requests.
	 */
	public void setTaskList(FutureQueue<Task> taskList) {
		this.taskList = taskList;
	}
	
	/**
	 * Returns the list of finished tasks.
	 * 
	 * @return the list of finished tasks.
	 */
	public List<Task> getFinishedTaskList() {
		return this.finishedTasks; 
	}

	/**
	 * Sets the orchestrator that is used in this simulation. Used when offloading
	 * the tasks.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @param edgeOrchestrator the orchestrator.
	 */
	public void setOrchestrator(Orchestrator edgeOrchestrator) {
		this.edgeOrchestrator = edgeOrchestrator;

	}

	/**
	 * Sets the network model that is used in this simulation.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @param networkModel the network model.
	 */
	public void setNetworkModel(NetworkModel networkModel) {
		this.networkModel = networkModel;
	}

	/**
	 * Returns the iteration number.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#startSimulation()
	 * 
	 * @return The iteration number.
	 */
	public int getIteration() {
		return iteration;
	}

	/**
	 * Returns the network model.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @return The network model.
	 */
	public NetworkModel getNetworkModel() {
		return networkModel;
	}

	/**
	 * Returns the simulation ID.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#startSimulation()
	 * 
	 * @return The simulation ID.
	 */
	public int getSimulationId() {
		return simulationId;
	}

	/**
	 * Returns the simulation logger.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#startSimulation()
	 * @see com.github.hennas.eisim.core.simulationmanager.SimLog
	 * 
	 * @return The simulation logger.
	 */
	public SimLog getSimulationLogger() {
		return simLog;
	}

	/**
	 * Returns the data centers manager that is used in this simulation.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @return The data centers manager.
	 */
	public DataCentersManager getDataCentersManager() {
		return this.dataCentersManager;
	}

	/**
	 * Returns the name of orchestration algorithm, the architecture in use, as well
	 * as the number of devices.
	 * 
	 * @return The simulation scenario.
	 */
	public Scenario getScenario() {
		return scenario;
	}
	
	/**
	 * Used to get the task failure rate.
	 * 
	 * @return The failure rate.
	 */
	public abstract double getFailureRate();

}
