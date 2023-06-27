package com.github.hennas.eisim.helpers;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import com.github.hennas.eisim.EisimSimulationParameters;

/**
 * Implements a command-line argument parser. Options and corresponding values are parsed 
 * from a String array that contains all the command-line arguments given to the program.
 * The parsed option values are saved into the {@link EisimSimulationParameters} class, 
 * through which the other classes in the simulation can access them.
 * 
 * @author Henna Kokkonen
 *
 */
public class ArgumentParser {

	protected DefaultParser parser;
	protected HelpFormatter formatter;
	protected Options options;
	protected Options helpOptions;
	protected String helpmsg;
	
	public ArgumentParser() {
		this.parser = new DefaultParser();
		this.formatter = new HelpFormatter();
		getOptions();
		this.helpOptions = new Options();
		helpOptions.addOption(Option.builder("h").longOpt("help").hasArg(false).build());
		this.helpmsg = "mvn -q exec:java -Dexec.mainClass=\"customimplementation.Main\" "
				+ "-Dexec.args=\"-i <setting_folder> -o <output_folder> -m <model_folder> [OTHER OPTIONS]\"";
	}
	
	/**
	 * Parses the command-line arguments given to the program.
	 * 
	 * @param args String array of command-line arguments.
	 * @return True if the given arguments were valid and help option was not used.
	 */
	public boolean parseArguments(String[] args) {
        try {
        	// Check whether help was requested, must be done separately as this.options has required options
        	// The following only works if help is the first option
        	CommandLine cmdHelp = parser.parse(helpOptions, args, true);
        	cmdHelp = parser.parse(helpOptions, args, true);
        	if (cmdHelp.hasOption("help")) {
                formatter.printHelp(helpmsg, "Required options: i, o, m", options, "");
                return false;
            }
            CommandLine cmd = parser.parse(options, args, false);
            // In case that for some reason user specified the required options and also used help option
            if (cmd.hasOption("help")) {
                formatter.printHelp(helpmsg, "Required options: i, o, m", options, "");
                return false;
            }
            setParameters(cmd);
        } catch (ParseException e) {
        	System.out.println(e.getMessage());
        	formatter.printHelp(helpmsg, "Required options: i, o, m", options, "");
        	return false;
        }
        return true;
    }
	
	/**
	 * Creates a collection of Option objects that describe the possible options for a command line.
	 */
	protected void getOptions() {
		this.options = new Options();
		options.addOption(
				Option.builder("h")
						.longOpt("help")
						.hasArg(false)
						.desc("Print this help and exit.")
						.build());
        options.addOption(
                Option.builder("i")
                        .longOpt("input")
                        .hasArg()
                        .argName("setting_folder")
                        .required()
                        .desc("Path to the setting folder that contains the setting files for the simulation.")
                        .build());
        options.addOption(
                Option.builder("o")
                        .longOpt("output")
                        .hasArg()
                        .argName("output_folder")
                        .required()
                        .desc("Path to the output folder where the simulation results are saved.")
                        .build());
        options.addOption(
                Option.builder("m")
                        .longOpt("model-folder")
                        .hasArg()
                        .argName("model_folder")
                        .required()
                        .desc("Path to the folder where the agent models are saved. "
                        		+ "Models are also loaded from this folder during simulation "
                        		+ "if the model files already exist.")
                        .build());
        options.addOption(
                Option.builder("s")
                        .longOpt("seed")
                        .hasArg()
                        .argName("seed")
                        .desc("A random seed for the simulation. "
                        		+ "Use if the simulation results should be reproducible.")
                        .build());
        options.addOption(
                Option.builder("T")
                        .longOpt("train")
                        .hasArg(false)
                        .desc("Turn on the training mode.")
                        .build());
        options.addOption(
                Option.builder("R")
                        .longOpt("random-steps")
                        .hasArg()
                        .argName("random_decision_steps")
                        .desc("Determine how many times at the beginning of a simulation each pricing agent decides the price randomly. "
                        		+ "Can be used to increase action exploration during training. Default value is 0.")
                        .build());
        options.addOption(
                Option.builder("r")
                        .longOpt("replay-size")
                        .hasArg()
                        .argName("replay_buffer_size")
                        .desc("The length of the experience replay. Default value is 2000.")
                        .build());
        options.addOption(
                Option.builder("b")
                        .longOpt("batch-size")
                        .hasArg()
                        .argName("batch_size")
                        .desc("The size of a training batch. Default value is 128.")
                        .build());
        options.addOption(
                Option.builder("d")
                        .longOpt("discount-factor")
                        .hasArg()
                        .argName("discount_factor")
                        .desc("The reward discount factor. The value should be in the range [0,1). "
                        		+ "Default value is 0.99.")
                        .build());
        options.addOption(
                Option.builder("a")
                        .longOpt("actorlr")
                        .hasArg()
                        .argName("actor_learning_rate")
                        .desc("The learning rate for actor network. Default value is 0.001.")
                        .build());
        options.addOption(
                Option.builder("c")
                        .longOpt("criticlr")
                        .hasArg()
                        .argName("critic_learning_rate")
                        .desc("The learning rate for critic network. Default value is 0.001.")
                        .build());
        options.addOption(
                Option.builder("t")
                        .longOpt("tau")
                        .hasArg()
                        .argName("tau")
                        .desc("The parameter used for updating the actor and critic target networks. "
                        		+ "Default value is 0.005.")
                        .build());
        options.addOption(
                Option.builder("u")
                        .longOpt("updates")
                        .hasArg()
                        .argName("model_updates")
                        .desc("Determine how many times models are updated during one update event. "
                        		+ "Default value is 1.")
                        .build());
        options.addOption(
                Option.builder("N")
                        .longOpt("noisesd")
                        .hasArg()
                        .argName("noise_standard_deviation")
                        .desc("The standard deviation for a zero-mean Gaussian noise process. "
                        		+ "Used in action exploration. Default value is 0.5. ")
                        .build());
        options.addOption(
                Option.builder("D")
                        .longOpt("noise-decay")
                        .hasArg()
                        .argName("noise_decay")
                        .desc("The rate at which noise is decayed during training. Default value is 1e-6.")
                        .build());
	}
	
