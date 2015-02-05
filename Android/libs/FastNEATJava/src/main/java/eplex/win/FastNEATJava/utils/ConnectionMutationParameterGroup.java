package eplex.win.FastNEATJava.utils;

/**
 * Created by paul on 8/16/14.
 */
public class ConnectionMutationParameterGroup {

    public double ActivationProportion;
    public NeatParameters.ConnectionPerturbationType PerturbationType;
    public NeatParameters.ConnectionSelectionType SelectionType;
    public double Proportion;
    public int Quantity;
    public double PerturbationFactor;
    public double Sigma;

    public ConnectionMutationParameterGroup(
            double activationProportion,
            NeatParameters.ConnectionPerturbationType perturbationType,
            NeatParameters.ConnectionSelectionType selectionType,
            double proportion,
            int quantity,
            double perturbationFactor,
            double sigma) {


        /// <summary>
        /// This group's activation proportion - relative to the totalled
        /// ActivationProportion for all groups.
        /// </summary>
        this.ActivationProportion = activationProportion;

        /// <summary>
        /// The type of mutation that this group represents.
        /// </summary>
        this.PerturbationType = perturbationType;

        /// <summary>
        /// The type of connection selection that this group represents.
        /// </summary>
        this.SelectionType = selectionType;

        /// <summary>
        /// Specifies the proportion for SelectionType.Proportional
        /// </summary>
        this.Proportion=proportion;

        /// <summary>
        /// Specifies the quantity for SelectionType.FixedQuantity
        /// </summary>
        this.Quantity= quantity;

        /// <summary>
        /// The perturbation factor for ConnectionPerturbationType.JiggleEven.
        /// </summary>
        this.PerturbationFactor= perturbationFactor;

        /// <summary>
        /// Sigma for for ConnectionPerturbationType.JiggleND.
        /// </summary>
        this.Sigma= sigma;
    }

    public static ConnectionMutationParameterGroup Copy(ConnectionMutationParameterGroup copyFrom) {
        return new ConnectionMutationParameterGroup(
                copyFrom.ActivationProportion,
                copyFrom.PerturbationType,
                copyFrom.SelectionType,
                copyFrom.Proportion,
                copyFrom.Quantity,
                copyFrom.PerturbationFactor,
                copyFrom.Sigma
        );
    }
}

