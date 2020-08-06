package eu.bcvsolutions.idm.connector.cgate.update;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;

import java.util.Set;

public interface Update {

    Uid update(Uid uid, Set<Attribute> attributes, OperationOptions options);
}
