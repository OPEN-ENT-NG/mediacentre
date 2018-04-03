package fr.openent.mediacentre.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public interface DataService {

    static Map<String,String> mapStructures = new HashMap<>();

    /**
     * Export Data to folder
     * @param handler response handler
     */
    void exportData(final Handler<Either<String, JsonObject>> handler);

}
