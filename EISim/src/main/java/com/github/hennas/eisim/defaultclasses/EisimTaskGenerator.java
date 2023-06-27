package com.github.hennas.eisim.defaultclasses;

import java.lang.reflect.Constructor;
import java.util.Random;

import com.github.hennas.eisim.core.simulationengine.FutureQueue;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager;
import com.github.hennas.eisim.core.datacentersmanager.ComputingNode;
import com.github.hennas.eisim.core.scenariomanager.SimulationParameters;
import com.github.hennas.eisim.core.taskgenerator.Task;
import com.github.hennas.eisim.core.taskgenerator.TaskGenerator;

/**
 * Generates all the tasks according to the application types and usage percentages specified in
 * applications.xml setting file.
 *     
 * @author Henna Kokkonen
 * 
 */
public class EisimTaskGenerator extends TaskGenerator {

	protected Random random;
	protected int id = 0;
	
	public EisimTaskGenerator(SimulationManager simulationManager) {
		super(simulationManager);
		random = new Random();
		random.setSeed(((EisimSimulationManager) simulationManager).seedGenerator.nextLong());
		// Setting own custom task model
		setCustomTaskClass(EisimTask.class);
	}

	@Override
	public FutureQueue<Task> generate() {
		// Remove devices that do not generate
		int dev = 0;
		while (dev < devicesList.size()) {
			if (!devicesList.get(dev).isGeneratingTasks()) {
				devicesList.remove(dev);
			} else
				dev++;
		}
		int devicesCount = devicesList.size();

		// Browse all applications
		for (int app = 0; app < SimulationParameters.applicationList.size() - 1; app++) {
			// Get the number of devices that use the current application
			int numberOfDevices = (int) SimulationParameters.applicationList.get(app).getUsagePercentage()
					* devicesCount / 100;

			for (int i = 0; i < numberOfDevices; i++) {
				// Pickup a random application type for every device
				dev = random.nextInt(devicesList.size());

				// Assign this application to that device
				devicesList.get(dev).setApplicationType(app);

				generateTasksForDevice(devicesList.get(dev), app);

				// Remove this device from the list
				devicesList.remove(dev);
			}
		}
		for (int j = 0; j < devicesList.size(); j++)
			generateTasksForDevice(devicesList.get(j), SimulationParameters.applicationList.size() - 1);

		return this.getTaskList();
	}

	protected void generateTasksForDevice(ComputingNode dev, int app) {
		// Generating tasks that will be offloaded during simulation
		double time = 0;
		double interarrivalTime = 0;
		double rate = SimulationParameters.applicationList.get(app).getRate(); // Tasks per second; Poisson arrival rate
		while (true) {
			// In Poisson process, interarrival times are exponentially distributed, so we can generate an interarrival time based on the inverse CDF of exponential distribution
			interarrivalTime = Math.log(1-random.nextDouble())/(-rate); 
			time += interarrivalTime; // Get the arrival time of a task by adding the randomly generated interarrival time
			if (time >= SimulationParameters.simulationDuration) {
				break;
			}
			insert(time, app, dev);
		}
		
		/** The above generates continuous arrival times for the tasks. 
		 * To get discrete (integer) arrival times, the rate could refer to the probability of a task arrival in a unit time (Bernoulli process)
		for (int time = 1; time <= SimulationParameters.simulationDuration; time++) {
			if (random.nextDouble() <= rate) {
				insert(time, app, dev);
			}
		}*/
	}

	protected void insert(double time, int app, ComputingNode dev) {
		// Get the task latency sensitivity (seconds)
		double maxLatency = SimulationParameters.applicationList.get(app).getLatency();

		// Get the average task length (MI: million instructions)
		long avgLength = SimulationParameters.applicationList.get(app).getTaskLength();
		// Sample a task length from an exponential distribution and cast it to long type
		long taskLength = (long) ((-avgLength) * Math.log(1-random.nextDouble()));

		// Get the minimum value for request size in bits
		long minValueRequest = SimulationParameters.applicationList.get(app).getRequestSizeMin();
		// Get the maximum value for request size in bits
		long maxValueRequest = SimulationParameters.applicationList.get(app).getRequestSizeMax();
		// Draw a random value for offloading request size in bits if min < max
		long requestSize = minValueRequest < maxValueRequest ?
				random.nextLong(minValueRequest, (maxValueRequest + 1)) : maxValueRequest;
		
		// Get the minimum value for container size in bits
		long minValueContainer = SimulationParameters.applicationList.get(app).getContainerSizeMin();
		// Get the maximum value for container size in bits
		long maxValueContainer = SimulationParameters.applicationList.get(app).getContainerSizeMax();
		
		long containerSize;
		if (minValueContainer == 0) {
			// Set the container size equal to the request size
			containerSize = requestSize;
		} else if (minValueContainer < maxValueContainer) {
			// Draw a random value
			containerSize = random.nextLong(minValueContainer, (maxValueContainer + 1));
		} else {
			containerSize = maxValueContainer;
		}
				
		// Get the minimum value for ratio
		double minRatio = SimulationParameters.applicationList.get(app).getResultRatioMin();
		// Get the maximum value for ratio
		double maxRatio = SimulationParameters.applicationList.get(app).getResultRatioMax();
		// Draw a random value for ratio size if min < max
		double ratio = minRatio < maxRatio ?
				random.nextDouble(minRatio, maxRatio) : maxRatio;
		// Get the size of the returned results in bits
		long outputSize = (long) (requestSize * ratio);

		// Create the task
		id++;
		Task task = createTask(id);
		task.setType(SimulationParameters.applicationList.get(app).getType());
		task.setTime(time);
		task.setFileSizeInBits(requestSize).setOutputSizeInBits(outputSize);
		task.setContainerSizeInBits(containerSize);
		task.setApplicationID(app);
		task.setMaxLatency(maxLatency);
		task.setLength(taskLength);
		task.setEdgeDevice(dev); // the device that generate this task (the origin)
		// Default registry could be set here, if needed. E.g., to set the cloud as registry
		//task.setRegistry(getSimulationManager().getDataCentersManager().getComputingNodesGenerator().getCloudOnlyList().get(0));
					
		taskList.add(task);
		getSimulationManager().getSimulationLogger()
				.deepLog("TaskGenerator - Task " + id + " with an offloading time of " + time + " (s) generated. "
						+ "Length: " + taskLength + " (MIs) Request size: " + requestSize + " (bits) Output size: " + outputSize + " (bits)");
	}

	protected Task createTask(int id) {
		Constructor<?> taskConstructor;
		Task task = null;
		try {
			taskConstructor = taskClass.getConstructor(int.class);
			task = (Task) taskConstructor.newInstance(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return task;
	}

}
