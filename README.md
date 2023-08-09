# Edge Intelligence Simulator (EISim)

## Purpose

EISim is developed towards supporting easier testing and evaluation of intelligent orchestration methods in the device-edge-cloud continuum. 
**EISim is built on top of an existing fog simulator ***[PureEdgeSim](https://github.com/CharafeddineMechalikh/PureEdgeSim)*** (version 5.1.0).**

EISim is particularly intended to aid research in the context of the vision presented in the article 
*[Autonomy and Intelligence in the Computing Continuum: Challenges, Enablers, and Future Directions for Orchestration](https://arxiv.org/abs/2205.01423)*. 
In short, this vision models the computing continuum as a hierarchical network of nearly autonomous agents that conduct actions related to orchestration functions. 
The core statement of the vision is that the development of intelligent solutions for distributed, multi-domain and multi-tenant edge orchestration is a necessity 
for bringing about a coherent and autonomous computing continuum that is able to manage its limited resources in a globally optimized manner.

The realization of the aforementioned vision brings forth multiple open research questions. Some of the most important questions concern the optimal level of autonomy 
in the orchestration solutions and the adaptation and application of AI methods in the complex computing continuum environment. 
Any proposed solution requires ways to test and evaluate the method and compare it with other potential solutions. 
For this, simulation provides a controllable and cost-effective testing environment. However, there does not currently exist openly available simulation platforms 
that would particularly be directed towards supporting the research on intelligent edge orchestration methods. EISim aims to fill this need.

> Note that intelligent orchestration methods particularly refer to (deep) reinforcement learning based solutions, because reinforcement based learning is seen as one 
key ingredient for intelligent computing continuum orchestration in the vision.

## Features

In its current form, EISim supports simulating scenarios related to task offloading and resource pricing. 
The focus of EISim is particularly on evaluating and comparing the performance of orchestration solutions against different orchestration control topologies.
This is because the control topology of the orchestration solution closely relates to the level of autonomy in the system, and the optimal level of autonomy is a 
central research question in the aforementioned vision.

EISim inherits many features from PureEdgeSim, such as efficiency, scalability, extensibility, and wide applicability. 
Like PureEdgeSim, EISim also allows the user to simulate a wide variety of scenarios and deployments in the device-edge-cloud computing continuum. 
EISim also introduces some improvements to the original PureEdgeSim, as well as many new features that are tailored towards facilitating the research on intelligent 
orchestration methods.

Improvements to PureEdgeSim:

* Allows creating edge nodes that only function as access points
    * These are specified alongside edge datacenters in the edge_datacenters.xml setting file
    * Each datacenter element in the edge_datacenters.xml file must have a name attribute that contains 'dc' if the node is an edge datacenter, and 'ap' if the node
    is an access point
* Reproducibility of the simulation results (seeding random number generators)
* Improved extensibility; users can also plug in their own implementation of the computing node generator, if needed
* More versatile application model
    * Tasks are generated according to a Poisson process with a specified rate
    * Task's request and container sizes can be sampled uniformly from a specified range
    * Task's result size is defined as a ratio of the request size, and the ratio can also be drawn uniformly from a specified range
    * Task length (in Million Instructions; MIs) is drawn from an exponential distribution with a specified expected value

Exclusive features:

* Three default orchestration algorithm implementations that correspond to three main orchestration control topologies, namely *decentralized*, *hybrid* and *centralized*
* Offering cluster information as a part of edge datacenter specification (in the edge_datacenters.xml setting file)
    * For each edge datacenter / server, cluster information consists of a non-negative integer that specifies the cluster to which the server belongs, and a boolean
    value that indicates whether the server is the head of the cluster or not
    * When using the decentralized orchestration algorithm, it is assumed that each edge server forms a cluster on its own
    * When using the centralized orchestration algorithm, it is assumed that all edge servers belong to the same cluster with one assigned cluster head. 
    The cluster head functions as a central orchestrator
    * The hybrid orchestration algorithm is intended for any type of grouping that resides between the decentralized and centralized extremes
* Tools for automatic generation of 1) Metropolitan Area Network (including AP placement, topology creation and edge server placement), 2) edge server clusters, 
and 3) edge_datacenters.xml setting files (see [Environment setup](Environment_setup/))
* A Deep Deterministic Policy Gradient (DDPG) based default implementation for a pricing agent
    * In each control topology, cluster heads function as pricing agents that decide a price for task execution on the resources in their cluster.
        * For these agents, the system time is divided into slots and a new price decision is made at the beginning of a slot. Default slot length is 5 seconds. 
    * Users can easily plug in their own implementation for the pricing agent
