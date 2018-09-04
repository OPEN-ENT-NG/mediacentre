package fr.openent.mediacentre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface EventService {

    /**
     * Store event in the database
     *
     * @param event   Event to store
     * @param user    User triggering event
     * @param handler Function handler returning data
     */
    void add(JsonObject event, UserInfos user, Handler<Either<String, JsonObject>> handler);
}
