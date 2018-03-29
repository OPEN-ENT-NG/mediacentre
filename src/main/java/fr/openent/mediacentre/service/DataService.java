package fr.openent.mediacentre.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public interface DataService {

    /**
     * Export Data to folder
     * @param handler response handler
     */
    void exportData(Handler<Either<String, JsonObject>> handler);

}
