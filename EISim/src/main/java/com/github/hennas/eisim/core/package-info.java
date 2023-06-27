/**
 * Contains classes from PureEdgeSim version 5.1.0. As EISim is built on PureEdgeSim, these classes
 * form the core of the EISim.
 * <p>
 * The contents in this package are not exact copy of the original PureEdgeSim 5.1.0, some 
 * modifications have been done. Mainly these are small fixes, but the four most major modifications
 * are the following:
 * <ol>
 * <li>ComputingNodesGenerator has been turned into customizable class. (Users can plug in their own 
 * generator implementations.)</li>
 * <li>File parsers and application model have been modified to correspond to the setting files of the 
 * IESim.</li>
 * <li>Original default orchestrator and default task generator have been removed as these are completely
 * replaced by new implementations.</li>
 * <li>The use of SecureRandom has been replaced with Random.</li>
 * </ol>
 */
package com.github.hennas.eisim.core;