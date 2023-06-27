# Environment setup

The files here can be used for the automatic generation of 

1. Metropolitan Area Network (including AP placement, topology creation and edge server placement) -> see Topology_creation_and_edge_server_placement.ipynb
1. Edge server clusters -> see Server_clustering.ipynb
1. edge_datacenters.xml setting files -> see Generate_XML_files.ipynb

These files are meant to be executed in the same order as specified in the list above:

* The Topology_creation_and_edge_server_placement.ipynb file creates .pickle files for access point locations, links between the access points, and edge server locations.
* These are inputs for the Server_clustering.ipynb file that creates .json files containing the cluster information for the edge servers.
* All the aforementioned files are inputs for the Generate_XML_files.ipynb file, which generates the edge_datacenters.xml setting files directly into the 
[EISim_settings folder](../EISim/EISim_settings).

#### In the Topology_creation_and_edge_server_placement.ipynb file:

* APs are placed on integer coordinates in a square area. Placement is based on covering the square area with a hexagon grid.
* Links between APs are created using the *Tunable Weight Spanning Tree* method presented in the article *[Generation of Synthetic Spatially Embedded Power Grid Networks](https://arxiv.org/abs/1508.04447)*
    * More links can be added between the APs with a method that is adapted from the *Reinforcement* method presented in the same article
* Edge servers are co-located with the APs. The servers are placed randomly so that the probability of choosing an AP node to host an edge server is proportional to the degree of the node.

#### In the Server_clustering.ipynb file:

* Edge servers are clustered using the *[Agglomerative clustering](https://scikit-learn.org/stable/modules/generated/sklearn.cluster.AgglomerativeClustering.html)* method
* Inside each cluster, the node with the highest betweenness centrality is chosen as the cluster head

#### In the Generate_XML_files.ipynb file:

* edge_datacenters.xml setting files used by the EISim are created based on the AP locations, links between the APs, edge server locations, cluster information, and edge server and AP specifications