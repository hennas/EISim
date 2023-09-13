import sys
import numpy as np
import pandas as pd
import itertools
import shutil
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter
from os import listdir, makedirs, rmdir
from os.path import join, exists
from datetime import datetime, timedelta

def combine_split_episodes(output_dir):
    '''
    Checks whether the output for some simulation run (one episode) in the provided output directory has been saved into two
    separate folders. For every such incident, combines the results into the same folder and removes the excess folder.
    
    In EISim, simulation output is saved into a folder that is named based on the start time of the simulation. The time
    is recorded up to the precision of seconds. When running several simulation scenarios in parallel, each thread records 
    their own start time. In some rare occasions, the second may change between the start time of the first thread and the 
    start time of the last thread, resulting in two output folders for the same simulation run. This method recognizes such
    situations and corrects them.
    
    Args:
        output_dir (str): A path to a folder that contains a number of simulation runs (episodes).
    
    Returns:
        Nothing.
    '''
    episode_folders = sorted(listdir(output_dir))
    date_format = '%Y-%m-%d_%H-%M-%S'
    episode_start_times = pd.Series([datetime.strptime(efolder, date_format) for efolder in episode_folders])
    split_episodes = episode_start_times.loc[episode_start_times.diff() < timedelta(seconds=2)]
    split_episodes_indices = sorted(split_episodes.index.tolist(), reverse=True)
    if len(split_episodes_indices) > 0:
        for i in split_episodes_indices:
            efolder_src = episode_folders[i]
            efolder_dst = episode_folders[i-1]
            path_to_src = join(output_dir, efolder_src)
            path_to_dst = join(output_dir, efolder_dst)
            for file_system_object in listdir(path_to_src):
                path_to_object = join(path_to_src, file_system_object)
                shutil.move(path_to_object, path_to_dst)
            rmdir(path_to_src)

def plot_training_progress(output_dir, window_size=5, fpath=None):
    '''
    Plots the training progress for all simulation scenarios in the provided output.
    
    Three figures are plotted for each simulation scenario. The first figure plots the total cumulative return of the 
    whole edge platform against the training episodes. The first figure also plots the simple moving average of the 
    total return. The second figure plots the cumulative return of each agent against the training episodes. Finally, 
    the third figure plots the average price of each agent against the training episodes.
    
    The figures are saved in PDF format with the file name '<scenario_name>_training_<n_of_episodes>episodes.pdf'. 
    It is possible to specify a folder for saving the figures in the argument 'fpath'. If the provided path has folders 
    that do not exist, they will be created.
    
    Args:
        output_dir (str): A path to a folder that contains training episodes for a number of simulation scenarios.
        window_size (int): The size of the window for calculating the simple moving average. Default is 5.
        fpath (str): A path to a folder where the figures will be saved. By default, figures are saved to the current 
                     working directory.
    
    Returns:
        Nothing.
    '''
    results_per_scenario, agent_labels = parse_results_from_training_episodes(output_dir)
    
    if results_per_scenario == None or agent_labels == None:
        raise SystemExit("Couldn't parse results from the provided output folder.")
    
    for scenario, result_dict in results_per_scenario.items():
        cum_returns = result_dict['cumulativeReturn']
        #cum_returns_MA = [calculate_SMA(cum_returns[:,ai], window_size) for ai in range(len(agent_labels))]
        
        total_returns = cum_returns.sum(axis=1) # Summed over agents
        total_returns_MA = calculate_SMA(total_returns, window_size)
        
        avg_prices = result_dict['avgPrice']
        
        n_of_episodes = len(total_returns)
        episodes = [i for i in range(1, n_of_episodes+1)]
        fig, (ax1, ax2, ax3) = plt.subplots(3, 1, figsize=(12, 12))
        fig.suptitle(f'Training progress for scenario: {scenario}\n', fontsize=14, fontweight='bold')
        
        ax1.plot(episodes, total_returns, color='steelblue')
        ax1.plot(episodes, total_returns_MA, color='red', linewidth=3)
        ax1.set_title('Total cumulative return per training episode for the whole edge platform')
        ax1.set_xlabel('Training episode')
        ax1.grid(True, 'major', 'y', linewidth=0.5, c='k', alpha=0.3)
        ax1.get_yaxis().set_major_formatter(FuncFormatter(format_y_labels))
        
        for ai in range(len(agent_labels)):
            ax2.plot(episodes, cum_returns[:,ai], label=agent_labels[ai], linewidth=1)
            ax3.plot(episodes, avg_prices[:,ai], label=agent_labels[ai], linewidth=1)
            
        ax2.set_title('Cumulative return per training episode for each agent')
        ax2.set_xlabel('Training episode')
        ax2.grid(True, 'major', 'y', linewidth=0.5, c='k', alpha=0.3)
        ax2.get_yaxis().set_major_formatter(FuncFormatter(format_y_labels))
            
        ax3.set_title('Average price per training episode for each agent')
        ax3.set_xlabel('Training episode')
        ax3.grid(True, 'major', 'y', linewidth=0.5, c='k', alpha=0.3)
        ax3.set_ylim(0, 1)
        
        # Only adding the legend if there are 20 agents at maximum
        if len(agent_labels) < 21:
            # Gets the tight bounding box of the x axis, including the axis and its decorators (x label, tick labels).
            x_bbox2 = ax2.get_xaxis().get_tightbbox(fig.canvas.get_renderer()) 
            # Gives the bottom left corner of the box in axis coordinates
            _, ymin2 = ax2.transAxes.inverted().transform(x_bbox2.min) 
            # The legend is placed below the figure, ymin is used to avoid placing on top of x tick labels
            ax2.legend(loc='upper center', bbox_to_anchor=(0.5, ymin2), ncol=10)
            
            x_bbox3 = ax3.get_xaxis().get_tightbbox(fig.canvas.get_renderer())
            _, ymin3 = ax3.transAxes.inverted().transform(x_bbox3.min)
            ax3.legend(loc='upper center', bbox_to_anchor=(0.5, ymin3), ncol=10)
        
        fig.tight_layout()
        
        fname = scenario + "_training_" + str(n_of_episodes) + 'episodes.pdf'
        if fpath != None:
            if not exists(fpath):
                makedirs(fpath)
            fname = join(fpath, fname)
            
        plt.savefig(fname, format='pdf', bbox_inches='tight')
        
