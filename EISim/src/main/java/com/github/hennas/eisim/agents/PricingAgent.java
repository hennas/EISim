package com.github.hennas.eisim.agents;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.nd4j.linalg.api.ndarray.INDArray;

import com.github.hennas.eisim.EisimSimulationParameters;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager;
import com.github.hennas.eisim.defaultclasses.EisimComputingNode;
import com.github.hennas.eisim.defaultclasses.EisimSimulationManager;
import com.github.hennas.eisim.core.scenariomanager.Scenario;

/**
 * An abstract class for a pricing agent. Any pricing agent implementation in the simulation
 * must extend this. A pricing agent in the simulation is an edge server node that has also been 
 * assigned as a cluster head.
 * <p>
 * Currently, it is assumed that the action space is one dimensional, consisting only of the price 
 * variable. Hence, any calls to act methods should return only one action (price) within the specified 
 * minimum and maximum prices.
 * 
 * @see EisimComputingNode
 * 
 * @author Henna Kokkonen
 *
 */
public abstract class PricingAgent {
	
	protected String serverName;
	protected int stateSpaceDim;
	protected int actionSpaceDim = 1; // This implementation only supports one dimensional action space with min and max values
	protected float minPrice;
	protected float maxPrice;
	protected Random random;
	protected EisimSimulationManager simulationManager;
	
	/**
	 * A Path to a scenario and agent specific directory that 
	 * can be used for saving and loading the agent's state
	 * 
	 * @see #createStateDirectoryForAgent()
	 */
	protected String directory;

	// Copying the hyperparameters from EisimSimulationParameters (mainly to simplify / shorten references to them)
	// Note that all pricing agents get the same hyperparameters
	protected static String modelFolder = EisimSimulationParameters.modelFolder;
	protected static boolean train = EisimSimulationParameters.train;
	protected static int replayBufferSize = EisimSimulationParameters.replayBufferSize;
	protected static int batchSize = EisimSimulationParameters.batchSize;
	protected static float discountFactor = EisimSimulationParameters.discountFactor;
	protected static float learningRateActor = EisimSimulationParameters.learningRateActor;
	protected static float learningRateCritic = EisimSimulationParameters.learningRateCritic;
	protected static float tau = EisimSimulationParameters.tau;
	protected static float modelUpdates = EisimSimulationParameters.modelUpdates;
	protected static float noiseSD = EisimSimulationParameters.noiseSD;
	protected static float noiseDecay = EisimSimulationParameters.noiseDecay;
	
	/**
	 * Initialize a pricing agent.
	 * 
	 * @param serverName		The unique name of the server node to which the PricingAgent instance belongs
	 * @param stateSpaceDim		Dimension of the state space
	 * @param minPrice			Minimum price that can be set by the agent
	 * @param maxPrice			Maximum price that can be set by the agent
	 * @param simulationManager The simulation manager that links between the different modules
	 */
	public PricingAgent(String serverName, int stateSpaceDim, float minPrice, float maxPrice, SimulationManager simulationManager) {
		this.serverName = serverName;
		this.stateSpaceDim = stateSpaceDim;
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
		this.simulationManager = (EisimSimulationManager) simulationManager;
		this.directory = createStateDirectoryForAgent();
		
		this.random = new Random();
		this.random.setSeed(this.simulationManager.seedGenerator.nextLong());
	}
	
	/**
	 * Uniformly samples a random action (price) from the action space [minPrice, maxPrice].
	 * 
	 * @return float: A randomly chosen action
	 */
	public float act() {
		return this.random.nextFloat(this.minPrice, this.maxPrice);
	}
	
	/**
	 * Decides the action (price) based on the current state.
	 * 
	 * @param state 	Current state observation
	 * @return float: 	The action chosen according to the given state
	 */
	public abstract float act(INDArray state);
	
	/**
	 * Conducts model training based on the agent's experience (state, action, reward, nextState).
	 * 
	 * @param state		Initial state
	 * @param action	The action taken in the initial state
	 * @param reward	The reward received
	 * @param nextState The state that followed from the initial state after taking the action
	 */
	public abstract void learn(INDArray state, float action, float reward, INDArray nextState);
	
	/**
	 * Saves the agent's state into files at the end of the simulation.
	 * 
	 * @throws IOException
	 */
	public abstract void saveAgentState() throws IOException;
	
	/**
	 * Forms and returns a path to the directory that is used for saving and loading the agent's state.
	 * Also creates all the directories in the path if they do not already exist.
	 * <p>
	 * Basically, when running a simulation scenario for the first time, a subfolder is created under 
	 * {@link EisimSimulationParameters#modelFolder} according to the name of the scenario, and then each
	 * pricing agent creates their own subfolder under this scenario folder according to their own unique 
	 * name. The agents will save their state to this folder. When running the same simulation scenario 
	 * with the same specified {@link EisimSimulationParameters#modelFolder} again, the agents will load 
	 * the state from their own state folder.
	 * 
	 * @return String: Path to the directory that is used for saving and loading states for the agent
	 */
	protected String createStateDirectoryForAgent() {
		Scenario scenario = this.simulationManager.getScenario();
		String scenarioName = "scenario_"
				+ scenario.getStringOrchAlgorithm() + "_" 
				+ scenario.getStringOrchArchitecture() + "_" 
				+ scenario.getDevicesCount();
		String directoryString = modelFolder + "/" + scenarioName + "/" + this.serverName;
		new File(directoryString).mkdirs(); // Creates the directories only if they do not already exist
		return directoryString + "/";
	}
	
}
