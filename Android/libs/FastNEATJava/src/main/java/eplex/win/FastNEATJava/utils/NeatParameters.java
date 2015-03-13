package eplex.win.FastNEATJava.utils;

import java.util.ArrayList;

public class NeatParameters
{
    public double pDisjointExcessGenesRecombined;
    public double pInterspeciesMating;
    public double pOffspringSexual;
    public double pOffspringAsexual;
    public double pInitialPopulationInterconnections;
    public int populationSize;
    public boolean allowSelfConnections;
    public boolean multiobjective;
    public boolean noveltyFloat;
    public boolean noveltyFixed;
    public boolean noveltyHistogram;
    public boolean noveltySearch;
    public int speciesDropoffAge;
    public double archiveThreshold;
    public int tournamentSize;
    public double connectionWeightRange;
    public boolean disallowRecurrence;


    public int postAsexualMutations = 5;
    public int postSexualMutations = 5;
    public int seedMutateConnectionCount = 15;

    //----- High level mutation proportions
    public double pMutateConnectionWeights = DEFAULT_P_MUTATE_CONNECTION_WEIGHTS;
    public double pMutateAddNode = DEFAULT_P_MUTATE_ADD_NODE;
    public double pMutateAddModule = DEFAULT_P_MUTATE_ADD_MODULE;
    public double pMutateAddConnection = DEFAULT_P_MUTATE_ADD_CONNECTION;
    public double pMutateDeleteConnection = DEFAULT_P_MUTATE_DELETE_CONNECTION;
    public double pMutateDeleteSimpleNeuron = DEFAULT_P_MUTATE_DELETE_SIMPLENEURON;
    public double pMutateChangeActivations = DEFAULT_P_MUTATE_CHANGE_ACTIVATIONS;
    public double pNodeMutateActivationRate = DEFAULT_N_MUTATE_ACTIVATION;

    public ArrayList<ConnectionMutationParameterGroup> connectionMutationParameterGroupList;

    public double compatibilityThreshold = DEFAULT_COMPATIBILITY_THRESHOLD;
    public double compatibilityDisjointCoeff = DEFAULT_COMPATIBILITY_DISJOINT_COEFF;
    public double compatibilityExcessCoeff = DEFAULT_COMPATIBILITY_EXCESS_COEFF;
    public double compatibilityWeightDeltaCoeff = DEFAULT_COMPATIBILITY_WEIGHTDELTA_COEFF;

    public double elitismProportion = DEFAULT_ELITISM_PROPORTION;
    public double selectionProportion = DEFAULT_SELECTION_PROPORTION;

    public int targetSpeciesCountMin = DEFAULT_TARGET_SPECIES_COUNT_MIN;
    public int targetSpeciesCountMax = DEFAULT_TARGET_SPECIES_COUNT_MAX;

    public double pruningPhaseBeginComplexityThreshold = DEFAULT_PRUNINGPHASE_BEGIN_COMPLEXITY_THRESHOLD;
    public double pruningPhaseBeginFitnessStagnationThreshold = DEFAULT_PRUNINGPHASE_BEGIN_FITNESS_STAGNATION_THRESHOLD;
    public double pruningPhaseEndComplexityStagnationThreshold = DEFAULT_PRUNINGPHASE_END_COMPLEXITY_STAGNATION_THRESHOLD;

    public enum ConnectionPerturbationType {
        /// <summary>
        /// Reset weights.
        /// </summary>
        reset,

        /// <summary>
        /// Jiggle - even distribution
        /// </summary>
        jiggleEven,

        /// <summary>
        /// Jiggle - normal distribution
        /// </summary>
//            jiggleND : 2
    }

    public enum ConnectionSelectionType {
        /// <summary>
        /// Select a proportion of the weights in a genome.
        /// </summary>
        proportional,

        /// <summary>
        /// Select a fixed number of weights in a genome.
        /// </summary>
        fixedQuantity
    }

    static int DEFAULT_POPULATION_SIZE = 150;
    static double DEFAULT_P_INITIAL_POPULATION_INTERCONNECTIONS = 1.00;//DAVID 0.05F;

    static double DEFAULT_P_OFFSPRING_ASEXUAL = 0.5;
    static double DEFAULT_P_OFFSPRING_SEXUAL = 0.5;
    static double DEFAULT_P_INTERSPECIES_MATING = 0.01;

    static double DEFAULT_P_DISJOINGEXCESSGENES_RECOMBINED = 0.1;

    //----- High level mutation proportions
    static double DEFAULT_P_MUTATE_CONNECTION_WEIGHTS = 0.988;
    static double DEFAULT_P_MUTATE_ADD_NODE = 0.002;
    static double DEFAULT_P_MUTATE_ADD_MODULE = 0.0;
    static double DEFAULT_P_MUTATE_ADD_CONNECTION = 0.018;
    static double DEFAULT_P_MUTATE_CHANGE_ACTIVATIONS = 0.001;
    static double DEFAULT_P_MUTATE_DELETE_CONNECTION = 0.001;
    static double DEFAULT_P_MUTATE_DELETE_SIMPLENEURON = 0.00;
    static double DEFAULT_N_MUTATE_ACTIVATION = 0.01;

