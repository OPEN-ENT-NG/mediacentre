package fr.openent.mediacentre.service.impl;

import fr.openent.mediacentre.helper.XmlExportHelper;
import fr.openent.mediacentre.service.DataService;
import org.entcore.common.neo4j.Neo4j;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.Map;

import static fr.openent.mediacentre.constants.GarConstants.*;

abstract class DataServiceBasePersonImpl implements DataService{

    XmlExportHelper xmlExportHelper;
    final Logger log;
    final String CONTROL_GROUP;

    Neo4j neo4j = Neo4j.getInstance();

    DataServiceBasePersonImpl(Container container) {
        this.log = container.logger();
        this.CONTROL_GROUP = container.config().getString("control-group", DEFAULT_CONTROL_GROUP);
    }

    /**
     * Process profiles without adaptive profile
     * @param person person to process
     * @param profileName profile to fill
     */
    void processProfiles(JsonObject person, String profileName) {
        processProfiles(person, profileName, null);
    }

    /**
     * Process profiles, set profile from structMap for structures in it
     * Set default profile for other etabs
     * @param person person to process
     * @param profileName default profile
     * @param structMap map for profile by structure
     */
    void processProfiles(JsonObject person, String profileName, Map<String, String> structMap) {
        JsonArray profiles = person.getArray("profiles");

        JsonArray garProfiles = new JsonArray();
        JsonArray garEtabs = new JsonArray();
        for(Object o2 : profiles) {
            if(!(o2 instanceof String)) continue;
            String structure = ((String)o2);

            garEtabs.addString(structure);

            if(structMap != null && structMap.containsKey(structure)) {
                addProfile(garProfiles, structure, structMap.get(structure));
                structMap.remove(structure);
            } else {
                addProfile(garProfiles, structure, profileName);
            }
        }
        if(structMap != null) {
            for (String structUAI : structMap.keySet()) {
                garEtabs.addString(structUAI);
                addProfile(garProfiles, structUAI, profileName);
            }
        }
        person.putArray(PERSON_PROFILES, garProfiles);
        person.putArray(PERSON_STRUCTURE, garEtabs);
        person.removeField("profiles");
    }

    /**
     * Process mefs info
     * @param mefs Array of mefs from Neo4j
     */
    void processPersonsMefs(JsonArray mefs) {
        processSimpleArray(mefs, PERSON_MEF);
    }

    /**
     * Save an array of JsonObjects in xml
     * @param array array from neo4j
     * @param name xml node name
     */
    void processSimpleArray(JsonArray array, String name) {
        for(Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            xmlExportHelper.saveObject(name, (JsonObject)o);
        }
    }

    /**
     * Add a profile in profileArray
     * @param profileArray Array to fill
     * @param structUAI structure UAI
     * @param profile profile name
     */
    private void addProfile(JsonArray profileArray, String structUAI, String profile) {
        JsonObject garProfile = new JsonObject();
        garProfile.putString(STRUCTURE_UAI, structUAI);
        garProfile.putString(PERSON_PROFILE, profile);
        profileArray.addObject(garProfile);
    }



}