def parse_results_from_training_episodes(output_dir):
    '''
    Parses cumulative returns and average prices for each agent, episode and scenario in the provided output. The results
    are returned as a dictionary, where each key is a scenario name. Each value is another dictionary with two keys. The 
    first key is 'cumulativeReturn', the value of which is a 2D numpy array of shape (n_of_episodes, n_of_agents). This array
    holds the cumulative returns for each agent and training episode. The second key is 'avgPrice', the value of which is 
    also a 2D numpy array of shape (n_of_episodes, n_of_agents), holding the average price for each agent and training episode.
    
    The method also returns a list of agent names, which maps each column in the 2D numpy arrays to the agent's name. 
    For example, the agent name at index 0 on the list gives the name of the agent to which the values in column [:,0] of
    the numpy arrays belong.
    
    It is assumed that the training is done under the same simulation settings, hence it is checked that the number of 
    scenarios in all episodes and the number of agents in all scenarios is the same.
    
    Args:
        output_dir (str): A path to a folder that contains training episodes.
    
    Returns:
        dict or NoneType: A dictionary where each key is a simulation scenario name and the corresponding value is another
                          dictionary with two keys, 'cumulativeReturn' and 'avgPrice', both holding 2D numpy arrays with the 
                          shape of (n_of_episodes, n_of_agents). None is returned if it is detected that the number of 
                          scenarios in all episodes or the number of agents in all scenarios is not the same.
        list or NoneType: A list of agent names. None is returned if it is detected that the number of scenarios in all 
                          episodes or the number of agents in all scenarios is not the same.
    '''
    episode_folders = sorted(listdir(output_dir))
    paths_to_episode_folders = [join(output_dir, efolder) for efolder in episode_folders]
    
    n_of_episodes = len(episode_folders)
    n_of_scenarios = 0
    n_of_agents = 0
    
    results_per_scenario = None
    agent_names = None
    
    for ei, path_to_efolder in enumerate(paths_to_episode_folders):
        log_folders = list(filter(lambda file_system_object: 'Pricelogs' in file_system_object, listdir(path_to_efolder)))
        scenario_names = list(map(lambda log_folder: '_'.join(log_folder.split('_')[2:]), log_folders))
        
        if n_of_scenarios == 0:
            n_of_scenarios = len(log_folders)
        elif len(log_folders) != n_of_scenarios:
            print('The number of scenarios in', path_to_efolder, 
                  'differs from the number of scenarios in other episode folders.')
            return None, None
        
        paths_to_log_folders = [join(path_to_efolder, lfolder) for lfolder in log_folders]
        
        for si, path_to_lfolder in enumerate(paths_to_log_folders):
            
            scenario = scenario_names[si]
            log_files = listdir(path_to_lfolder)
            
            if n_of_agents == 0:
                n_of_agents = len(log_files)
                agent_names = sorted(list(map(lambda log_file: log_file.split('_')[0], log_files)), 
                                     key=get_agent_number_from_name
                                    )
                results_per_scenario = {scenario_name: {'cumulativeReturn': np.zeros((n_of_episodes, n_of_agents)), 
                                                        'avgPrice': np.zeros((n_of_episodes, n_of_agents))
                                                       }
                                        for scenario_name in scenario_names}
            elif len(log_files) != n_of_agents:
                print('The number of agent price log files in', path_to_lfolder, 
                      'differs from the number of agent price log files in other price log folders.')
                return None, None
            
            paths_to_log_files = [join(path_to_lfolder, log_file) for log_file in log_files]
            
            for li, path_to_lfile in enumerate(paths_to_log_files):
                
                agent_name = log_files[li].split('_')[0]
                ai = agent_names.index(agent_name)
                
                agent_data = pd.read_csv(path_to_lfile)
                
                cumulative_return_for_episode = agent_data.CumulativeProfit.iat[-1]
                results_per_scenario[scenario]['cumulativeReturn'][ei][ai] = cumulative_return_for_episode
                
                avg_price_for_episode = agent_data.Price.mean()
                results_per_scenario[scenario]['avgPrice'][ei][ai] = avg_price_for_episode
                
    return results_per_scenario, agent_names
                
