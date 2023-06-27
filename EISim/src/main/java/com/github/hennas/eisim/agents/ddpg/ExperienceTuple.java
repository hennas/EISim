package com.github.hennas.eisim.agents.ddpg;

import java.io.Serializable;

import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * Encapsulates one agent experience of the form (state, action, reward, nextState).
 * 
 * @author Henna Kokkonen
 *
 */
public class ExperienceTuple implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final INDArray state;
	private final INDArray action;
	private final INDArray reward;
	private final INDArray nextState;

	public ExperienceTuple(INDArray state, INDArray action, INDArray reward, INDArray nextState) {
		this.state = state;
		this.action = action;
		this.reward = reward;
		this.nextState = nextState;
	}
	
	public INDArray getState() {
		return this.state;
	}
	
	public INDArray getAction() {
		return this.action;
	}
	
	public INDArray getReward() {
		return this.reward;
	}
	
	public INDArray getNextState() {
		return this.nextState;
	}

}
