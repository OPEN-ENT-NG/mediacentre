package fr.openent.gar.export;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface DataService {

    String FILE_LIST_KEY = "filelist";

    /**
     * Export Data to folder
     * @param handler response handler
     */
    void exportData(final Handler<Either<String, JsonObject>> handler);

}
