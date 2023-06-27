package com.github.hennas.eisim.defaultclasses;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.github.hennas.eisim.core.datacentersmanager.ComputingNode;
import com.github.hennas.eisim.core.datacentersmanager.DefaultComputingNodesGenerator;
import com.github.hennas.eisim.core.locationmanager.MobilityModel;
import com.github.hennas.eisim.core.simulationmanager.SimulationManager;
import com.github.hennas.eisim.core.energy.EnergyModelComputingNode;
import com.github.hennas.eisim.core.locationmanager.Location;
import com.github.hennas.eisim.core.scenariomanager.SimulationParameters;

/**
 * Generates all the nodes from the setting files.
 * 
 * @author Henna Kokkonen
 *
 */
public class EisimComputingNodesGenerator extends DefaultComputingNodesGenerator {

	/**
	 * Used for randomly locating the edge devices on the simulation map
	 * 
	 * @see #createComputingNode
	 */
	protected Random random;
	
	/**
	 * Initializes the Computing nodes generator.
	 *
	 * @param simulationManager  The simulation Manager
	 * @param mobilityModelClass The mobility model that will be used in the simulation
	 * @param computingNodeClass The computing node class that will be used to generate computing 
	 * 							 resources
	 */
	public EisimComputingNodesGenerator(SimulationManager simulationManager,
			Class<? extends MobilityModel> mobilityModelClass, Class<? extends ComputingNode> computingNodeClass) {
		super(simulationManager, mobilityModelClass, computingNodeClass);
		random = new Random();
		random.setSeed(((EisimSimulationManager) simulationManager).seedGenerator.nextLong());
	}
	
	@Override
	public void generateDatacentersAndDevices() {

		// Generate Cloud data centers.
		generateDataCenters(SimulationParameters.cloudDataCentersFile, SimulationParameters.TYPES.CLOUD); 

		// Generate Edge datacenters and APs
		generateEdgeDataCentersAndAPs(SimulationParameters.edgeDataCentersFile);
		
		// Generate edge devices.
		generateEdgeDevices();

		getSimulationManager().getSimulationLogger()
				.print(getClass().getSimpleName() + " - Datacenters and devices were generated");

	}
	
