package com.github.hennas.eisim.agents.ddpg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.AMSGrad;

import com.github.hennas.eisim.EisimSimulationParameters.AgentFileTypes;
import com.github.hennas.eisim.agents.PricingAgent;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager;
import com.github.hennas.eisim.defaultclasses.EisimComputingNode;

/**
 * Implements a DDPG based pricing agent. This class hold the agent's models and experience memory.
 * Models are trained based on the data provided by the environment.
 * 
 * @see EisimComputingNode
 * 
 * @author Henna Kokkonen
 *
 */
public class DdpgPricingAgent extends PricingAgent {
	// Agent models
	protected MultiLayerNetwork actor;
	protected MultiLayerNetwork critic;
	protected MultiLayerNetwork actorTarget;
	protected MultiLayerNetwork criticTarget;
	
	protected ExperienceReplay memory;
	
	// File names for saving and loading the agent's state
	protected String actorFilePath;
	protected String actorTargetFilePath;
	protected String criticFilePath;
	protected String criticTargetFilePath;
	protected String memoryFilePath;
	protected String noiseCoeffFilePath;
	
	/* Min and max values for actor output, used in scaling the actor output into pricing range. 
	 * All PricingAgents have the same actor structure, so these values are shared between all instances of this class.
	 * The following values correspond to tanh activation, which gives values in range (-1,1)
	 */ 
	protected static float minActorOut = -1;
	protected static float maxActorOut = 1;
	// The derivative of the function that scales the actor output to a price; Needed during training
	protected float scaleFunDerivative; 
	
	/* Each agent has their own noise coefficient that is decayed after each model update of the agent. 
	 * Every noise sample that is added to the action during training is multiplied with this. In other
	 * words, this determines the portion of noise that is added to the action.
	 * Agents will save the current value of this field into a file at the end of a simulation run, 
	 * and then load it from the file at the beginning of a new simulation run (in a case where training
	 * is conducted over multiple simulation runs for the same simulation scenario).
	 */
	protected float noiseCoeff = 1f;