def calculate_SMA(data, window_size):
    '''
    Calculates the simple moving average of a 1D array.
    
    The returned array has the same length as the original array, meaning that the step size and the minimum 
    number of observations in window required to have a value are both 1.
    
    Args:
        data (numpy.ndarray or list): 1D array of data.
        window_size (int): The size of the moving window.
        
    Returns:
        list: Simple moving average over data.
    '''
    data_MA = []
    for i in range(1, len(data)+1):
        data_MA.append(np.sum(data[:i]) / i if i < window_size else 
                       np.sum(data[(i-window_size):i]) / window_size)
    return data_MA

def get_agent_number_from_name(name):
    '''
    Gets the the number of an agent from its name. It is assumed that the name of an agent ends with a number that can 
    be anything between 0 and 999. E.g., for name 'dc54' the integer 54 is returned.
    
    Args:
        name (str): The name of the edge server agent.
        
    Returns:
        int: The number of the agent.
    '''
    try:
        return int(name[-3:])
    except ValueError:
        try:
            return int(name[-2:])
        except ValueError:
            return int(name[-1])
        
def format_y_labels(value, position):
    '''
    Formats the y-axis labels for the Matplotlib figures.
    
    Args:
        value (numpy.float64): Y label.
        position (int): The position of the label.
    
    Returns:
        str: Formatted y label.
    '''
    if value == 0:
        return str(int(value))
    return f'{round(value * 1e-6, 3)}M'

if __name__ == '__main__':
    if len(sys.argv) == 2:
        output_dir = sys.argv[1]
        save_dir = None
    elif len(sys.argv) == 3:
        output_dir = sys.argv[1]
        save_dir = sys.argv[2]
    else:
        raise SystemExit(f"Usage: {sys.argv[0]} <output_dir> [save_dir]\n\n" +
        "output_dir is mandatory, it provides a path to a folder that contains training episodes.\n" + 
        "save_dir can be used to provide a path to a folder where the figures will be saved."
        )
        
    combine_split_episodes(output_dir)
    plot_training_progress(output_dir, 10, save_dir)


    