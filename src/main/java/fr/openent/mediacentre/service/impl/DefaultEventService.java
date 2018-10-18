package fr.openent.mediacentre.service.impl;

import fr.openent.mediacentre.service.EventService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.common.user.UserInfos;

public class DefaultEventService extends MongoDbCrudService implements EventService {

    public DefaultEventService(String collection) {
        super(collection);
    }

    @Override
    public void add(JsonObject event, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.create(event, user, handler);
    }
}