	/**
	 * Initializes a DDPG pricing agent for an edge server node. 
	 * <p>
	 * If the simulation is run in the training mode as specified by {@link EisimSimulationParameters#train}, 
	 * it is first checked whether actor and critic networks and their target counterparts can be found in 
	 * the agent's state folder under {@link EisimSimulationParameters#modelFolder}. If <b>all</b> model files 
	 * are found and successfully loaded, the PricingAgent instance is initialized with the loaded models. 
	 * Otherwise new networks with randomly initialized weights are created. Then, if experience memory file 
	 * exists and its content is what expected, an experience replay is initialized with the loaded memory
	 * content. Otherwise a new, empty experience replay is initialized. Finally, the value for noise coefficient
	 * is loaded from the corresponding file in case the file exists. Otherwise the noise coefficient uses the 
	 * default value of 1.
	 * <p>
	 * If the simulation is run in the evaluation mode, only actor model is loaded from the corresponding file. 
	 * If the actor model file is not found or successfully loaded, an exception is thrown.
	 * 
	 * @param serverName		The unique name of the server node to which the PricingAgent instance belongs
	 * @param stateSpaceDim		Dimension of the state space
	 * @param minPrice			Minimum price that can be set by the agent
	 * @param maxPrice			Maximum price that can be set by the agent
	 * @param simulationManager The simulation manager that links between the different modules
	 */
	public DdpgPricingAgent(String serverName, int stateSpaceDim, float minPrice, float maxPrice,
			SimulationManager simulationManager) {
		super(serverName, stateSpaceDim, minPrice, maxPrice, simulationManager);
		
		this.scaleFunDerivative = (this.maxPrice - this.minPrice) / (maxActorOut - minActorOut);
		
		this.actorFilePath = this.directory + AgentFileTypes.ACTOR.getFileName();
		this.actorTargetFilePath = this.directory + AgentFileTypes.ACTOR_TARGET.getFileName();
		this.criticFilePath = this.directory + AgentFileTypes.CRITIC.getFileName();
		this.criticTargetFilePath = this.directory + AgentFileTypes.CRITIC_TARGET.getFileName();
		this.memoryFilePath = this.directory + AgentFileTypes.EXPERIENCE_REPLAY.getFileName();
		this.noiseCoeffFilePath = this.directory + AgentFileTypes.NOISE_COEFF.getFileName();
		
		if (train) {
			// Check whether all model files can be found and try to load the models from them
			boolean filesFound = this.checkAndLoadModelFiles();
			// If all the files were not found, initialize new models
			if (!filesFound) {
				long actorSeed = this.simulationManager.seedGenerator.nextLong();
				long criticSeed = this.simulationManager.seedGenerator.nextLong();
				
				// Same seed guarantees that the actor and its target model are initialized with the exactly same parameters
				this.actor = this.getActorModel(actorSeed, 64, 64); // Initialize actor network
				this.actorTarget = this.getActorModel(actorSeed, 64, 64); // Initialize target actor network with the same params as the actor network
				
				// Same seed guarantees that the critic and its target model are initialized with the exactly same parameters
				this.critic = this.getCriticModel(criticSeed, 64, 64); // Initialize critic network
				this.criticTarget = this.getCriticModel(criticSeed, 64, 64); // Initialize target critic network with the same params as the critic network
			}
			
			// Initialize experience replay
			ArrayList<ExperienceTuple> loadedMemoryContent = this.getMemoryContentFromFile(); // Deserialize experience replay content from file
			// If the file existed, create a new experience replay with the loaded memory content, otherwise create a new, empty experience replay
			this.memory = loadedMemoryContent != null 
					? new ExperienceReplay(replayBufferSize, batchSize, loadedMemoryContent, this.simulationManager)
					: new ExperienceReplay(replayBufferSize, batchSize, this.simulationManager);
			
			// Load noise coefficient
			this.noiseCoeff = this.getNoiseCoeffFromFile();
			
		} else {
			// If the simulation is run in the evaluation mode, only the actor network is needed
			// The method on the following line will try to load the actor model from the file and will throw an exception if it does not succeed.
			this.actor = this.loadModelFromFile(new File(actorFilePath), false);
		}
	}

	/**
	 * Uses actor network to decide the action in the current state. 
	 * If the simulation is run in the training mode as specified by {@link EisimSimulationParameters#train}, 
	 * noise is added to the chosen action according to the current state of the noise process.
	 * 
	 * @param state 	Current state observation
	 * @return float: 	The action chosen according to the given state
	 */
	@Override
	public float act(INDArray state) {
		// Input the state into actor network
		INDArray actorOutput = this.actor.output(state);
		
		// Get the output as float
		float actorOutputScalar = actorOutput.data().getFloat(0);
		
		// If training mode, add noise
		if (train) {
			// Sampling noise from normal distribution with zero mean and standard deviation of noiseSD
			double noise = this.random.nextGaussian() * noiseSD * this.noiseCoeff;
			actorOutputScalar += noise;
			
			// Clip the noisy output to activation range
			actorOutputScalar = Math.max(minActorOut, Math.min(maxActorOut, actorOutputScalar));
		}
		
		// Scale the output to [minPrice, maxPrice]
		float price = (actorOutputScalar - minActorOut) / (maxActorOut - minActorOut) * (this.maxPrice - this.minPrice) + this.minPrice;
		return price;
	}

	/**
	 * Adds the agent's experience tuple into experience replay and conducts model training.
	 * 
	 * @param state		Initial state
	 * @param action	The action taken in the initial state
	 * @param reward	The reward received
	 * @param nextState The state that followed from the initial state after taking the action
	 */
	@Override
	public void learn(INDArray state, float action, float reward, INDArray nextState) {
		// Transform action and reward into 2D INDArray
		INDArray actionAsArray = Nd4j.create(new float[][] {{action}});
		INDArray rewardAsArray = Nd4j.create(new float[][] {{reward}});
		
		// Add experience to memory
		memory.addExperience(new ExperienceTuple(state, actionAsArray, rewardAsArray, nextState));
		
		// If time to update
		if (this.memory.size() >= batchSize) {
			// Update models specified number of times with random experience batches
			for (int i = 0; i < modelUpdates; i++) {
				// Sample a batch
				ArrayList<ExperienceTuple> batch = memory.getBatch();
				// Train using the batch
				train(batch);
			}
		}
	}

