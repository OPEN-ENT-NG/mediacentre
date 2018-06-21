package fr.openent.mediacentre.export;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public interface DataService {

    String FILE_LIST_KEY = "filelist";

    /**
     * Export Data to folder
     * @param handler response handler
     */
    void exportData(final Handler<Either<String, JsonObject>> handler);

}
