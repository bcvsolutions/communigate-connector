package eu.bcvsolutions.idm.connector.cgate.update;

import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;

public interface Update {

    Uid update(Uid uid, Set<Attribute> attributes, OperationOptions options);
}
