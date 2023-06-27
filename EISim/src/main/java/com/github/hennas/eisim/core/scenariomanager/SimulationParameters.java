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
package com.github.hennas.eisim.core.scenariomanager;

import java.util.List;

import com.github.hennas.eisim.core.network.TransferProgress;
import com.github.hennas.eisim.core.simulationmanager.SimLog;
import com.github.hennas.eisim.core.simulationmanager.SimulationAbstract.Files;
import com.github.hennas.eisim.core.taskgenerator.Application;
import com.github.hennas.eisim.core.taskgenerator.Task;

public class SimulationParameters {
	
	/**
	 * The path to the configuration file.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationAbstract#setCustomFilePath(String path, Files file) 
	 */
	public static String simulationParametersFile = "settings/simulation_parameters.properties";

	/**
	 * The path to the applications characteristics file.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationAbstract#setCustomFilePath(String,
	 *      Files)
	 */
	public static String applicationFile = "settings/applications.xml";

	/**
	 * The path to the edge data centers characteristics file.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationAbstract#setCustomFilePath(String,
	 *      Files)
	 */
	public static String edgeDataCentersFile = "settings/edge_datacenters.xml";

	/**
	 * The path to the edge devices characteristics file.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationAbstract#setCustomFilePath(String,
	 *      Files)
	 */
	public static String edgeDevicesFile = "settings/edge_devices.xml";

	/**
	 * The path to the cloud characteristics file.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationAbstract#setCustomFilePath(String,
	 *      Files)
	 */
	public static String cloudDataCentersFile = "settings/cloud.xml";

	/**
	 * The output folder path.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationAbstract#setCustomOutputFolder(String)
	 */
	public static String outputFolder = "output/";

	/**
	 * If true simulations will be launched in parallel
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.Simulation#launchSimulation()
	 */
	public static boolean parallelism_enabled = false;

	/**
	 * Simualtion time in seconds.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.DefaultSimulationManager#startInternal()
	 */
	public static double simulationDuration;

	/**
	 * Pause between iterations (in seconds)
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimulationThread#pause(SimLog
	 *      simLog)
	 */
	public static int pauseLength;

	/**
	 * Update interval (Mobility and other events) (in seconds)
	 * 
	 */
	public static double updateInterval;

	/**
	 * If true, real-time charts will be displayed
	 * 
	 * @see com.github.hennas.eisim.core.simulationvisualizer.SimulationVisualizer#updateCharts()
	 */
	public static boolean displayRealTimeCharts;

	/**
	 * If true, real-time charts are automatically closed when simulation finishes
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.DefaultSimulationManager#processEvent(com.github.hennas.eisim.core.simulationengine.Event)
	 */
	public static boolean autoCloseRealTimeCharts;

	/**
	 * Charts refresh interval in seconds
	 * 
	 * @see com.github.hennas.eisim.core.simulationvisualizer.SimulationVisualizer#updateCharts()
	 */
	public static double chartsUpdateInterval;

	/**
	 * If true, charts are automatically generated at the end of the simulation and
	 * saved in bitmap format in the {@link SimulationParameters#outputFolder}
	 * 
	 * @see com.github.hennas.eisim.core.simulationvisualizer.SimulationVisualizer#updateCharts()
	 */
	public static boolean saveCharts;

	/**
	 * The length of simulation map in meters.
	 * 
	 * @see com.github.hennas.eisim.core.locationmanager.MobilityModel
	 * @see com.github.hennas.eisim.core.locationmanager.DefaultMobilityModel
	 */
	public static int simulationMapLength;

	/**
	 * The width of simulation map in meters.
	 * 
	 * @see com.github.hennas.eisim.core.locationmanager.MobilityModel
	 * @see com.github.hennas.eisim.core.locationmanager.DefaultMobilityModel
	 */
	public static int simulationMapWidth;

	/**
	 * The number of edge data centers.
	 * 
	 * @see com.github.hennas.eisim.core.scenariomanager.DatacentersParser#typeSpecificChecking(org.w3c.dom.Document)
	 */
	public static int numberOfEdgeDataCenters;

	/**
	 * The number of cloud data centers.
	 * 
	 * @see com.github.hennas.eisim.core.scenariomanager.DatacentersParser#typeSpecificChecking(org.w3c.dom.Document)
	 */
	public static int numberOfCloudDataCenters;

	/**
	 * The minimum number of edge devices.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.Simulation#loadScenarios()
	 */
	public static int minNumberOfEdgeDevices;

	/**
	 * The maximum number of edge devices.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.Simulation#loadScenarios()
	 */
	public static int maxNumberOfEdgeDevices;

	/**
	 * The incremental step of edge devices
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.Simulation#loadScenarios()
	 */
	public static int edgeDevicesIncrementationStepSize;

