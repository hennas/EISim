# Scripts

The scripts in this folder can be used to run simulations for hyperparameter tuning, training and evaluation of the pricing agents.

All scripts can be run from this folder after the EISim project has been built.

The default workflow of the scripts is as follows.

#### hyperparam_tuning_* files

* Does a gridsearch over actor and critic learning rates (9 combinations)
* For each combination, the models are trained for 10 rounds with different seeds, after which the models are evaluated for 5 rounds with different seeds

#### training_* files

* Trains the models for 100 rounds with different seeds
* Plots the training progress every 20th round using the [plot_training_progress.py file](../plot_training_progress.py)

#### evaluation_* files

* Evaluates the trained models for 5 rounds with different seeds

These scripts provide a starting point for running hyperparameter tuning, training and evaluation rounds for the pricing agents. 
Naturally these can be modified to fit the needs of the user. 
However, it is good to note that the logic used to save the results for different hyperparameter combinations in the hyperparam_tuning_* files 
should be preserved if the intention is to use [the provided file](../../Result_plotting/hyperparam_plots.ipynb) for plotting the results. 
It is assumed that the path to the output folder for each hyperparameter combination is exactly the same up to some point, after which a string of 
numbers that specifies the used hyperparameter combination has been added to the folder name. The length of the number string is equal to the number 
of hyperparameters, and the number itself gives the index of the used hyperparameter value in the corresponding value list.

> For example, if there are two hyperparameters with two possible values, all possible extensions to the partial output folder path are '00', '01', 
'10', and '11'. When the partial path to the output folder is extended with '01', the resulting folder contains the evaluation output when the first 
hyperparameter had been set to use the first value in the corresponding value list, and the second hyperparameter had been set to use the second value 
in the corresponding value list.