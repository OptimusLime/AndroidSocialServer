package eplex.win.FastNEATJava.help;

import java.util.ArrayList;

import eplex.win.FastNEATJava.utils.cuid;

/**
 * Created by paul on 8/16/14.
 */
public class CorrelationResults {

    public CorrelationStatistics correlationStatistics = new CorrelationStatistics();
    public ArrayList<CorrelationItem> correlationList = new ArrayList<CorrelationItem>();

    public boolean performIntegrityCheckByInnovation()
    {
        String prevInnovationId= "";

        for(CorrelationItem correlationItem : correlationList)
        {
            switch(correlationItem.correlationType)
            {
                // Disjoint and excess genes.
                case disjointConnectionGene:
                case excessConnectionGene:
                    // Disjoint or excess gene.
                    if((correlationItem.connection1 == null && correlationItem.connection2 == null)
                            ||	(correlationItem.connection1 != null && correlationItem.connection2 != null))
                    {	// Precisely one gene should be present.
                        return false;
                    }
                    if(correlationItem.connection1 != null)
                    {
                        if(cuid.isLessThan(correlationItem.connection1.gid, prevInnovationId)
                                || correlationItem.connection1.gid.equals(prevInnovationId))
                            return false;

                        prevInnovationId = correlationItem.connection1.gid;
                    }
                    else // ConnectionGene2 is present.
                    {
                        if(cuid.isLessThan(correlationItem.connection2.gid, prevInnovationId)
                                || (correlationItem.connection2.gid.equals(prevInnovationId)))
                            return false;

                        prevInnovationId = correlationItem.connection2.gid;
                    }

                    break;
                case matchedConnectionGenes:

                    if(correlationItem.connection1 == null || correlationItem.connection2 == null)
                        return false;

                    if(		(!correlationItem.connection1.gid.equals(correlationItem.connection2.gid))
                            ||	(!correlationItem.connection1.sourceID.equals(correlationItem.connection2.sourceID))
                            ||	(!correlationItem.connection1.targetID.equals(correlationItem.connection2.targetID)))
                        return false;

                    // Innovation ID's should be in order and not duplicated.
                    if(cuid.isLessThan(correlationItem.connection1.gid, prevInnovationId)
                            || (correlationItem.connection1.gid.equals(prevInnovationId)))
                        return false;

                    prevInnovationId = correlationItem.connection1.gid;

                    break;
            }
        }

        return true;
    }
}