    //-----
    static double DEFAULT_COMPATIBILITY_THRESHOLD = 8;
    static double DEFAULT_COMPATIBILITY_DISJOINT_COEFF = 1.0;
    static double DEFAULT_COMPATIBILITY_EXCESS_COEFF = 1.0;
    static double DEFAULT_COMPATIBILITY_WEIGHTDELTA_COEFF = 0.05;

    static double DEFAULT_ELITISM_PROPORTION = 0.2;
    static double DEFAULT_SELECTION_PROPORTION = 0.2;

    static int DEFAULT_TARGET_SPECIES_COUNT_MIN = 6;
    static int DEFAULT_TARGET_SPECIES_COUNT_MAX = 10;

    static int DEFAULT_SPECIES_DROPOFF_AGE = 200;

    static int DEFAULT_PRUNINGPHASE_BEGIN_COMPLEXITY_THRESHOLD = 50;
    static int DEFAULT_PRUNINGPHASE_BEGIN_FITNESS_STAGNATION_THRESHOLD = 10;
    static int DEFAULT_PRUNINGPHASE_END_COMPLEXITY_STAGNATION_THRESHOLD = 15;

    static double DEFAULT_CONNECTION_WEIGHT_RANGE = 10.0;
//		public const double DEFAULT_CONNECTION_MUTATION_SIGMA = 0.015;

    static double DEFAULT_ACTIVATION_PROBABILITY = 1.0;


    public double[] activationProbabilities;


    public NeatParameters() {
        this.archiveThreshold = 3.00;
        this.tournamentSize = 4;
        this.noveltySearch = false;
        this.noveltyHistogram = false;
        this.noveltyFixed = false;
        this.noveltyFloat = false;
        this.multiobjective = false;

        this.allowSelfConnections = false;

        this.populationSize = DEFAULT_POPULATION_SIZE;
        this.pInitialPopulationInterconnections = DEFAULT_P_INITIAL_POPULATION_INTERCONNECTIONS;

        this.pOffspringAsexual = DEFAULT_P_OFFSPRING_ASEXUAL;
        this.pOffspringSexual = DEFAULT_P_OFFSPRING_SEXUAL;
        this.pInterspeciesMating = DEFAULT_P_INTERSPECIES_MATING;

        this.pDisjointExcessGenesRecombined = DEFAULT_P_DISJOINGEXCESSGENES_RECOMBINED;

        //----- High level mutation proportions
        this.pMutateConnectionWeights = DEFAULT_P_MUTATE_CONNECTION_WEIGHTS;
        this.pMutateAddNode = DEFAULT_P_MUTATE_ADD_NODE;
        this.pMutateAddModule = DEFAULT_P_MUTATE_ADD_MODULE;
        this.pMutateAddConnection = DEFAULT_P_MUTATE_ADD_CONNECTION;
        this.pMutateDeleteConnection = DEFAULT_P_MUTATE_DELETE_CONNECTION;
        this.pMutateDeleteSimpleNeuron = DEFAULT_P_MUTATE_DELETE_SIMPLENEURON;
        this.pMutateChangeActivations = DEFAULT_P_MUTATE_CHANGE_ACTIVATIONS;
        this.pNodeMutateActivationRate = DEFAULT_N_MUTATE_ACTIVATION;

        //----- Build a default ConnectionMutationParameterGroupList.
        this.connectionMutationParameterGroupList = new ArrayList<ConnectionMutationParameterGroup>();

        this.connectionMutationParameterGroupList.add(new ConnectionMutationParameterGroup(0.125, NeatParameters.ConnectionPerturbationType.jiggleEven,
                NeatParameters.ConnectionSelectionType.proportional, 0.5, 0, 0.05, 0.0));

        this.connectionMutationParameterGroupList.add(new ConnectionMutationParameterGroup(0.5, NeatParameters.ConnectionPerturbationType.jiggleEven,
                NeatParameters.ConnectionSelectionType.proportional, 0.1, 0, 0.05, 0.0));

        this.connectionMutationParameterGroupList.add(new ConnectionMutationParameterGroup(0.125, NeatParameters.ConnectionPerturbationType.jiggleEven,
                NeatParameters.ConnectionSelectionType.fixedQuantity, 0.0, 1, 0.05, 0.0));

        this.connectionMutationParameterGroupList.add(new ConnectionMutationParameterGroup(0.125, NeatParameters.ConnectionPerturbationType.reset,
                NeatParameters.ConnectionSelectionType.proportional, 0.1, 0, 0.0, 0.0));

        this.connectionMutationParameterGroupList.add(new ConnectionMutationParameterGroup(0.125, NeatParameters.ConnectionPerturbationType.reset,
                NeatParameters.ConnectionSelectionType.fixedQuantity, 0.0, 1, 0.0, 0.0));

        //-----
        this.compatibilityThreshold = DEFAULT_COMPATIBILITY_THRESHOLD;
        this.compatibilityDisjointCoeff = DEFAULT_COMPATIBILITY_DISJOINT_COEFF;
        this.compatibilityExcessCoeff = DEFAULT_COMPATIBILITY_EXCESS_COEFF;
        this.compatibilityWeightDeltaCoeff = DEFAULT_COMPATIBILITY_WEIGHTDELTA_COEFF;

        this.elitismProportion = DEFAULT_ELITISM_PROPORTION;
        this.selectionProportion = DEFAULT_SELECTION_PROPORTION;

        this.targetSpeciesCountMin = DEFAULT_TARGET_SPECIES_COUNT_MIN;
        this.targetSpeciesCountMax = DEFAULT_TARGET_SPECIES_COUNT_MAX;

        this.pruningPhaseBeginComplexityThreshold = DEFAULT_PRUNINGPHASE_BEGIN_COMPLEXITY_THRESHOLD;
        this.pruningPhaseBeginFitnessStagnationThreshold = DEFAULT_PRUNINGPHASE_BEGIN_FITNESS_STAGNATION_THRESHOLD;
        this.pruningPhaseEndComplexityStagnationThreshold = DEFAULT_PRUNINGPHASE_END_COMPLEXITY_STAGNATION_THRESHOLD;

        this.speciesDropoffAge = DEFAULT_SPECIES_DROPOFF_AGE;

        this.connectionWeightRange = DEFAULT_CONNECTION_WEIGHT_RANGE;

        //DAVID
        this.activationProbabilities = new double[4];
        this.activationProbabilities[0] = (DEFAULT_ACTIVATION_PROBABILITY);
        this.activationProbabilities[1] = 0;
        this.activationProbabilities[2] = 0;
        this.activationProbabilities[3] = 0;
    }

