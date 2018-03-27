package fr.openent.mediacentre.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public interface DataService {

    /**
     * Export Data to folder
     * @param path folder path
     * @param handler response handler
     */
    void exportData(String path, Handler<Either<String, JsonObject>> handler);

}
