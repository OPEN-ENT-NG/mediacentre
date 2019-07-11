package fr.openent.mediacentre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ParameterService {

    /**
     * Get Structure with optional gar group
     * @param handler Function handler returning data
     */
    void getStructureGar(Handler<Either<String, JsonArray>> handler);

    /**
     * Create new group gar to chosen structure
     * @param body          body query
     * @param result        Function handler returning data
     */
    void createGarGroupToStructure(JsonObject body, Handler<Either<String, JsonObject>> result);

    /**
     * Add specific user to gar group selected
     * @param body          body query
     * @param result        Function handler returning data
     */
    void addUserToGarGroup(JsonObject body, Handler<Either<String, JsonObject>> result);
}
