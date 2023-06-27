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
