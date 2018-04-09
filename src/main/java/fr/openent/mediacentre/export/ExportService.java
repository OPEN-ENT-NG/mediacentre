package fr.openent.mediacentre.export;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public interface ExportService {

    void launchExport(final Handler<Either<String,JsonObject>> handler);
    void launchExport(final Message<JsonObject> message);
}