	/**
	 * The types of computing nodes.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator#generateDatacentersAndDevices()
	 * @see com.github.hennas.eisim.core.datacentersmanager.ComputingNode#setType(TYPES)
	 */
	public enum TYPES {
		CLOUD, EDGE_DATACENTER, EDGE_DEVICE
	}

	/**
	 * Whether deep logging is enabled or not.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimLog#deepLog(String)
	 */
	public static boolean deepLoggingEnabled;

	/**
	 * Whether to save the log or not.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimLog#saveLog()
	 */
	public static boolean saveLog;

	/**
	 * If true, it delete previous logs and simulation results.
	 * 
	 * @see com.github.hennas.eisim.core.simulationmanager.SimLog#cleanOutputFolder()
	 */
	public static boolean cleanOutputFolder;

	/**
	 * The WAN (core+data center network) bandwidth in bits per second.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#setBandwidth(double)
	 */
	public static double wanBandwidthBitsPerSecond;

	/**
	 * The WAN (core+data center network) latency in seconds.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#setLatency(double)
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWanUp
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWanDown
	 */
	public static double wanLatency;

	/**
	 * The WAN (core+data center network) energy consumption in watthour per bit.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#getEnergyModel()
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWanUp
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWanDown
	 */
	public static double wanWattHourPerBit;

	/**
	 * If true, all data sent to /received from the cloud will be transmitted
	 * through the same WAN network (i.e. share the same bandwidth).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#setBandwidth(double)
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWanUp
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWanDown
	 */
	public static boolean useOneSharedWanLink;

	/**
	 * The MAN (the links between edge data centers) bandwidth in bits per second.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#setBandwidth(double)
	 * @see com.github.hennas.eisim.core.network.NetworkLinkMan
	 */
	public static double manBandwidthBitsPerSecond;

	/**
	 * The MAN (the links between edge data centers) latency in seconds.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#setLatency(double)
	 * @see com.github.hennas.eisim.core.network.NetworkLinkMan
	 */
	public static double manLatency;

	/**
	 * The MAN (the links between edge data centers) energy consumption in watthour
	 * per bit.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#getEnergyModel()
	 * @see com.github.hennas.eisim.core.network.NetworkLinkMan
	 */
	public static double manWattHourPerBit;

	/**
	 * The WiFI bandwidth in bits per second.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#setBandwidth(double)
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifi
	 */
	public static double wifiBandwidthBitsPerSecond;

	/**
	 * The energy consumed by the device when transmitting data (in watthour per bit).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#getEnergyModel()
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifi
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifiDeviceToDevice
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifiUp
	 */
	public static double wifiDeviceTransmissionWattHourPerBit;

	/**
	 * The energy consumed by the device when receiving data (in watthour per bit).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#getEnergyModel()
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifi
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifiDeviceToDevice
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifiDown
	 */
	public static double wifiDeviceReceptionWattHourPerBit;

	/**
	 * The energy consumed by the WiFi access point when transmitting data in watthour per bit.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#getEnergyModel()
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifi
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifiDown
	 */
	public static double wifiAccessPointTransmissionWattHourPerBit;

	/**
	 * The energy consumed by the WiFi access point when receiving data in watthour per bit.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#getEnergyModel()
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifi
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifiUp
	 */
	public static double wifiAccessPointReceptionWattHourPerBit;

	/**
	 * The WiFi latency in seconds.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#setLatency(double)
	 * @see com.github.hennas.eisim.core.network.NetworkLinkWifi
	 */
	public static double wifiLatency;

	/**
	 * The Ethernet bandwidth in bits per second.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#setBandwidth(double)
	 * @see com.github.hennas.eisim.core.network.NetworkLinkEthernet
	 */
	public static double ethernetBandwidthBitsPerSecond;

	/**
	 * The Ethernet energy consumption in watthour per bit.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#getEnergyModel()
	 * @see com.github.hennas.eisim.core.network.NetworkLinkEthernet
	 */
	public static double ethernetWattHourPerBit;

	/**
	 * The Ethernet latency in seconds.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#setLatency(double)
	 * @see com.github.hennas.eisim.core.network.NetworkLinkEthernet
	 */
	public static double ethernetLatency;

	/**
	 * The mobile communication/ cellular network (e.g. 3G, 4G, 5G) bandwidth in
	 * bits per second.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#setBandwidth(double)
	 * @see com.github.hennas.eisim.core.network.NetworkLinkCellular
	 */
	public static double cellularBandwidthBitsPerSecond;

	/**
	 * The energy consumed by an edge device when transmitting data using a cellular connection (e.g. 3G,
	 * 4G, 5G) (in watthour per bit).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#getEnergyModel()
	 * @see com.github.hennas.eisim.core.network.NetworkLinkCellularUp 
	 */
	public static double cellularDeviceTransmissionWattHourPerBit;

