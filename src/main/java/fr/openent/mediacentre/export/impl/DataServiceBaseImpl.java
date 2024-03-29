package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.helper.XmlExportHelper;
import fr.openent.mediacentre.export.DataService;
import fr.wseduc.webutils.Either;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;


import java.util.HashMap;
import java.util.Map;

import static fr.openent.mediacentre.constants.GarConstants.*;

abstract class DataServiceBaseImpl implements DataService {
    static Map<String,String> mapStructures = new HashMap<>();
    XmlExportHelper xmlExportHelper;
    final Logger log = LoggerFactory.getLogger(DataServiceBaseImpl.class);
    final Neo4j neo4j = Neo4j.getInstance();

    DataServiceBaseImpl() {
    }

    /**
     * Process profiles, set profile from structMap for structures in it
     * Set default profile for other etabs
     * @param person person to process
     * @param profileName default profile
     * @param structMap map for profile by structure
     */
    void processProfiles(JsonObject person, String profileName, Map<String, String> structMap) {
        JsonArray profiles = person.getJsonArray("profiles");

        JsonArray garProfiles = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray garEtabs = new fr.wseduc.webutils.collections.JsonArray();
        for(Object o2 : profiles) {
            if(!(o2 instanceof String)) continue;
            String structure = ((String)o2);

            garEtabs.add(structure);

            if(structMap != null && structMap.containsKey(structure)) {
                addProfile(garProfiles, structure, structMap.get(structure));
                structMap.remove(structure);
            } else {
                addProfile(garProfiles, structure, profileName);
            }
        }
        if(structMap != null) {
            for (String structUAI : structMap.keySet()) {
                garEtabs.add(structUAI);
                addProfile(garProfiles, structUAI, profileName);
            }
        }
        person.put(PERSON_PROFILES, garProfiles);
        person.put(PERSON_STRUCTURE, garEtabs);
        person.remove("profiles");
    }

    /**
     * Save an array of JsonObjects in xml
     * Only save object with all mandatory fields present
     * @param array array from neo4j
     * @param name xml node name
     * @param mandatoryFields list of mandatory fields that must be present in the JsonObject
     * @return result
     */
    Either<String,JsonObject> processSimpleArray(JsonArray array, String name, String[] mandatoryFields) {
        try {

            for(Object o : array) {
                if (!(o instanceof JsonObject)) continue;
                JsonObject obj = (JsonObject)o;
                if(isMandatoryFieldsAbsent(obj, mandatoryFields)) {
                    log.warn(name + " object malformed : " + obj.toString());
                    continue;
                }
                xmlExportHelper.saveObject(name, obj);
            }
            return new Either.Right<>(null);
        } catch (Exception e){
            return new Either.Left<>(e.getMessage());
        }
    }

    boolean isMandatoryFieldsAbsent(JsonObject obj, String[] mandatoryFields) {
        if(obj == null) return true;
        for(String s : mandatoryFields) {
            if(!obj.containsKey(s) || null == obj.getValue(s)) return true;
        }
        return false;
    }

    /**
     * Validate response and trigger handler if not
     * @param event event to validate
     * @param handler handler to respond to
     * @return true if response is valid
     */
    boolean validResponseNeo4j(Either<String,JsonArray> event, final Handler<Either<String, JsonObject>> handler) {
        if(event.isLeft()) {
            handler.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
            log.error(event.left().getValue());
            return false;
        } else {
            return true;
        }
    }

    /**
     * Validate response and trigger handler if not
     * @param event event to validate
     * @param handler handler to respond to
     * @return true if response is valid
     */
    boolean validResponse(Either<String,JsonObject> event, final Handler<Either<String, JsonObject>> handler) {
        if(event.isLeft()) {
            handler.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
            return false;
        } else {
            return true;
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
        garProfile.put(STRUCTURE_UAI, structUAI);
        garProfile.put(PERSON_PROFILE, profile);
        profileArray.add(garProfile);
    }
}