	/**
	 * Saves the parsed option values into the {@link EisimSimulationParameters} class.
	 * 
	 * @param cmd CommandLine object which represents the list of arguments parsed based on 
	 * 			  an Options object.
	 */
	protected void setParameters(CommandLine cmd) {
		EisimSimulationParameters.settingFolder = cmd.getOptionValue("input");
		EisimSimulationParameters.outputFolder = cmd.getOptionValue("output");
		EisimSimulationParameters.modelFolder = cmd.getOptionValue("model-folder");
		
        if (cmd.hasOption("seed")) {
        	EisimSimulationParameters.useSeed = true;
        	EisimSimulationParameters.seed = Long.parseLong(cmd.getOptionValue("seed"));
        }
        
        EisimSimulationParameters.train = cmd.hasOption("train");
        
        if (cmd.hasOption("random-steps")) {
        	EisimSimulationParameters.randomDecisionSteps = Math.max(0, Integer.parseInt(cmd.getOptionValue("random-steps")));
        }
        
        if (cmd.hasOption("replay-size")) {
        	EisimSimulationParameters.replayBufferSize = Math.max(0, Integer.parseInt(cmd.getOptionValue("replay-size")));
        }
        
        if (cmd.hasOption("batch-size")) {
        	EisimSimulationParameters.batchSize = Math.max(0, Integer.parseInt(cmd.getOptionValue("batch-size")));
        }
        
        if (cmd.hasOption("discount-factor")) {
        	EisimSimulationParameters.discountFactor = Math.max(0f, Math.min(1f, Float.parseFloat(cmd.getOptionValue("discount-factor"))));
        }
        
        if (cmd.hasOption("actorlr")) {
        	EisimSimulationParameters.learningRateActor = Math.max(0f, Float.parseFloat(cmd.getOptionValue("actorlr")));
        }
        
        if (cmd.hasOption("criticlr")) {
        	EisimSimulationParameters.learningRateCritic = Math.max(0f, Float.parseFloat(cmd.getOptionValue("criticlr")));
        }
        
        if (cmd.hasOption("tau")) {
        	EisimSimulationParameters.tau = Math.max(0f, Float.parseFloat(cmd.getOptionValue("tau")));
        }
        
        if (cmd.hasOption("updates")) {
        	EisimSimulationParameters.modelUpdates = Math.max(1, Integer.parseInt(cmd.getOptionValue("updates")));
        }
        
        if (cmd.hasOption("noisesd")) {
        	EisimSimulationParameters.noiseSD = Math.max(0f, Float.parseFloat(cmd.getOptionValue("noisesd")));
        }
        
        if (cmd.hasOption("noise-decay")) {
        	EisimSimulationParameters.noiseDecay = Math.max(0f, Float.parseFloat(cmd.getOptionValue("noise-decay")));
        }
		
	}
}
