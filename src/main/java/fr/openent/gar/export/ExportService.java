package fr.openent.gar.export;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public interface ExportService {

    void launchExport(final Handler<Either<String,JsonObject>> handler);
    void launchExport(final Message<JsonObject> message);
}