	/**
	 * Generates edge data centers and access points from the edge_datacenters.xml file.
	 * 
	 * @param file The configuration file for edge data centers.
	 */
	protected void generateEdgeDataCentersAndAPs(String file){
		try (InputStream serversFile = new FileInputStream(file)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(serversFile);
			NodeList datacenterList = doc.getElementsByTagName("datacenter");
			for (int i = 0; i < datacenterList.getLength(); i++) {
				Element datacenterElement = (Element) datacenterList.item(i);
				ComputingNode computingNode = parseEdgeDcElement(datacenterElement);
				// Both APs and Edge data centers are added to the following lists
				edgeOnlyList.add(computingNode);
				mistAndEdgeListSensorsExcluded.add(computingNode);
				allNodesList.add(computingNode);
				allNodesListSensorsExcluded.add(computingNode);
				edgeAndCloudList.add(computingNode);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates the computing node corresponding to either an edge data center or an access point.
	 * <p>
	 * The edge data centers and access points are differentiated based on the {@code name} 
	 * attribute in the {@code datacenter} element. The name for an edge data center must contain 
	 * {@code dc} and the name for the access point must contain {@code ap}.
	 * <p>
	 * This implementation supports offline clustering of the servers. 
	 * Information about clusters and cluster heads can be given inside a {@code datacenter} element 
	 * by adding the following two elements: 
	 * <ol>
	 * <li>{@code cluster}, which gives the number of the cluster to which an edge server belongs 
	 * (must be integer).</li>
	 * <li>{@code clusterHead}, which indicates whether the edge data center is a cluster head 
	 * (true) or not (false).</li>
	 * </ol>
	 * The above elements are only accounted for if the {@code name} attribute in the 
	 * {@code datacenter} element contains {@code dc}.
	 * 
	 * @param datacenterElement	The XML {@code datacenter} element that encapsulates the specification for an edge node
	 * @return The created computing node corresponding to either edge data center or AP
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws InvocationTargetException
	 * @see #generateEdgeDataCentersAndAPs(String)
	 */
	protected ComputingNode parseEdgeDcElement(Element datacenterElement) 
			throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
		Boolean mobile = false;
		double speed = 0;
		double minPauseDuration = 0;
		double maxPauseDuration = 0;
		double minMobilityDuration = 0;
		double maxMobilityDuration = 0;
		Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
		int xPosition = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
		int yPosition = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
		Location datacenterLocation = new Location(xPosition, yPosition);
		
		for (int i = 0; i < edgeOnlyList.size(); i++)
			if (datacenterLocation.equals(edgeOnlyList.get(i).getMobilityModel().getCurrentLocation()))
				throw new IllegalArgumentException(
						" Each Edge Data Center and Access Point must have a different location, check the \"edge_datacenters.xml\" file!");
		
		Constructor<?> mobilityConstructor = mobilityModelClass.getConstructor(SimulationManager.class, Location.class);
		MobilityModel mobilityModel = ((MobilityModel) mobilityConstructor.newInstance(simulationManager,
				datacenterLocation)).setMobile(mobile).setSpeed(speed).setMinPauseDuration(minPauseDuration)
				.setMaxPauseDuration(maxPauseDuration).setMinMobilityDuration(minMobilityDuration)
				.setMaxMobilityDuration(maxMobilityDuration);
		
		String name = datacenterElement.getAttribute("name");
		
		if (StringUtils.containsIgnoreCase(name, "dc")) {
			double idleConsumption = Double
					.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
			double maxConsumption = Double
					.parseDouble(datacenterElement.getElementsByTagName("maxConsumption").item(0).getTextContent());
			int numOfCores = Integer.parseInt(datacenterElement.getElementsByTagName("cores").item(0).getTextContent());
			double mips = Double.parseDouble(datacenterElement.getElementsByTagName("mips").item(0).getTextContent());
			double storage = Double.parseDouble(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
			double ram = Double.parseDouble(datacenterElement.getElementsByTagName("ram").item(0).getTextContent());
			
			boolean clusterHead = Boolean.parseBoolean(datacenterElement.getElementsByTagName("clusterHead").item(0).getTextContent());
			int cluster = Integer.parseInt(datacenterElement.getElementsByTagName("cluster").item(0).getTextContent());
			
			Constructor<?> datacenterConstructor = computingNodeClass.getConstructor(SimulationManager.class, double.class,
					int.class, double.class, double.class, int.class, boolean.class);
			ComputingNode computingNode = (ComputingNode) datacenterConstructor.newInstance(getSimulationManager(), mips,
					numOfCores, storage, ram, cluster, clusterHead);
			
			computingNode.setEnergyModel(new EnergyModelComputingNode(maxConsumption, idleConsumption));
			computingNode.setName(name);
			computingNode.setPeriphery(Boolean.parseBoolean(datacenterElement.getElementsByTagName("periphery").item(0).getTextContent()));
			computingNode.setType(SimulationParameters.TYPES.EDGE_DATACENTER);
			computingNode.setMobilityModel(mobilityModel);

			computingNode.setAsOrchestrator(Boolean
					.parseBoolean(datacenterElement.getElementsByTagName("isOrchestrator").item(0).getTextContent()));

			if (computingNode.isOrchestrator() || (SimulationParameters.enableOrchestrators
					&& SimulationParameters.deployOrchestrators == "EDGE"))
				orchestratorsList.add(computingNode);
			
			getSimulationManager().getSimulationLogger()
			.deepLog("ComputingNodesGenerator - Edge data center: " + name + "    location: ( "
					+ datacenterLocation.getXPos() + "," + datacenterLocation.getYPos() + " )	cluster: " + cluster + "	head: " + clusterHead);
			
			return computingNode;
			
		} else {
			Constructor<?> datacenterConstructor = computingNodeClass.getConstructor(SimulationManager.class);
			ComputingNode computingNode = (ComputingNode) datacenterConstructor.newInstance(getSimulationManager());
			computingNode.setName(name);
			computingNode.setPeriphery(true);
			computingNode.setType(SimulationParameters.TYPES.EDGE_DATACENTER);
			computingNode.setMobilityModel(mobilityModel);
			computingNode.setAsOrchestrator(false);
			
			getSimulationManager().getSimulationLogger()
			.deepLog("ComputingNodesGenerator - AP: " + name + "    location: ( "
					+ datacenterLocation.getXPos() + "," + datacenterLocation.getYPos() + " )");
			
			return computingNode;
		}
		
	}
	
	/* Creates cloud data center and edge device nodes*/
	@Override
	protected ComputingNode createComputingNode(Element datacenterElement, SimulationParameters.TYPES type)
			throws NoSuchMethodException, InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException {
		Boolean mobile = false;
		double speed = 0;
		double minPauseDuration = 0;
		double maxPauseDuration = 0;
		double minMobilityDuration = 0;
		double maxMobilityDuration = 0;
		int xPosition = -1;
		int yPosition = -1;
		double idleConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
		double maxConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("maxConsumption").item(0).getTextContent());
		Location datacenterLocation = new Location(xPosition, yPosition);
		int numOfCores = Integer.parseInt(datacenterElement.getElementsByTagName("cores").item(0).getTextContent());
		double mips = Double.parseDouble(datacenterElement.getElementsByTagName("mips").item(0).getTextContent());
		double storage = Double.parseDouble(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
		double ram = Double.parseDouble(datacenterElement.getElementsByTagName("ram").item(0).getTextContent());

		Constructor<?> datacenterConstructor = computingNodeClass.getConstructor(SimulationManager.class, double.class,
				int.class, double.class, double.class);
		ComputingNode computingNode = (ComputingNode) datacenterConstructor.newInstance(getSimulationManager(), mips,
				numOfCores, storage, ram);

		computingNode.setAsOrchestrator(Boolean
				.parseBoolean(datacenterElement.getElementsByTagName("isOrchestrator").item(0).getTextContent()));

		if (computingNode.isOrchestrator())
			orchestratorsList.add(computingNode);

		computingNode.setEnergyModel(new EnergyModelComputingNode(maxConsumption, idleConsumption));

		if (type == SimulationParameters.TYPES.EDGE_DEVICE) {
			mobile = Boolean.parseBoolean(datacenterElement.getElementsByTagName("mobility").item(0).getTextContent());
			speed = Double.parseDouble(datacenterElement.getElementsByTagName("speed").item(0).getTextContent());
			minPauseDuration = Double
					.parseDouble(datacenterElement.getElementsByTagName("minPauseDuration").item(0).getTextContent());
			maxPauseDuration = Double
					.parseDouble(datacenterElement.getElementsByTagName("maxPauseDuration").item(0).getTextContent());
			minMobilityDuration = Double.parseDouble(
					datacenterElement.getElementsByTagName("minMobilityDuration").item(0).getTextContent());
			maxMobilityDuration = Double.parseDouble(
					datacenterElement.getElementsByTagName("maxMobilityDuration").item(0).getTextContent());
			computingNode.getEnergyModel().setBattery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("battery").item(0).getTextContent()));
			computingNode.getEnergyModel().setBatteryCapacity(Double
					.parseDouble(datacenterElement.getElementsByTagName("batteryCapacity").item(0).getTextContent()));
			computingNode.getEnergyModel().setIntialBatteryPercentage(Double.parseDouble(
					datacenterElement.getElementsByTagName("initialBatteryLevel").item(0).getTextContent()));
			computingNode.getEnergyModel().setConnectivityType(
					datacenterElement.getElementsByTagName("connectivity").item(0).getTextContent());
			computingNode.enableTaskGeneration(Boolean
					.parseBoolean(datacenterElement.getElementsByTagName("generateTasks").item(0).getTextContent()));
			// Generate random location for edge devices
			datacenterLocation = new Location(random.nextInt(SimulationParameters.simulationMapWidth),
					random.nextInt(SimulationParameters.simulationMapLength));
			getSimulationManager().getSimulationLogger()
					.deepLog("DefaultComputingNodesGenerator- Edge device:" + mistOnlyList.size() + "    location: ( "
							+ datacenterLocation.getXPos() + "," + datacenterLocation.getYPos() + " )");
		}
		computingNode.setType(type);
		Constructor<?> mobilityConstructor = mobilityModelClass.getConstructor(SimulationManager.class, Location.class);
		MobilityModel mobilityModel = ((MobilityModel) mobilityConstructor.newInstance(simulationManager,
				datacenterLocation)).setMobile(mobile).setSpeed(speed).setMinPauseDuration(minPauseDuration)
				.setMaxPauseDuration(maxPauseDuration).setMinMobilityDuration(minMobilityDuration)
				.setMaxMobilityDuration(maxMobilityDuration);

		computingNode.setMobilityModel(mobilityModel);

		return computingNode;
	}

}
