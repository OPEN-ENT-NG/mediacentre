package fr.openent.mediacentre.service.impl;

import fr.openent.mediacentre.service.ParameterService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class DefaultParameterService implements ParameterService {

    private EventBus eb;
    private static final String GAR_GROUP_NAME = "RESP-AFFECT-GAR";
    private static final String FUNCTION_DIRECTION_NAME = "DIR";
    private static final String FUNCTION_DOCUMENTATION_NAME = "DOC";


    public DefaultParameterService(EventBus eb) {
        this.eb = eb;
    }

    @Override
    public void getStructureGar(Handler<Either<String, JsonArray>> handler) {
        String query = "match (s:Structure) OPTIONAL MATCH (s)<-[:DEPENDS]-(g:ManualGroup{name: {groupName} }) " +
                "return s.UAI as uai, s.name as name, s.id as structureId, g.id as id";

        JsonObject params = new JsonObject().put("groupName", GAR_GROUP_NAME);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void createGarGroupToStructure(JsonObject body, Handler<Either<String, JsonObject>> handler) {
        body.put("groupDisplayName", body.getString("name"));
        JsonObject action = new JsonObject()
                .put("action", "manual-create-group")
                .put("structureId", body.getString("structureId"))
                .put("classId", body.getString("classId"))
                .put("group", body);
        eb.send( "entcore.feeder", action, handlerToAsyncHandler(validUniqueResultHandler(0, handler)));
    }

    @Override
    public void addUserToGarGroup(JsonObject body, Handler<Either<String, JsonObject>> handler) {
        String query = "match (g:ManualGroup{name: {groupName}, id: {groupId} }), " +
                "(u:User{profiles:['Personnel']})--(Structure{id: {structureId} }) " +
                "WHERE ANY(function IN u.functions WHERE function CONTAINS {direction} OR function CONTAINS {documentation}) " +
                "create unique (u)-[:IN]->(g)";

        JsonObject params = new JsonObject()
                .put("groupName", GAR_GROUP_NAME)
                .put("groupId", body.getString("groupId"))
                .put("structureId", body.getString("structureId"))
                .put("direction", FUNCTION_DIRECTION_NAME)
                .put("documentation", FUNCTION_DOCUMENTATION_NAME);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validUniqueResultHandler(handler));
    }
}
