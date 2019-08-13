package fr.openent.gar.service.impl;

import fr.openent.gar.service.ParameterService;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

public class DefaultParameterService implements ParameterService {

    private EventBus eb;
    private static final String GAR_GROUP_NAME = "RESP-AFFECT-GAR";
    private static final String GAR_LINK_NAME = "GAR_AFFECTATION_IHM_CONNECTEUR";
    private static final String FUNCTION_DIRECTION_NAME = "DIR";
    private static final String FUNCTION_DOCUMENTATION_NAME = "DOC";


    public DefaultParameterService(EventBus eb) {
        this.eb = eb;
    }

    @Override
    public void undeployStructureGar(String structureId, Handler<Either<String, JsonObject>> handler) {
        String query = "MATCH (s:Structure {id:{structureId}}) SET s.exports = FILTER(val IN s.exports WHERE val <> 'GAR') RETURN s.exports;";
        JsonObject params = new JsonObject()
                .put("structureId", structureId);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getStructureGar(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE HAS(s.UAI) OPTIONAL MATCH (s)<-[:DEPENDS]-(g:ManualGroup{name: {groupName} })" +
                "RETURN DISTINCT s.UAI as uai, s.name as name, s.id as structureId, (HAS(s.exports) AND 'GAR' IN s.exports) as deployed, g.id as id";

        JsonObject params = new JsonObject().put("groupName", GAR_GROUP_NAME);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void createGarGroupToStructure(JsonObject body, Handler<Either<String, JsonObject>> handler) {
        String query = "MATCH (s:Structure {id:{structureId}}) " +
                "OPTIONAL MATCH (s)<-[:DEPENDS]-(g:ManualGroup{name: {groupName} }) SET s.exports = coalesce(s.exports, []) + 'GAR' RETURN g.id as groupId";
        JsonObject creationParams = new JsonObject()
                .put("structureId", body.getString("structureId"))
                .put("groupName", GAR_GROUP_NAME);
        Neo4j.getInstance().execute(query, creationParams, Neo4jResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                handler.handle(new Either.Left<>("Failed to deploy structure"));
                return;
            }

            JsonObject creationResult = either.right().getValue();
            if (!(null == creationResult.getValue("groupId"))) {
                handler.handle(new Either.Right<>(new JsonObject()));
                return;
            }

            body.put("groupDisplayName", body.getString("name"));
            JsonObject action = new JsonObject()
                    .put("action", "manual-create-group")
                    .put("structureId", body.getString("structureId"))
                    .put("classId", body.getString("classId"))
                    .put("group", body);
            eb.send("entcore.feeder", action, (Handler<AsyncResult<Message<JsonObject>>>) createGarResult -> {
                if (createGarResult.failed()) {
                    handler.handle(new Either.Left<>("Failed to create gar group"));
                    return;
                }

                String groupId = createGarResult.result().body()
                        .getJsonArray("results")
                        .getJsonArray(0)
                        .getJsonObject(0).getString("id");

                String queryRole = "MATCH (a:Application)-[]->(ac:Action)<-[]-(r:Role)" +
                        " WHERE a.name = {linkName} RETURN r.id as id";

                Neo4j.getInstance().execute(queryRole, new JsonObject().put("linkName", GAR_LINK_NAME),
                        Neo4jResult.validUniqueResultHandler(linkResult -> {
                            if (linkResult.isLeft()) {
                                handler.handle(new Either.Left<>("Failed to fetch role id"));
                            }
                            String roleId = linkResult.right().getValue().getString("id");
                            String queryLink = "MATCH (r:Role), (g:Group) " +
                                    "WHERE r.id = {roleId} and g.id = {groupId} " +
                                    "CREATE UNIQUE (g)-[:AUTHORIZED]->(r)";
                            JsonObject params = new JsonObject()
                                    .put("groupId", groupId)
                                    .put("roleId", roleId);
                            Neo4j.getInstance().execute(queryLink, params, Neo4jResult.validUniqueResultHandler(handler));
                        }));
            });
        }));
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
