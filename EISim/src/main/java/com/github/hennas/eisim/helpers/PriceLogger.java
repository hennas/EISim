package com.github.hennas.eisim.helpers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.nd4j.linalg.api.ndarray.INDArray;

import com.github.hennas.eisim.EisimSimulationParameters;
import com.github.hennas.eisim.agents.PricingAgent;
import com.github.hennas.eisim.core.scenariomanager.Scenario;
import com.github.hennas.eisim.core.scenariomanager.SimulationParameters;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager;
import com.github.hennas.eisim.defaultclasses.EisimComputingNode;

/**
 * Logs the prices, profits, states and cumulative return of a pricing agent into a CSV file. 
 * 
 * @see EisimComputingNode
 * @see PricingAgent
 * 
 * @author Henna Kokkonen
 *
 */
public class PriceLogger {
	
	protected ArrayList<String> priceLog = new ArrayList<String>();
	protected DecimalFormat decimalFormat;
	protected String serverName;
	protected String simStartTime;
	protected SimulationManager simulationManager;
	protected double cumulativeProfit = 0.0;
	
	/**
	 * Initialize a price logger.
	 * 
	 * @param simulationManager The simulation manager that links between the different modules
	 * @param serverName		The unique name of the server node to which the PriceLogger instance belongs
	 */
	public PriceLogger(SimulationManager simulationManager, String serverName) {
		this.simulationManager = simulationManager;
		this.simStartTime = simulationManager.getSimulationLogger().getSimStartTime();
		this.serverName = serverName;
		
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.GERMAN);
		otherSymbols.setDecimalSeparator('.'); // use the dot "." as separation symbol, since the comma "," is used in CSV files as a separator
		this.decimalFormat = new DecimalFormat("######.####", otherSymbols);

		// Add the CSV file header
		this.priceLog.add("SimTime,Price,Profit,CumulativeProfit,State");
	}
	
	/**
	 * Adds a line to the price log. Records the price, profit and state of the past price slot, 
	 * as well as the total cumulative profit up to the current simulation time.
	 * 
	 * @param simTime	The current simulation time
	 * @param price		The price used during the slot
	 * @param profit	The profit gained during the slot
	 * @param state		The state at the beginning of the slot
	 */
	public void addLine(double simTime, float price, double profit, INDArray state) {
		if (state != null) {
			cumulativeProfit += profit;
			String stateStr = Arrays.toString(state.data().asFloat()).replace(",", ";");
			priceLog.add(decimalFormat.format(simTime) + "," + price + "," + profit + "," + cumulativeProfit + "," + stateStr);
		}
	}
	
	/**
	 * Saves the log into a CSV file.
	 * 
	 * @throws IOException
	 */
	public void saveLog() throws IOException {
		writeFile(getPathName(".csv"), getPriceLog());
	}
	
	/**
	 * Creates a path name for saving the price log.
	 * <p>
	 * The created path to the price log file is 
	 * <outputFolder>/<simulationStartTime>/Pricelogs_<simulationScenario>/<serverName>_log.<extension>. 
	 * outputFolder is specified by {@link EisimSimulationParameters#outputFolder}.
	 * 
	 * @param extension 	The file extension to use
	 * @return
	 */
	protected String getPathName(String extension) {
		Scenario scenario = this.simulationManager.getScenario();
		String scenarioName = "scenario_"
				+ scenario.getStringOrchAlgorithm() + "_" 
				+ scenario.getStringOrchArchitecture() + "_" 
				+ scenario.getDevicesCount();
		
		String outputFilePathName = SimulationParameters.outputFolder + "/" + simStartTime
				+ "/Pricelogs_" + scenarioName;
		new File(outputFilePathName).mkdirs();
		outputFilePathName += "/" + serverName + "_log";

		return outputFilePathName + extension;
	}
	
	protected void writeFile(String pathName, List<String> lines) throws IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(pathName, true))) {
			for (String str : lines) {
				bufferedWriter.append(str);
				bufferedWriter.newLine();
			}
			lines.clear();
		} catch (IOException e) {
			throw new IOException("IOException occurred while saving the price log for edge server " + this.serverName, e);
		}
	}
	
	protected List<String> getPriceLog() {
		return this.priceLog;
	}
}
