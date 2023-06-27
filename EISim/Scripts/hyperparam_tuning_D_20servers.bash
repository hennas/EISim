#!/usr/bin/env bash

YELLOW='\033[1;33m'
NC='\033[0m'

cd ..

actorRates=( 0.005 0.001 0.0005 )
criticRates=( 0.005 0.001 0.0005 )

alrCounter=0
for alr in "${actorRates[@]}"
do
    clrCounter=0
    for clr in "${criticRates[@]}"
    do
        echo -e "${YELLOW}TRAINING (actor lr: $alr critic lr: $clr)"
        rSteps=500
        for seed in {1..10}
        do
            echo -e "${NC}Training round with seed: $seed"
            mvn -q exec:java -Dexec.mainClass="com.github.hennas.eisim.Main" -Dexec.args="-i EISim_settings/settings_D_20servers/ -o EISim_output/D_20tuning/output_D_20servers_train_hparam_$alrCounter$clrCounter/ -m EISim_output/D_20tuning/models_D_20servers_hparam_$alrCounter$clrCounter/ -T -a $alr -c $clr -R $rSteps -s $seed"
            rSteps=1
        done
        echo -e "${YELLOW}EVALUATING (actor lr: $alr critic lr: $clr)"
        for seed in {11..15}
        do
            echo -e "${NC}Eval round with seed: $seed"
            mvn -q exec:java -Dexec.mainClass="com.github.hennas.eisim.Main" -Dexec.args="-i EISim_settings/settings_D_20servers/ -o EISim_output/D_20tuning/output_D_20servers_eval_hparam_$alrCounter$clrCounter/ -m EISim_output/D_20tuning/models_D_20servers_hparam_$alrCounter$clrCounter/ -s $seed"
        done
        ((clrCounter++))
    done
    ((alrCounter++))
done