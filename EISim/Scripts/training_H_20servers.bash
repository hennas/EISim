#!/usr/bin/env bash

YELLOW='\033[1;33m'
NC='\033[0m'

cd ..

# Random exploration steps
rSteps=500
# Training for 100 rounds
for seed in {101..200}
do 
    echo -e "${YELLOW}TRAINING round with seed: $seed${NC}"
    mvn -q exec:java -Dexec.mainClass="com.github.hennas.eisim.Main" -Dexec.args="-i EISim_settings/settings_H_20servers/ -o EISim_output/H_20results/output_H_20servers_training/ -m EISim_output/H_20results/models_H_20servers/ -T -a 0.0005 -c 0.0005 -R $rSteps -s $seed"
    if [ $seed -eq 104 ]; then
        # Exploring randomly during the first four training rounds, after which only the first decision is random
        rSteps=1
    fi
    if [ $(($seed % 20)) -eq 0 ]; then
        # Plot training progress every twentieth round
        python3 plot_training_progress.py "EISim_output/H_20results/output_H_20servers_training" "EISim_output/H_20results/H_20servers_training_plots"   
    fi
    if [ $? -ne 0 ]; then
        # If something goes wrong with the training progress plotting, stop training
        break
    fi
done