	/**
	 * The energy consumed by an edge device when receiving data using a cellular connection (e.g. 3G,
	 * 4G, 5G) (in watthour per bit).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#getEnergyModel()
	 * @see com.github.hennas.eisim.core.network.NetworkLinkCellularUp 
	 */
	public static double cellularDeviceReceptionWattHourPerBit;

	/**
	 * The mobile base station uplink network (e.g. 3G, 4G, 5G) energy consumption
	 * (in watthour per bit).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#getEnergyModel()
	 * @see com.github.hennas.eisim.core.network.NetworkLinkCellularUp
	 */
	public static double cellularBaseStationWattHourPerBitUpLink;

	/**
	 * The mobile base station downlink network (e.g. 3G, 4G, 5G) energy consumption
	 * (in watthour per bit).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#getEnergyModel()
	 * @see com.github.hennas.eisim.core.network.NetworkLinkCellularDown
	 */
	public static double cellularBaseStationWattHourPerBitDownLink;

	/**
	 * The mobile communication/ cellular (e.g. 3G, 4G, 5G) network latency in
	 * seconds.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.DefaultTopologyCreator#generateTopologyGraph()
	 * @see com.github.hennas.eisim.core.network.NetworkLink#setLatency(double)
	 * @see com.github.hennas.eisim.core.network.NetworkLinkCellular
	 */
	public static double cellularLatency;

	/**
	 * The WiFi range of edge devices when using a device to device connection( in
	 * meters).
	 * 
	 * @see com.github.hennas.eisim.core.locationmanager.MobilityModel#distanceTo(com.github.hennas.eisim.core.datacentersmanager.ComputingNode)
	 */
	public static int edgeDevicesRange;

	/**
	 * The edge data centers coverage area (in meters) in which edge devices can
	 * connect with them directly (one hop).
	 * 
	 * @see com.github.hennas.eisim.core.locationmanager.MobilityModel#distanceTo(com.github.hennas.eisim.core.datacentersmanager.ComputingNode)
	 */
	public static int edgeDataCentersRange;

	/**
	 * The network model update interval.
	 * 
	 * @see com.github.hennas.eisim.core.network.NetworkLink#startInternal()
	 */
	public static double networkUpdateInterval;

	/**
	 * If true, the network model will be more realistic and gives more accurate
	 * results, but will increase simulation duration.
	 * 
	 * @see com.github.hennas.eisim.core.network.NetworkLink#updateTransfer(TransferProgress
	 *      transfer)
	 */
	public static boolean realisticNetworkModel;

	/**
	 * If true, the tasks will be sent for another computing node (i.e. the
	 * orchestrator) in order to make offlaoding decision, before being sent to the
	 * destination (the node that actually executes the task).
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.ComputingNode#getOrchestrator()
	 * @see examples.Example7
	 */
	public static boolean enableOrchestrators;

	/**
	 * Where the orchestrator(s) are deployed, e.g. on cloud data centers, edge data
	 * centers, edge device, or custom strategy.
	 * 
	 * @see com.github.hennas.eisim.core.datacentersmanager.ComputingNode#getOrchestrator()
	 * @see examples.Example7
	 */
	public static String deployOrchestrators;

	/**
	 * The algorithm that will be used in the simulation to orchestrate the tasks.
	 * 
	 * @see com.github.hennas.eisim.core.taskorchestrator.DefaultOrchestrator#findComputingNode(String[]
	 *      architecture, Task task)
	 */
	public static String[] orchestrationAlgorithms;

	/**
	 * The architecture/paradigms to use in the simulation
	 * 
	 * @see com.github.hennas.eisim.core.taskorchestrator.Orchestrator#orchestrate(Task)
	 */
	public static String[] orchestrationArchitectures;

	/**
	 * If enable, a container will be pulled from the registry before executing the
	 * task.
	 * 
	 * @see examples.Example7
	 */
	public static boolean enableRegistry;

	/**
	 * Sets a custom strategy for downloading containers.
	 * 
	 * @see examples.Example7
	 */
	public static String registryMode;

	/**
	 * The list of applications.
	 * 
	 * @see com.github.hennas.eisim.core.scenariomanager.ApplicationFileParser
	 * @see com.github.hennas.eisim.core.taskgenerator.DefaultTaskGenerator#generate()
	 */
	public static List<Application> applicationList;

	/**
	 * After the end of the simulation time, some tasks may still have not executed yet, 
	 * enabling this will force the simulation to wait for the execution of all tasks.
	 */
	public static boolean waitForAllTasksToFinish;

	/**
	 * How many tasks are scheduled each time (used to avoid scheduling all tasks from the beginning of the simulation).
	 */
	public static int batchSize;

	/**
	 * A private constructor to prevent this class from being instantiated.
	 * 
	 */
	private SimulationParameters () {
		throw new IllegalStateException("SimulationParameters class cannot be instantiated");
	}

}