	/**
	 * Saves the agent's state into files at the end of the simulation. 
	 * Saving is only done if the simulation is run in the training mode as specified by {@link EisimSimulationParameters#train}.
	 * <p>
	 * An agent's state consists of its models (actor, critic, actorTarget, criticTarget), experience replay content, and 
	 * noise coefficient.
	 * 
	 * @throws IOException
	 */
	@Override
	public void saveAgentState() throws IOException {
		// The saving is only done if the simulation is run in training mode
		if (train) {
			// Save the networks
			File actorFile = new File(this.actorFilePath);
			File criticFile = new File(this.criticFilePath);
			File actorTargetFile = new File(this.actorTargetFilePath);
			File criticTargetFile = new File(this.criticTargetFilePath);
			
			try {
				this.actor.save(actorFile, true); // true = save also updater state
				this.critic.save(criticFile, true);
				this.actorTarget.save(actorTargetFile, false); // false = do not save updater state as it is not needed for target networks
				this.criticTarget.save(criticTargetFile, false);
			} catch (IOException e) {
				throw new IOException("IOException occurred while saving models for agent " + this.serverName, e);
			}
			
			// Save the experience memory
			ArrayList<ExperienceTuple> memoryContent = this.memory.getMemoryContent();
			
			try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(this.memoryFilePath))) {
				oos.writeObject(memoryContent);
			} catch (IOException e) {
				throw new IOException("IOException occurred while saving the experience memory for agent " + this.serverName, e);
			}
			