* Scripts that can be used to run simulations for hyperparameter tuning, training and evaluation (See [Scripts](EISim/Scripts/))
    * Simulation runs can be considered as pseudo-episodes over which the pricing agents can be trained and evaluated
    * Agents save their state at the end of a simulation run (episode), and load the state at the beginning of a new one
* Tools for result plotting
    * [Plots](Result_plotting/hyperparam_plots.ipynb) for comparing the performance of different hyperparameter combinations
        * Metrics: The cumulative return (profit) of the whole system and the cumulative return of each pricing agent in the system
    * [Plots](EISim/plot_training_progress.py) for observing the training progress of the agents
        * The cumulative return of the whole system and the cumulative return of each pricing agent are plotted against training episodes
    * [Plots](Result_plotting/final_evaluation_plots.ipynb) for comparing the final performance of the system after running evaluation episodes with the final trained models
        * The main comparison axis is the three different control topologies
        * Several metrics are plotted

The workflows of the three default orchestration algorithm implementations can be summarized as follows.

> Decentralized control topology regards every user and server in the system as a self-interested, autonomous agent. 
Each user agent aims to maximize its own utility which takes into account task execution latency, energy consumption and monetary cost of the execution on the edge platform. 
Each edge server agent aims to optimize its resource usage and maximize its profit, with the high level objective of maximizing the profit of the edge service provider. 
In this control topology, every server decides about the price for task execution on its resources, and users decide whether to offload their tasks and to which edge server. 

> In the hybrid control topology, edge servers are clustered into groups with assigned cluster heads. A cluster head agent decides the price for task execution in the cluster, 
each user agent decides to which cluster it offloads the tasks. The cluster head agent also decides how the incoming tasks are allocated on the cluster nodes.

> In the centralized control topology, there is one central edge server agent that decides the price for the task execution on the platform, as well as the allocation of 
all user tasks. User agents only decide whether they offload or not.

The decentralized control topology has the most autonomous agents, as both servers and users are able to decide about the pricing and offloading targets independently. 
The hybrid control topology introduces a level of control where cluster heads decide independently for the price and task allocation inside their clusters.
This reduces the autonomy of other edge servers inside the clusters, as well as that of the users as they can no longer decide about the exact location for the execution 
of their tasks, although they still have partial autonomy with regard to deciding the destination cluster. The centralized control topology has the least amount of autonomy, 
as all edge servers hand over the decision-making power to the central orchestrator, and users lose almost completely their say in where their tasks are executed.

The default implementation of the EISim is focused on edge based processing, that is, tasks are either processed locally by the edge devices that generate them, 
or offloaded to the edge servers. However, due to the high extensibility of the EISim, it is easy to change these implementations to also account, e.g., 
offloading to the cloud datacenters.

## Usage

### Requirements

* EISim is built on Java SE Platform (target release: 17)
    * Maven is needed as the build automation tool for the project (used version: Apache Maven 3.9.1)
* Python is needed for environment setup and result plotting (required version >= 3.7)
    * Jupyter notebooks are used as an interactive environment for executing the Python codes
    * [requirements.txt](requirements.txt) file lists all the required Python modules and their versions. 
    These can be installed with `pip install -r requirements.txt`

### Running the simulations

Simulations can be run either through the command line (recommended) or through a Java development environment, such as Eclipse IDE.

#### From command line

After cloning the project, navigate to the EISim folder that contains the pom.xml file and run:

```
mvn clean package
```

If you want to use EISim as a dependency in other projects locally, run `mvn clean install` instead. This also installs the package into the local repository.

To see the available options for running the simulation, use:

```
mvn -q exec:java -Dexec.mainClass="com.github.hennas.eisim.Main" -Dexec.args="--help"
```

For example, to run a centralized simulation with 20 servers in a training mode, using a seed of 10, use:

```
mvn -q exec:java -Dexec.mainClass="com.github.hennas.eisim.Main" -Dexec.args="-i EISim_settings/settings_C_20servers/ -o EISim_output/output_C_20servers/ -m EISim_output/models_C_20servers/ -T -s 10"
```
#### From Eclipse IDE

1. Import the project to the IDE by selecting the location where it was cloned
    * File -> New -> Java Project, and choose the EISim folder that contains the pom.xml file as the location
1. Convert the project to a Maven project
    * Right click the project -> Configure -> Convert to Maven Project
1. Run the project from the Main class
    * This will directly print the available options and their explanations to the console
    * For defining the command line arguments, go to 
    Run -> Run Configurations... -> Choose Main under Java Application -> Go to Arguments tab -> Write the command line arguments into the Program arguments box
    * For example, to run the centralized simulation with 20 servers in a training mode, using a seed of 10, write into the box: 
    -i EISim_settings/settings_C_20servers/ -o EISim_output/output_C_20servers/ -m EISim_output/models_C_20servers/ -T -s 10
