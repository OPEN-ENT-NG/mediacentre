package fr.openent.mediacentre.helper.impl;

import fr.openent.mediacentre.helper.PaginatorHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;

import static fr.openent.mediacentre.Mediacentre.CONFIG;
import static org.entcore.common.neo4j.Neo4jResult.validResult;

public class PaginatorHelperImpl implements PaginatorHelper  {

    public static int LIMIT;
    final Neo4j neo4j = Neo4j.getInstance();

    public PaginatorHelperImpl() {
        this.LIMIT = 25000;
        if(CONFIG.containsKey("pagination-limit")){
            this.LIMIT = CONFIG.getInteger("pagination-limit");
        }
    }


    @Override
    public void neoStreamList( String query, JsonObject params, JsonArray finalValues, int skip, Handler<Either<String, JsonArray>> handler) {
        params.put("limit", LIMIT);
        neo4j.execute(query, params.copy().put("skip", skip), res -> {
            Either<String, JsonArray> r = validResult(res);
            if (r.isRight()) {
                JsonArray rvalues = r.right().getValue();
                for (int i = 0; i < rvalues.size(); i++) {
                    finalValues.add(rvalues.getJsonObject(i));
                }
                if (rvalues.size() == LIMIT) {
                    neoStreamList(query, params, finalValues, skip + LIMIT, handler);
                } else {
                    handler.handle(new Either.Right<>(finalValues));
                }
            } else {
                handler.handle(new Either.Left<>(""));
            }

        });
    }

    @Override
    public void neoStreamList(String query, JsonObject params, int skip, Handler<Either<String, JsonArray>> handler, final Handler<Either<String, JsonObject>> handlerFinal) {
        params.put("limit", LIMIT);
        neo4j.execute(query, params.copy().put("skip", skip), res -> {
            Either<String, JsonArray> r = validResult(res);
            if (r.isRight()) {
                JsonArray rvalues = r.right().getValue();
                handler.handle(new Either.Right<>(rvalues));

                if (rvalues.size() == LIMIT) {
                    neoStreamList(query, params, skip + LIMIT, handler, handlerFinal);
                } else {
                    handlerFinal.handle(new Either.Right<>(new JsonObject()));
                }
            } else {
                handler.handle(new Either.Left<>(""));
            }

        });
    }
}
