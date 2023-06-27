package com.github.hennas.eisim.core.scenariomanager;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.hennas.eisim.core.scenariomanager.SimulationParameters.TYPES;

public class DatacentersParser extends ComputingNodesParser {

	public DatacentersParser(String file, TYPES type) {
		super(file, type);
	}

	@Override
	protected boolean typeSpecificChecking(Document xmlDoc) {
		NodeList datacenterList = xmlDoc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			for (String element : List.of("isOrchestrator", "idleConsumption", "maxConsumption", "cores", "mips", "ram",
					"storage"))
				isElementPresent(datacenterElement, element);

			for (String element : List.of("cores", "mips", "ram", "storage"))
				assertDouble(datacenterElement, element, value -> (value > 0), "> 0. Check the file: " + file );

			assertDouble(datacenterElement, "idleConsumption", value -> (value >= 0),
					">= 0. Check the file " + file);
			double idleConsumption = Double
					.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
			assertDouble(datacenterElement, "maxConsumption", value -> (value > idleConsumption),
					"> \"idleConsumption\". Check the file " + file);

			if (type == TYPES.CLOUD) {
				SimulationParameters.numberOfCloudDataCenters++;
			} else {
				isAttributePresent(datacenterElement, "name");
				String name = datacenterElement.getAttribute("name");
				if (StringUtils.containsIgnoreCase(name, "dc")) {
					SimulationParameters.numberOfEdgeDataCenters++;
					for (String element : List.of("periphery", "cluster", "clusterHead"))
						isElementPresent(datacenterElement, element);
					
					assertDouble(datacenterElement, "cluster", value -> (value >= 0), "an integer >= 0. Check the " + file + " file!");
					
				} else if (!StringUtils.containsIgnoreCase(name, "ap")) {
					throw new IllegalArgumentException(getClass().getSimpleName()
							+ " - Check the \"edge_datacenters.xml\" file! Every datacenter element must have a name "
							+ "attribute that specifies whether the element represents an edge data center or an access "
							+ "point. The name must contain \"dc\" if and only if the element represents an edge data center, "
							+ "otherwise the name must contain \"ap\".");
				}
				
				Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
				isElementPresent(location, "x_pos");
				isElementPresent(location, "y_pos");
				assertDouble(location, "x_pos", value -> (value >= 0), ">= 0. Check the " + file + " file!");
				assertDouble(location, "y_pos", value -> (value > 0), "> 0. Check the " + file + " file!");
			}

		}
		return true;
	}

}