    public static NeatParameters Copy(NeatParameters copyFrom) {
        NeatParameters self = new NeatParameters();

        //paul - joel originally
        self.noveltySearch = copyFrom.noveltySearch;
        self.noveltyHistogram = copyFrom.noveltyHistogram;
        self.noveltyFixed = copyFrom.noveltyFixed;
        self.noveltyFloat = copyFrom.noveltyFloat;

        self.postAsexualMutations = copyFrom.postAsexualMutations;
        self.postSexualMutations = copyFrom.postSexualMutations;
        self.seedMutateConnectionCount = copyFrom.seedMutateConnectionCount;

        self.allowSelfConnections = copyFrom.allowSelfConnections;

        self.populationSize = copyFrom.populationSize;

        self.pOffspringAsexual = copyFrom.pOffspringAsexual;
        self.pOffspringSexual = copyFrom.pOffspringSexual;
        self.pInterspeciesMating = copyFrom.pInterspeciesMating;

        self.pDisjointExcessGenesRecombined = copyFrom.pDisjointExcessGenesRecombined;

        self.pMutateConnectionWeights = copyFrom.pMutateConnectionWeights;
        self.pMutateAddNode = copyFrom.pMutateAddNode;
        self.pMutateAddModule = copyFrom.pMutateAddModule;
        self.pMutateAddConnection = copyFrom.pMutateAddConnection;
        self.pMutateDeleteConnection = copyFrom.pMutateDeleteConnection;
        self.pMutateDeleteSimpleNeuron = copyFrom.pMutateDeleteSimpleNeuron;

        // Copy the list.
        self.connectionMutationParameterGroupList = new ArrayList<ConnectionMutationParameterGroup>();
        self.connectionMutationParameterGroupList.addAll(copyFrom.connectionMutationParameterGroupList);

        self.compatibilityThreshold = copyFrom.compatibilityThreshold;
        self.compatibilityDisjointCoeff = copyFrom.compatibilityDisjointCoeff;
        self.compatibilityExcessCoeff = copyFrom.compatibilityExcessCoeff;
        self.compatibilityWeightDeltaCoeff = copyFrom.compatibilityWeightDeltaCoeff;

        self.elitismProportion = copyFrom.elitismProportion;
        self.selectionProportion = copyFrom.selectionProportion;

        self.targetSpeciesCountMin = copyFrom.targetSpeciesCountMin;
        self.targetSpeciesCountMax = copyFrom.targetSpeciesCountMax;

        self.pruningPhaseBeginComplexityThreshold = copyFrom.pruningPhaseBeginComplexityThreshold;
        self.pruningPhaseBeginFitnessStagnationThreshold = copyFrom.pruningPhaseBeginFitnessStagnationThreshold;
        self.pruningPhaseEndComplexityStagnationThreshold = copyFrom.pruningPhaseEndComplexityStagnationThreshold;

        self.speciesDropoffAge = copyFrom.speciesDropoffAge;

        self.connectionWeightRange = copyFrom.connectionWeightRange;

        return self;
    }
}


