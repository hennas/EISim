#!/usr/bin/env bash

YELLOW='\033[1;33m'
NC='\033[0m'

cd ..

# Evaluating for 5 rounds
for seed in {1001..1005}
do 
    echo -e "${YELLOW}EVALUATION round with seed: $seed${NC}"
    mvn -q exec:java -Dexec.mainClass="com.github.hennas.eisim.Main" -Dexec.args="-i EISim_settings/settings_C_100servers/ -o EISim_output/C_100results/output_C_100servers_evaluation/ -m EISim_output/C_100results/models_C_100servers/ -s $seed"
done