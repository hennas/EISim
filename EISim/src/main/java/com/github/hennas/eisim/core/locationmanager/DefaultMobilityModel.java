package com.github.hennas.eisim.core.locationmanager;

import java.util.Random;

import com.github.hennas.eisim.core.scenariomanager.SimulationParameters;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager;
import com.github.hennas.eisim.defaultclasses.EisimSimulationManager;

public class DefaultMobilityModel extends MobilityModel {
	/**
	 * Used to generate random values.
	 * 
	 * @see #pause
	 * @see #reoriontate(double, double)
	 */
	protected Random random;
	protected boolean pause = false;
	protected double pauseDuration = -1;
	protected double mobilityDuration;
	protected int orientationAngle;

	public DefaultMobilityModel(SimulationManager simulationManager, Location currentLocation) {
		super(simulationManager, currentLocation);
		random = new Random();
		random.setSeed(((EisimSimulationManager) simulationManager).seedGenerator.nextLong());
		orientationAngle = random.nextInt(359);
	}

	@Override
	protected Location getNextLocation(Location newLocation) {
		double xPosition = newLocation.getXPos(); // Get the initial X coordinate assigned to this device
		double yPosition = newLocation.getYPos(); // Get the initial y coordinate assigned to this device

		if (pause && pauseDuration > 0) {
			// The device mobility is paused until that random delay finishes
			pauseDuration -= SimulationParameters.updateInterval;
			return newLocation;
		}

		// Make sure that the device stay in the simulation area
		reoriontate(xPosition, yPosition);

		if (mobilityDuration <= 0) {
			pause();
		}

		if (pauseDuration <= 0) {
			resume();
		}

		// Update the currentLocation of this device
		return updateLocation(xPosition, yPosition);

	}

	protected Location updateLocation(double xPosition, double yPosition) {
		double distance = getSpeed() * SimulationParameters.updateInterval;
		double X_distance = Math.cos(Math.toRadians(orientationAngle)) * distance;
		double Y_distance = Math.sin(Math.toRadians(orientationAngle)) * distance;
		// Update the xPosition
		double X_pos = xPosition + X_distance;
		double Y_pos = yPosition + Y_distance;
		return new Location(X_pos, Y_pos);
	}

	protected void resume() {
		// Resume mobility in the next iteration
		pause = false;
		// Increment time and then calculate the next coordinates in the next iteration
		// (the device is moving)
		mobilityDuration -= SimulationParameters.updateInterval;
	}

	protected void pause() {
		// Pickup random duration from 50 to 200 seconds
		pauseDuration = getMinPauseDuration()
				+ random.nextInt((int) (getMaxPauseDuration() - getMinPauseDuration()));
		// Pause mobility (the device will stay in its location for the randomly
		// generated duration
		pause = true;
		// Reorientate the device to a new direction
		orientationAngle = random.nextInt(359);
		// The mobility will be resumed for the following period of time
		mobilityDuration = random.nextInt((int) (getMaxMobilityDuration() - getMinMobilityDuration()))
				+ getMinMobilityDuration();
	}

	protected void reoriontate(double xPosition, double yPosition) {
		if (xPosition >= SimulationParameters.simulationMapWidth)
			orientationAngle = -90 - random.nextInt(180);
		else if (xPosition <= 0)
			orientationAngle = -90 + random.nextInt(180);
		if (yPosition >= SimulationParameters.simulationMapLength)
			orientationAngle = -random.nextInt(180);
		else if (yPosition <= 0)
			orientationAngle = random.nextInt(180);
	}

}
