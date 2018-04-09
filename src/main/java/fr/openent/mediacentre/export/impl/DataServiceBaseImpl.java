package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.helper.XmlExportHelper;
import fr.openent.mediacentre.export.DataService;
import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo4j;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.Map;

import static fr.openent.mediacentre.constants.GarConstants.*;

abstract class DataServiceBaseImpl implements DataService{

    XmlExportHelper xmlExportHelper;
    final Logger log;
    final String CONTROL_GROUP;

    final Neo4j neo4j = Neo4j.getInstance();

    DataServiceBaseImpl(Container container) {
        this.log = container.logger();
        this.CONTROL_GROUP = container.config().getString("control-group", DEFAULT_CONTROL_GROUP);
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
     * Save an array of JsonObjects in xml
     * @param array array from neo4j
     * @param name xml node name
     */
    void processSimpleArray(JsonArray array, String name) {
        try {

            for(Object o : array) {
                if (!(o instanceof JsonObject)) continue;
                xmlExportHelper.saveObject(name, (JsonObject)o);
            }
        } catch (Exception e){
            log.info(e.getMessage());
        }
    }

    /**
     * Save an array of JsonObjects in xml
     * @param array array from neo4j
     * @param name xml node name
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
        if(obj == null) return false;
        for(String s : mandatoryFields) {
            if(!obj.containsField(s)) return false;
        }
        return true;
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
        garProfile.putString(STRUCTURE_UAI, structUAI);
        garProfile.putString(PERSON_PROFILE, profile);
        profileArray.addObject(garProfile);
    }



}
