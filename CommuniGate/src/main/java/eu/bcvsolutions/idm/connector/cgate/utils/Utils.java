package eu.bcvsolutions.idm.connector.cgate.utils;

import java.util.Arrays;
import java.util.Vector;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;

import eu.bcvsolutions.idm.connector.cgate.CommuniGateConnector;
import eu.bcvsolutions.idm.connector.cgate.GuardedStringAccessor;

public class Utils {

    private Log log = Log.getLog(Utils.class);

    /**
     * Transform to String
     * @param object
     * @return
     * Transformace:
     * 	GuardedString to String
     * 	String[]  to String - "\n"
     * 	Object to String.
     */
    public static String asString(Object object) {
        if (object instanceof GuardedString) {
            GuardedString guarded = (GuardedString)object;
            GuardedStringAccessor accessor = new GuardedStringAccessor();
            guarded.access(accessor);
            char[] result = accessor.getArray();
            return new String(result);
        } else if (object instanceof String [] ) {
            StringBuilder st = new StringBuilder();
            String item = null;

            String[] array = (String[]) object;
            for (int i = 0; i < array.length; i++) {
                item = array[i];
                if (item == null) {
                    continue;
                }
                st.append(item);
                st.append("\n");
            }
            return st.toString();
        } else if (object != null) {
            return object.toString();
        } else {
            return null;
        }
    }

    /**
     * Transforms object as Vector.
     * Handles object of type String and Vector, otherwise returns empty Vector.
     *
     * @param attr input object
     * @return object as Vector
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Vector asVector(Object attr) {
        if (attr == null) {
            return null;
        }
        if (attr instanceof Vector) {
            return (Vector) attr;
        }
        if (attr instanceof String) {
            Vector vec = new Vector();
            vec.addElement((String) attr);
            return vec;
        }
        return new Vector();
    }

    /**
     * Transforms access modes for use in CLI setAccountSettings method.
     * Returns unchanged Vector, or String if it is in reserved words.
     *
     * @param accModes access modes
     * @return transformed access modes for CLI
     */
    @SuppressWarnings("rawtypes")
    public static Object transformAccModes(Vector accModes) {
        if (accModes != null && accModes.size() == 1) {
            String mode = (String) accModes.get(0);
            if (Arrays.asList(CommuniGateConnector.ACCESS_MODES_RESERVED_VALUES).contains(mode)) {
                return mode;
            }
        }
        return accModes;
    }
}
