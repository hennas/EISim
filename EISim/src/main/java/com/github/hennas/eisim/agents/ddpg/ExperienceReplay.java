package com.github.hennas.eisim.agents.ddpg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.github.hennas.eisim.defaultclasses.EisimSimulationManager;

/**
 * Implements an experience replay for an agent.
 * 
 * @author Henna Kokkonen
 *
 */
public class ExperienceReplay {
	
	private int limit;
	private int batchSize;
	private ArrayList<ExperienceTuple> memory;
	
	/**
	 * Used for sampling random minibatches
	 * 
	 * @see #getBatch()
	 */
	private Random random;

	/**
	 * Initialize empty experience replay.
	 * 
	 * @param limit		The maximum size of experience replay
	 * @param batchSize The size for a minibatch
	 */
	public ExperienceReplay(int limit, int batchSize, EisimSimulationManager EisimSimulationManager) {
		this.limit = limit;
		this.batchSize = batchSize;
		this.memory = new ArrayList<>(limit);
		
		this.random = new Random();
		this.random.setSeed(EisimSimulationManager.seedGenerator.nextInt());
	}
	
	/**
	 * Initialize experience replay with existing experience tuples. 
	 * If there are more experiences in the given list of tuples than the specified memory limit, excess ones are removed.
	 * 
	 * @param limit		The maximum size of experience replay
	 * @param batchSize The size for a minibatch
	 * @param memory	A list of existing experience tuples that are used to initialize the experience memory
	 */
	public ExperienceReplay(int limit, int batchSize, ArrayList<ExperienceTuple> memory, EisimSimulationManager EisimSimulationManager) {
		this(limit, batchSize, EisimSimulationManager);
		while (memory.size() > limit) {
			memory.remove(0);
		}
		this.memory = memory;
	}
	
	/**
	 * Adds an experience tuple into the memory. 
	 * If the memory limit has been reached, deletes the oldest experience before adding the new one.
	 * 
	 * @param e	ExperienceTuple object that encapsulates one (state, action, reward, nextState) experience
	 */
	public void addExperience(ExperienceTuple e) {
		if (this.size() == this.limit) {
			this.memory.remove(0);
		}
		this.memory.add(e);
		
	}
	
	/**
	 * Get the current content of the experience replay.
	 * 
	 * @return ArrayList<ExperienceTuple>: The memory content
	 */
	public ArrayList<ExperienceTuple> getMemoryContent() {
		return this.memory;
	}
	
	/**
	 * Samples a random minibatch from the memory.
	 * <p>
	 * The returned minibatch is sampled uniformly at random from the memory.
	 * 
	 * @return ArrayList<ExperienceTuple>: A list of ExperienceTuple objects that encapsulate (state, action, reward, nextState) experiences
	 */
	public ArrayList<ExperienceTuple> getBatch() {
		ArrayList<ExperienceTuple> minibatch = new ArrayList<>(this.batchSize); 
		ArrayList<Integer> indices = getListOfIndices(); // A list of indices for current memory objects
		
		Collections.shuffle(indices, this.random); // Create a random permutation of the indices using the given Random object
		for (int i = 0; i < this.batchSize; i++) {
			minibatch.add(this.memory.get(indices.get(i))); // indices.get(i) gives the index for a randomly chosen memory object
		}
		return minibatch;
	}
	
	/**
	 * Get the current size of the experience replay.
	 * 
	 * @return int: The size of the memory
	 */
	public int size() {
		return this.memory.size();
	}
	
	/**
	 * Creates an index list according to the current memory size.
	 * 
	 * @return ArrayList<Integer>: A list of indices 
	 */
	private ArrayList<Integer> getListOfIndices() {
		ArrayList<Integer> indices = new ArrayList<>(this.size());
		for (int i = 0; i < this.size(); i++) {
			indices.add(i);
		}
		return indices;
	}
}
