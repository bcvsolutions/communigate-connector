package eu.bcvsolutions.idm.connector.cgate.execute;

import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;

public interface ExecuteQuery {

    void execute(String query, ResultsHandler handler, OperationOptions opt);
}
