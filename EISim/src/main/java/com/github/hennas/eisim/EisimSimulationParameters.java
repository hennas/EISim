package com.github.hennas.eisim;

import com.github.hennas.eisim.defaultclasses.EisimSimulationManager;
import com.github.hennas.eisim.helpers.ArgumentParser;

/**
 * Holds the EISim simulation parameters. These specify where the simulation setting files are located, 
 * where to save simulation results and agent states, whether to seed the simulation and with what seed, 
 * whether to run the simulation in training mode and with what hyperparameters. 
 * These can be given to the program through command-line arguments.
 * 
 * @see ArgumentParser
 * 
 * @author Henna Kokkonen
 *
 */
public class EisimSimulationParameters {

	/**
	 * Setting folder path.
	 */
	public static String settingFolder;
	
	/**
	 * Output folder path.
	 */
	public static String outputFolder;
	
	/**
	 * Folder path for agent models. Used for saving and loading agent states.
	 * 
	 * For each simulation scenario, a subfolder is created into this directory. Each edge server 
	 * agent in the scenario creates its own subfolder into this scenario folder. The name of the 
	 * subfolder is the same as the agent's name (the name is specified in edge_datacenters.xml 
	 * setting file).
	 */
	public static String modelFolder;
	
	/**
	 * Whether the seed generator should be seeded or not.
	 * 
	 * @see EisimSimulationManager#seedGenerator
	 */
	public static boolean useSeed = false;
	
	/**
	 * A seed for the seed generator. The seed generator generates the seeds for every random 
	 * number generator in the simulation.
	 * 
	 * @see EisimSimulationManager#seedGenerator
	 */
	public static long seed;
	
	/**
	 * Whether the simulation is run in training mode.
	 */
	public static boolean train;

	/**
	 * Determines how many times at the beginning of a simulation each pricing agent decides the 
	 * price randomly.
	 */
	public static int randomDecisionSteps = 0;
	
	/**
	 * Experience replay size.
	 */
	public static int replayBufferSize = 2000;
	
	/**
	 * Batch size for agent training.
	 */
	public static int batchSize = 128;
	
	/**
	 * Discount factor for future rewards.
	 */
	public static float discountFactor = 0.99f;
	
	/**
	 * The learning rate for actor network.
	 */
	public static float learningRateActor = 0.001f;
	
	/**
	 * The learning rate for critic network.
	 */
	public static float learningRateCritic = 0.001f;
	
	/**
	 * For updating the actor and critic target models.
	 */
	public static float tau = 0.005f;
	
	/**
	 * How many times models are updated during one update event.
	 */
	public static int modelUpdates = 1;
	
	/**
	 * Standard deviation for the noise process.
	 */
	public static float noiseSD = 0.5f;
	
	/**
	 * Noise decay rate.
	 */
	public static float noiseDecay = 1e-6f;
	
	/**
	 * Stores filenames for all allowed types of pricing agent files.
	 */
	public enum AgentFileTypes {
		ACTOR ("actor.zip"),
		ACTOR_TARGET ("actor_target.zip"), 
		CRITIC ("critic.zip"), 
		CRITIC_TARGET ("critic_target.zip"), 
		EXPERIENCE_REPLAY ("experience_replay.ser"),
		NOISE_COEFF("noise_coeff.dat");
		
		private String fileName;
		
		AgentFileTypes(String fileName) {
			this.fileName = fileName;
		}
		
		public String getFileName() {
			return this.fileName;
		}
	}

	/**
	 * This class should not be instantiated.
	 */
	private EisimSimulationParameters() {
		throw new IllegalStateException("EisimSimulationParameters class cannot be instantiated");
	}

}
