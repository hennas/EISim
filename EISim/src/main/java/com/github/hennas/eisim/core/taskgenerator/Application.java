package com.github.hennas.eisim.core.taskgenerator;

/**
 * The instances of this class represent application types for EISim.
 * Replaces the original PureEdgeSim 5.1.0 {@code Application} class.
 * 
 * @author Henna Kokkonen
 *
 */
public class Application {
	protected String type;
	protected double rate; // Poisson rate
	protected int usagePercentage; // The percentage of devices using this application
	protected double latency; // in seconds
	protected long containerSizeMin; // in bits
	protected long containerSizeMax; // in bits
	protected long requestSizeMin; // in bits
	protected long requestSizeMax; // in bits
	protected double resultRatioMin; // ratio of request size
	protected double resultRatioMax; // ratio of request size
	protected long taskLength; // MIs (expected value for exponential distribution)
	
	public Application(String type, double rate, int usagePercentage, double latency, 
			long containerSizeMin, long containerSizeMax, long requestSizeMin, 
			long requestSizeMax, double resultRatioMin, double resultRatioMax,
			long taskLength) {
		setType(type);
		setRate(rate);
		setUsagePercentage(usagePercentage);
		setLatency(latency);
		setContainerSizeMin(containerSizeMin);
		setContainerSizeMax(containerSizeMax);
		setRequestSizeMin(requestSizeMin);
		setRequestSizeMax(requestSizeMax);
		setResultRatioMin(resultRatioMin);
		setResultRatioMax(resultRatioMax);
		setTaskLength(taskLength);
	}

	/**
	 * @return type: the type of the application
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type The type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return rate: the Poisson rate
	 */
	public double getRate() {
		return rate;
	}

	/**
	 * @param rate The Poisson rate to set
	 */
	public void setRate(double rate) {
		this.rate = rate;
	}

	/**
	 * @return usagePercentage: the percentage of devices using this application
	 */
	public int getUsagePercentage() {
		return usagePercentage;
	}

	/**
	 * @param usagePercentage The usagePercentage to set
	 */
	public void setUsagePercentage(int usagePercentage) {
		this.usagePercentage = usagePercentage;
	}

	/**
	 * @return latency in seconds
	 */
	public double getLatency() {
		return latency;
	}

	/**
	 * @param latency The latency to set
	 */
	public void setLatency(double latency) {
		this.latency = latency;
	}

	/**
	 * @return containerSizeMin: the minimum container size in bits
	 */
	public long getContainerSizeMin() {
		return containerSizeMin;
	}

	/**
	 * @param containerSizeMin The containerSizeMin to set
	 */
	public void setContainerSizeMin(long containerSizeMin) {
		this.containerSizeMin = containerSizeMin;
	}

	/**
	 * @return containerSizeMax: the maximum container size in bits
	 */
	public long getContainerSizeMax() {
		return containerSizeMax;
	}

	/**
	 * @param containerSizeMax The containerSizeMax to set
	 */
	public void setContainerSizeMax(long containerSizeMax) {
		this.containerSizeMax = containerSizeMax;
	}

	/**
	 * @return requestSizeMin: the minimum request size in bits
	 */
	public long getRequestSizeMin() {
		return requestSizeMin;
	}

	/**
	 * @param requestSizeMin The requestSizeMin to set
	 */
	public void setRequestSizeMin(long requestSizeMin) {
		this.requestSizeMin = requestSizeMin;
	}

	/**
	 * @return requestSizeMax: the maximum request size in bits
	 */
	public long getRequestSizeMax() {
		return requestSizeMax;
	}

	/**
	 * @param requestSizeMax The requestSizeMax to set
	 */
	public void setRequestSizeMax(long requestSizeMax) {
		this.requestSizeMax = requestSizeMax;
	}

	/**
	 * @return resultRatioMin: the minimum ratio of request size (for determining the result size in bits)
	 */
	public double getResultRatioMin() {
		return resultRatioMin;
	}

	/**
	 * @param resultRatioMin The resultRatioMin to set
	 */
	public void setResultRatioMin(double resultRatioMin) {
		this.resultRatioMin = resultRatioMin;
	}

	/**
	 * @return resultRatioMax: the maximum ratio of request size (for determining the result size in bits)
	 */
	public double getResultRatioMax() {
		return resultRatioMax;
	}

	/**
	 * @param resultRatioMax The resultRatioMax to set
	 */
	public void setResultRatioMax(double resultRatioMax) {
		this.resultRatioMax = resultRatioMax;
	}

	/**
	 * @return taskLength: The average task length in MIs (expected value of an exponential distribution)
	 */
	public long getTaskLength() {
		return taskLength;
	}

	/**
	 * @param taskLength The taskLength to set
	 */
	public void setTaskLength(long taskLength) {
		this.taskLength = taskLength;
	}

	

}