			// Save the noise coefficient
			try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(this.noiseCoeffFilePath))) {
				dos.writeFloat(this.noiseCoeff);
			} catch (IOException e) {
				throw new IOException("IOException occurred while saving the noise coefficient for agent " + this.serverName, e);
			}
			
		}
	}

	/**
	 * Loads actor and critic networks, as well as their target network counterparts from files. 
	 * Returns true only and only if all the files existed and all the models were loaded successfully.
	 * 
	 * @return boolean: True if all the model files were found and loaded successfully, otherwise false.
	 */
	protected boolean checkAndLoadModelFiles() {
		File actorFile = new File(actorFilePath);
		File criticFile = new File(criticFilePath);
		File actorTargetFile = new File(actorTargetFilePath);
		File criticTargetFile = new File(criticTargetFilePath);
		if (actorFile.exists() && actorTargetFile.exists() && criticFile.exists() && criticTargetFile.exists()) {
			this.actor = loadModelFromFile(actorFile, true);
			this.actorTarget = loadModelFromFile(actorTargetFile, false);
			this.critic = loadModelFromFile(criticFile, true);
			this.criticTarget = loadModelFromFile(criticTargetFile, false);
			return true;
		}
		return false;
		
	}
	
	/**
	 * Loads a model from a file.
	 * 
	 * @param file					The file where the model was saved (as a File object)
	 * @param loadUpdater 			Whether to also load the state of the model's updater
	 * @return MultiLayerNetwork:	The loaded model
	 */
	protected MultiLayerNetwork loadModelFromFile(File file, boolean loadUpdater) {
		MultiLayerNetwork net = null;
		try {
			net = MultiLayerNetwork.load(file, loadUpdater);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return net;
	}
	
	/**
	 * Deserializes experience replay content from the memory file if the file exists.
	 * 
	 * @return ArrayList<ExperienceTuple>: The memory content if the memory file exists; 
	 * 									   null if the file does not exist or the content is not what expected
	 */
	protected ArrayList<ExperienceTuple> getMemoryContentFromFile() {
		ArrayList<ExperienceTuple> memoryContent = new ArrayList<>(replayBufferSize);
		
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(this.memoryFilePath))) {
			Object content = ois.readObject();
			
			if (content instanceof ArrayList) {
				ArrayList<?> contentList = (ArrayList<?>) content;
				for (Object member : contentList) {
					if (member instanceof ExperienceTuple) {
						memoryContent.add((ExperienceTuple) member);
					} else {
						return null;
					}
				}
			} else {
				return null;
			}
			
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return memoryContent;
	}
	
	/**
	 * Loads the noise coefficient from the corresponding file. 
	 * 
	 * @return float: The loaded value of the coefficient if the file exists, 
	 * 				  otherwise returns the default value of 1.
	 */
	protected float getNoiseCoeffFromFile() {
		float noiseCoeff = 1f;
		try (DataInputStream dis = new DataInputStream(new FileInputStream(this.noiseCoeffFilePath))) {
			noiseCoeff = dis.readFloat();
		} catch (FileNotFoundException e) {
			return noiseCoeff;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return noiseCoeff;
	}
	
	/**
	 * Creates and initializes an actor network.
	 * 
	 * @param seed 		 		 	Seed For Random Number Generator
	 * @param outHidden1 		 	The number of units in the first hidden layer
	 * @param outHidden2 		 	The number of units in the second hidden layer
	 * @return MultiLayerNetwork:	Actor network
	 */
	protected MultiLayerNetwork getActorModel(long seed, int outHidden1, int outHidden2) {
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
	            .seed(seed)
	            .updater(new AMSGrad(learningRateActor))
	            .list()
	            .layer(new DenseLayer.Builder()
	            		.nIn(stateSpaceDim)
	            		.nOut(outHidden1)
	            		.activation(Activation.RELU)
	            		.weightInit(WeightInit.RELU)
	            		.build())
	            .layer(new DenseLayer.Builder()
	            		.nIn(outHidden1)
	            		.nOut(outHidden2)
	            		.activation(Activation.RELU)
	            		.weightInit(WeightInit.RELU)
	            		.build())
	            .layer(new DenseLayer.Builder()
	            		.nIn(outHidden2)
	            		.nOut(actionSpaceDim)
	            		.activation(Activation.TANH)
	            		.weightInit(WeightInit.XAVIER)
	            		.build())
	            .build();
		MultiLayerNetwork actor = new MultiLayerNetwork(conf);
        actor.init();
        return actor;
	}
	
	/**
	 * Creates and initializes a critic network.
	 * 
	 * @param seed 		 		 	Seed For Random Number Generator
	 * @param outHidden1 		 	The number of units in the first hidden layer
	 * @param outHidden2 		 	The number of units in the second hidden layer
	 * @return MultiLayerNetwork:	Critic network
	 */
	protected MultiLayerNetwork getCriticModel(long seed, int outHidden1, int outHidden2) {
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
	            .seed(seed)
	            .updater(new AMSGrad(learningRateCritic))
	            .list()
	            .layer(new DenseLayer.Builder()
	            		.nIn(stateSpaceDim + actionSpaceDim)
	            		.nOut(outHidden1)
	            		.activation(Activation.RELU)
	            		.weightInit(WeightInit.RELU)
	            		.build())
	            .layer(new DenseLayer.Builder()
	            		.nIn(outHidden1)
	            		.nOut(outHidden2)
	            		.activation(Activation.RELU)
	            		.weightInit(WeightInit.RELU)
	            		.build())
	            .layer(new DenseLayer.Builder()
	            		.nIn(outHidden2)
	            		.nOut(1)
	            		.activation(Activation.IDENTITY)
	            		.weightInit(WeightInit.NORMAL)
	            		.build())
	            .build();
		MultiLayerNetwork critic = new MultiLayerNetwork(conf);
        critic.init();
        return critic;
	}
	
	/**
	 * Performs one iteration of training with the given minibatch.
	 * <p>
	 * The method first updates the actor and critic networks over one minibatch, 
	 * then updates the actor and critic target networks according to {@link EisimSimulationParameters#tau}, 
	 * as well as the noise process according to {@link EisimSimulationParameters#noiseDecay}.
	 * 
	 * @param batch An ArrayList of ExperienceTuples that forms a minibatch
	 */
	protected void train(ArrayList<ExperienceTuple> batch) {
		/**************EXTRACTING STATES, ACTIONS, REWARDS AND NEXTSTATES FROM THE MINIBATCH**************/
		// Get states, actions, rewards, nextStates from the experience batch
		INDArray[] statesArray = batch.stream().map(e -> e.getState()).toArray(INDArray[]::new);
		INDArray states = Nd4j.concat(0, statesArray);
		
		INDArray[] actionsArray = batch.stream().map(e -> e.getAction()).toArray(INDArray[]::new);
		INDArray actions = Nd4j.concat(0, actionsArray);
		
		INDArray[] rewardsArray = batch.stream().map(e -> e.getReward()).toArray(INDArray[]::new);
		INDArray rewards = Nd4j.concat(0, rewardsArray);
		
		INDArray[] nextStatesArray = batch.stream().map(e -> e.getNextState()).toArray(INDArray[]::new);
		INDArray nextStates = Nd4j.concat(0, nextStatesArray);
		
		INDArray statesAndActions = Nd4j.hstack(states, actions);
		
		/**************USING TARGET NETWORKS TO CALCULATE THE CRITIC TARGET**************/
		// Predicting nextActions for the nextStates using the actor target network
		INDArray predictedNextActions = this.actorTarget.output(nextStates);
		scaleToPriceRange(predictedNextActions);
		INDArray nextStatesAndPredNextActions = Nd4j.hstack(nextStates, predictedNextActions);
		
		// Predicting the Q values for the nextStates + nextActions with the critic target network
		INDArray predictedNextQvalues = this.criticTarget.output(nextStatesAndPredNextActions);
		
		// The target for updating the critic network is rewards + discountFactor * predictedQvalues
		INDArray targetForCritic = predictedNextQvalues.muli(discountFactor).addi(rewards);
		
		/**************CRITIC UPDATE**************/
		this.critic.setInput(statesAndActions);
		
		// Doing a forward pass through critic without clearing input activations as they are needed to calculate gradients
		List<INDArray> activationsCritic = this.critic.feedForward(true, false); // true = training mode, false = do not clear inputs
		
		// The loss L for the critic is L = MSE(predictedQvalues, targetForCritic), where predictedQvalues = critic(states, actions)
		// The following line calculates the error signal for the critic, which is needed in backpropGradient() method 
		// The error signal is the gradient of the MSE loss with regard to critic output (Q-values), that is, dL/dQvals
		INDArray errorForCritic = activationsCritic.get(activationsCritic.size()-1).subi(targetForCritic).muli(2); // shape [batchSize, nOut], nOut = 1
		
		// Do backpropagation (calculate gradients) based on the error signal
		Gradient gradientForCritic = this.critic.backpropGradient(errorForCritic, null).getFirst();
		
		// Updating the gradient for critic: applying learning rate, momentum, etc. (the Gradient object is modified in-place)
        int iteration = 0;
        int epoch = 0;
        this.critic.getUpdater().update(this.critic, gradientForCritic, iteration, epoch, batchSize, LayerWorkspaceMgr.noWorkspaces());
        
        //Get the gradient array as a row vector and apply it to the parameters to update the critic (in-place)
        INDArray updateVectorForCritic = gradientForCritic.gradient();
        this.critic.params().subi(updateVectorForCritic);
        
        /**************ACTOR UPDATE**************/
        this.actor.setInput(states);
        
        // Doing a forward pass through actor without clearing the input activations in each layer, as those are needed for calculating gradients in backpropagation
        List<INDArray> activationsActor = this.actor.feedForward(true, false);
		
        // Scaling the actor outputs to [minPrice, maxPrice] (predicting pricing actions for the given states; predictedActions = scaleFun(actor(states)))
        INDArray predictedActions = activationsActor.get(activationsActor.size()-1);
        scaleToPriceRange(predictedActions);
        INDArray statesAndPredActions = Nd4j.hstack(states, predictedActions);
        
        // The loss L for actor is L = -1 * mean(critic(states, predictedActions)) = -1 * mean(Qvalues)
        // Need to calculate dL/da (error signal for backpropGradient() method), 'a' being the final activation output from the actor
        /* 
         * Note that when calculating the gradient of the loss with regard to critic input with backpropGradient() method, 
         * gradients are also calculated for the critic's parameters, even though they are not needed. (DL4J does not currently support 
         * any convenient way to get the gradient with regard to network input without calculating that for the parameters as well, 
         * such as setting 'requires_grad=False' for critic params in PyTorch)
         * Also, feedforward and backpropagation through critic are done in training mode, which is problematic in some cases, such 
         * as when the network is configured to use parameter noise during training. (When actor is updated, critic is not trained 
         * so such noise schemes should not be used in critic, but the following code would also apply noise to critic parameters. 
         * Not a problem for the networks in the current implementation, but it should be remembered that the following is not a 
         * working solution for every type of network configuration.)
         */
        this.critic.setInput(statesAndPredActions);
        this.critic.feedForward(true, false);
        INDArray criticErrorForActorLoss = Nd4j.valueArrayOf(batchSize, 1, -1); // dL/dQval for each sample in minibatch
        INDArray epsilonInput = this.critic.backpropGradient(criticErrorForActorLoss, null).getSecond(); //dL/dSa
        /* 
         * epsilonInput corresponds to the gradient of the actor loss with regard to critic input, 
         * that is, epsilonInput = dL/dSa, where Sa is the state variables + action variable input to the critic. 
         * The following code line gets the partial derivatives for the action variable 'a_scaled', which is the actor output 'a' 
         * scaled to [minPrice, maxPrice]. 
         * The length of the critic input is stateSpaceDim + actionSpaceDim, and actionSpaceDim = 1 (cannot be anything else in 
         * this implementation); hence getting the column from index 'stateSpaceDim' corresponds to the action variable column.
         */
        INDArray epsilonAction = epsilonInput.getColumns(stateSpaceDim); // dL/da_scaled
        epsilonAction.muli(this.scaleFunDerivative); //dL/da; scaleFunDerivative = da_scaled/da
        
        // Do backpropagation (calculate gradients) based on the error signal for actor (based on dL/da)
        Gradient gradientForActor = this.actor.backpropGradient(epsilonAction, null).getFirst();
     		
     	// Updating the gradient for actor: applying learning rate, momentum, etc. (the Gradient object is modified in-place)
        this.actor.getUpdater().update(this.actor, gradientForActor, iteration, epoch, batchSize, LayerWorkspaceMgr.noWorkspaces());
             
        //Get the gradient array as a row vector and apply it to the parameters to update the actor (in-place)
        INDArray updateVectorForActor = gradientForActor.gradient();
        this.actor.params().subi(updateVectorForActor);
		
        /**************UPDATING TARGET NETWORKS**************/
		this.actorTarget.params().muli(1-tau).addi(this.actor.params().dup().muli(tau));
		this.criticTarget.params().muli(1-tau).addi(this.critic.params().dup().muli(tau));
        
        /**************UPDATING NOISE ACCORDING TO DECAY**************/
		this.noiseCoeff -= noiseDecay;
	}
	
	/**
	 * Scales the actor output from the range [minActorOut, maxActorOut] to the price range [minPrice, maxPrice]. 
	 * The scaling is done in-place to the referenced array.
	 * 
	 * @param actorOutput INDArray that contains output activations from the actor network
	 */
	protected void scaleToPriceRange(INDArray actorOutput) {
    	actorOutput.subi(minActorOut).divi(maxActorOut - minActorOut).muli(this.maxPrice - this.minPrice).addi(this.minPrice);
    }

}
