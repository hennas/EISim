package com.github.hennas.eisim.core.scenariomanager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.hennas.eisim.core.simulationmanager.SimLog;
import com.github.hennas.eisim.core.taskgenerator.Application;

public class ApplicationFileParser extends XmlFileParser {

	public ApplicationFileParser(String file) {
		super(file);
	}

	@Override
	public boolean parse() {
		return checkAppFile();
	}

	protected boolean checkAppFile() {
		String condition = "> 0. Check the \"";
		String application = "\" application in \"";
		SimLog.println("%s - Checking applications file.", this.getClass().getSimpleName());
		SimulationParameters.applicationList = new ArrayList<>();
		Document doc;
		try (InputStream applicationFile = new FileInputStream(file)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(applicationFile);
			doc.getDocumentElement().normalize();

			NodeList appList = doc.getElementsByTagName("application");
			int percentage = 0;
			for (int i = 0; i < appList.getLength(); i++) {
				Node appNode = appList.item(i);

				Element appElement = (Element) appNode;
				isAttributePresent(appElement, "name");

				for (String element : List.of("type", "poissonRate", "usagePercentage", "latency", 
						"containerSizeMin", "containerSizeMax", "requestSizeMin", "requestSizeMax",
						"resultRatioMin", "resultRatioMax", "taskLength"))
					isElementPresent(appElement, element);

				// The type of application.
				String type = appElement.getElementsByTagName("type").item(0).getTextContent();
				
				// The Poisson generation rate (tasks per second)
				double rate = assertDouble(appElement, "poissonRate", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file + "\" file");

				// The percentage of devices using this type of application.
				int usagePercentage = (int) assertDouble(appElement, "usagePercentage", value -> (value > 0 && value <= 100),
						"> 0 and <= 100. Check the \"" + appElement.getAttribute("name") + application + file + "\" file");
				percentage += usagePercentage;
				
				// Latency-sensitivity in seconds.
				double latency = assertDouble(appElement, "latency", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file + "\" file");

				// The minimum size of the container (bits).
				long containerSizeMin = (long) (8000 * assertDouble(appElement, "containerSizeMin", value -> (value >= 0),
						">= 0. Check the \"" + appElement.getAttribute("name") + application + file + "\" file"));
				
				// The maximum size of the container (bits).
				long containerSizeMax = (long) (8000 * assertDouble(appElement, "containerSizeMax", value -> (value >= 0),
						">= 0. Check the \"" + appElement.getAttribute("name") + application + file + "\" file"));
				
				// The minimum size of the request (bits).
				long requestSizeMin = (long) (8000 * assertDouble(appElement, "requestSizeMin", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file + "\" file"));

				// The maximum size of the request (bits).
				long requestSizeMax = (long) (8000 * assertDouble(appElement, "requestSizeMax", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file + "\" file"));
				
				// The minimum ratio for result size (ratio of request size).
				double resultRatioMin = assertDouble(appElement, "resultRatioMin", value -> (value > 0 && value <= 1),
						"> 0 and <= 1. Check the \"" + appElement.getAttribute("name") + application + file + "\" file");
				
				// The maximum ratio for result size (ratio of request size).
				double resultRatioMax = assertDouble(appElement, "resultRatioMax", value -> (value > 0 && value <= 1),
						"> 0 and <= 1. Check the \"" + appElement.getAttribute("name") + application + file + "\" file");
				
				// Average task length (MI) for exponential distribution.
				long taskLength = (long) assertDouble(appElement, "taskLength", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file + "\" file");
				
				// Checking that minimas are not bigger than maximas
				assertMinMax(containerSizeMin, containerSizeMax, "containerSizeMin", "containerSizeMax", 
						"Check the \"" + appElement.getAttribute("name") + application + file + "\" file");
				assertMinMax(requestSizeMin, requestSizeMax, "requestSizeMin", "requestSizeMax", 
						"Check the \"" + appElement.getAttribute("name") + application + file + "\" file");
				assertMinMax(resultRatioMin, resultRatioMax, "resultRatioMin", "resultRatioMax", 
						"Check the \"" + appElement.getAttribute("name") + application + file + "\" file");
				
				// Save applications parameters.
				SimulationParameters.applicationList.add(new Application(type, rate, usagePercentage, latency,
						containerSizeMin, containerSizeMax, requestSizeMin, requestSizeMax,
						resultRatioMin, resultRatioMax, taskLength));
			}
			if (percentage != 100) {
				throw new IllegalArgumentException(getClass().getSimpleName()
						+ " - Check the \"applications.xml\" file! The sum of usage percentages must be equal to 100%.");
			}

		} catch (Exception e) {
			SimLog.println("%s - Applications XML file cannot be parsed!", this.getClass().getSimpleName());
			e.printStackTrace();
			return false;
		}

		SimLog.println("%s - Applications XML file successfully loaded!", this.getClass().getSimpleName());
		return true;
	}
	
	protected void assertMinMax(double min, double max, String parameterMin, String parameterMax, String message) {
		if (Double.compare(min, max) > 0)
			throw new IllegalArgumentException(
					getClass().getSimpleName() + " - Error, the value of \"" + parameterMin + 
					"\" must be smaller than or equal to \"" + parameterMax + "\". " + message);
	}

}
