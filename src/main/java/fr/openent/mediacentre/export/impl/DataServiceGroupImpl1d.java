package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.export.DataService;
import fr.openent.mediacentre.helper.impl.PaginatorHelperImpl;
import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.openent.mediacentre.constants.GarConstants.*;

public class DataServiceGroupImpl1d extends DataServiceBaseImpl implements DataService {

    private PaginatorHelperImpl paginator;
    private JsonObject config;
    private String entId;
    private String source;
    private Boolean hasAcademyPrefix;

    DataServiceGroupImpl1d(String entId, String source, JsonObject config, String strDate) {
        this.entId = entId;
        this.source = source;
        this.config = config;
        xmlExportHelper = new XmlExportHelperImpl(entId, source, config, GROUPS_ROOT, GROUPS_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
        hasAcademyPrefix = this.config.containsKey("academy-prefix") && !"".equals(this.config.getString("academy-prefix").trim());
    }

    /**
     * Export Data to folder
     * - Export Groups info
     * - Export Groups content (people into the groups)
     * - Export Groups fields of study     *
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {
        getAndProcessGroupsInfoFromNeo4j(groupsResults -> {
            if (validResponse(groupsResults, handler)) {
                getAndProcessGroupsPersonFromNeo4j(groupPersonResults -> {
                    if (validResponse(groupPersonResults, handler)) {
                        xmlExportHelper.closeFile();
                        handler.handle(new Either.Right<String, JsonObject>(
                                new JsonObject().put(
                                        FILE_LIST_KEY,
                                        xmlExportHelper.getFileList()
                                )));
                    }
                });
            }
        });
    }

    /**
     * Process groups info, validate data and save to xml
     *
     * @param handler result handler
     */
    private void getAndProcessGroupsInfoFromNeo4j(final Handler<Either<String, JsonObject>> handler) {
        final Map<String, Map<String, List<String>>> mapStructGroupClasses = new HashMap<>();
        getGroupsStructureInfoFromNeo4j(mapStructGroupClasses, res -> {
            if (res.isRight()) {
                getAndProcessDivisionGroupsInfoFromNeo4j(0, event -> {
                    if (validResponse(event, handler)) {
                        getAndProcessOtherGroupsInfoFromNeo4j(0, mapStructGroupClasses, handler);
                    }
                });
            } else {
                handler.handle(new Either.Left<>(res.left().getValue()));
            }
        });
    }

    private void getAndProcessDivisionGroupsInfoFromNeo4j(int skip, final Handler<Either<String, JsonObject>> handler) {
        getDivisionGroupsInfoFromNeo4j(skip, groupsResults -> {
            if (validResponseNeo4j(groupsResults, handler)) {
                Either<String, JsonObject> result = processGroupsInfo(groupsResults.right().getValue());

                if (groupsResults.right().getValue().size() == PaginatorHelperImpl.LIMIT) {
                    getAndProcessDivisionGroupsInfoFromNeo4j(skip + PaginatorHelperImpl.LIMIT, handler);
                } else {
                    handler.handle(result);
                }
            } else {
                log.error("[DataServiceGroupImpl@getAndProcessDivisionGroupsInfoFromNeo4j] Failed to process");
            }
        });
    }

    private void getAndProcessOtherGroupsInfoFromNeo4j(int skip, final Map<String, Map<String, List<String>>> mapStructGroupClasses, final Handler<Either<String, JsonObject>> handler) {
        getOtherGroupsInfoFromNeo4j(skip, groupsResults -> {
            if (validResponseNeo4j(groupsResults, handler)) {
                final JsonArray results = groupsResults.right().getValue();
                if (results != null && !results.isEmpty()) {
                    for (final Object obj : results.getList()) {
                        if (!(obj instanceof JsonObject)) continue;
                        final JsonObject jo = (JsonObject) obj;

                        final Map<String, List<String>> groupClasses = mapStructGroupClasses.get(jo.getString(STRUCTURE_UAI));
                        if (groupClasses != null) {
                            if (groupClasses.containsKey(jo.getString(GROUPS_CODE))) {
                                jo.put(GROUPS_DIVISION, new JsonArray(groupClasses.get(jo.getString(GROUPS_CODE))));
                            }
                        }
                    }

                    Either<String, JsonObject> result = processGroupsInfo(results);

                    if (results.size() == PaginatorHelperImpl.LIMIT) {
                        getAndProcessOtherGroupsInfoFromNeo4j(skip + PaginatorHelperImpl.LIMIT, mapStructGroupClasses, handler);
                    } else {
                        handler.handle(result);
                    }
                } else {
                    handler.handle(new Either.Right<>(null));
                }
            } else {
                log.error("[DataServiceGroupImpl@getAndProcessOtherGroupsInfoFromNeo4j] Failed to process");
            }
        });
    }

    private void getAndProcessGroupsPersonFromNeo4j(final Handler<Either<String, JsonObject>> handler) {
        getAndProcessDivisionGroupsPersonFromNeo4j(0, event -> {
            if (validResponse(event, handler)) {
                getAndProcessOtherGroupsPersonFromNeo4j(0, handler);
            }
        });
    }

    private void getAndProcessDivisionGroupsPersonFromNeo4j(int skip, final Handler<Either<String, JsonObject>> handler) {
        getDivisionGroupsPersonFromNeo4j(skip, groupPersonResults -> {
            if (validResponseNeo4j(groupPersonResults, handler)) {
                Either<String, JsonObject> result = processGroupPersonInfo(groupPersonResults.right().getValue());

                if (groupPersonResults.right().getValue().size() == PaginatorHelperImpl.LIMIT) {
                    getAndProcessDivisionGroupsPersonFromNeo4j(skip + PaginatorHelperImpl.LIMIT, handler);
                } else {
                    handler.handle(result);
                }
            } else {
                log.error("[DataServiceGroupImpl@getAndProcessDivisionGroupsPersonFromNeo4j] Failed to process");
            }
        });
    }

    private void getAndProcessOtherGroupsPersonFromNeo4j(int skip, final Handler<Either<String, JsonObject>> handler) {
        getOtherGroupsPersonFromNeo4j(skip, groupPersonResults -> {
            if (validResponseNeo4j(groupPersonResults, handler)) {
                Either<String, JsonObject> result = processGroupPersonInfo(groupPersonResults.right().getValue());

                if (groupPersonResults.right().getValue().size() == PaginatorHelperImpl.LIMIT) {
                    getAndProcessOtherGroupsPersonFromNeo4j(skip + PaginatorHelperImpl.LIMIT, handler);
                } else {
                    handler.handle(result);
                }
            } else {
                log.error("[DataServiceGroupImpl@getAndProcessOtherGroupsPersonFromNeo4j] Failed to process");
            }
        });
    }

    private void getGroupsStructureInfoFromNeo4j(Map<String, Map<String, List<String>>> mapStructGroupClasses, Handler<Either<String, Boolean>> handler) {
        //in AAF1D s.groups is null for the moment
        final String divisionQuery = "MATCH (s:Structure {source:'"+ this.source +"'}) WHERE HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports " +
                "RETURN distinct s.UAI as uai, s.groups as groups";
        JsonObject params = new JsonObject().put("entId", entId);

        neo4j.execute(divisionQuery, params, res -> {
            if (res.body() != null && res.body().containsKey("result")) {
                JsonArray queryResult = Utils.getOrElse(res.body().getJsonArray("result"), new JsonArray());

                queryResult.forEach((entry) -> {
                    if (entry instanceof JsonObject) {
                        JsonObject field = (JsonObject) entry;
                        String uai = field.getString("uai", "");
                        final HashMap<String, List<String>> mapGroup = new HashMap<>();

                        final JsonArray groups = field.getJsonArray("groups");

                        if (groups != null && !groups.isEmpty()) {
                            groups.forEach((group) -> {
                                String gp = (String) group;
                                if (gp != null && !gp.isEmpty()) {
                                    final String[] elems = gp.split("\\$");
                                    if (elems.length >= 3) {
                                        final List<String> classes = new ArrayList<String>();
                                        for (int i = 2; i < elems.length; i++) {
                                            classes.add(elems[i]);
                                        }

                                        if (!classes.isEmpty()) {
                                            mapGroup.put(elems[0], classes);
                                        }
                                    }
                                }
                            });

                            if (!mapGroup.isEmpty()) {
                                mapStructGroupClasses.put(uai, mapGroup);
                            }
                        }
                    }
                });
                handler.handle(new Either.Right<>(true));
            } else {
                log.error("[DataServiceGroupImple@getGroupsStructureInfoFromNeo4j] Failed to process divisionQuery");
                handler.handle(new Either.Left<>("error"));
            }
        });
    }

    /**
     * Get groups info from Neo4j
     * Get classes (or divisions)
     * Get user groups. Link groups to classes only for students
     * Get group external id if it exists, else get internal id
     *
     * @param handler results
     */
    private void getOtherGroupsInfoFromNeo4j(int skip, Handler<Either<String, JsonArray>> handler) {
        final String condition;
        if (hasAcademyPrefix) {
            condition = "CASE WHEN fg.externalId =~ '(" + this.config.getString("academy-prefix") + ")-[A-Z0-9-]+' THEN reduce(v=fg.externalId, prefix in split('" +
                    this.config.getString("academy-prefix") +"', '|') | replace(v, prefix + '-', '')) ELSE fg.externalId END as fgcode";
        } else {
            condition = "split(fg.externalId,\"-\") as fgcode";
        }

        final String groupsQuery = "MATCH (u:User)-[:IN]->(fg:FunctionalGroup)-[d2:DEPENDS]->" +
                "(s:Structure {source:'" + this.source + "'}) " +
                "WHERE HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports " +
                "AND head(u.profiles) IN ['Student', 'Teacher'] " +
                "AND NOT(HAS(u.deleteDate)) " +
                "AND NOT(HAS(u.disappearanceDate)) " +
                "with distinct s.UAI as uai, fg, " + condition +
                " return distinct coalesce(" + (hasAcademyPrefix ? "fgcode" : "fgcode[size(fgcode)-1]") + ", fg.id) as `" + GROUPS_CODE + "`, " +
                "uai as `" + STRUCTURE_UAI + "`, " +
                "fg.name as `" + GROUPS_DESC + "`, " +
                "\"" + GROUPS_GROUP_NAME + "\" as `" + GROUPS_STATUS + "` " +
                "order by `" + STRUCTURE_UAI + "`, `" + GROUPS_CODE + "` " +
                " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT).put("entId", entId);
        paginator.neoStream(groupsQuery, params, skip, handler);
    }


    /**
     * Get groups info from Neo4j
     * Get classes (or divisions)
     * Get user groups. Link groups to classes only for students
     * Get group external id if it exists, else get internal id
     *
     * @param handler results
     */
    private void getDivisionGroupsInfoFromNeo4j(int skip, Handler<Either<String, JsonArray>> handler) {
        //in 1D (no more split(c.externalId,"$")[1] for code)
        final String condition;
        if (hasAcademyPrefix) {
            condition = "CASE WHEN c.externalId =~ '(" + this.config.getString("academy-prefix") + ")-[A-Z0-9-]+' THEN reduce(v=c.externalId, prefix in split('" +
                    this.config.getString("academy-prefix") +"', '|') | replace(v, prefix + '-', '')) ELSE c.externalId END as ccode";
        } else {
            condition = "split(c.externalId,\"-\") as ccode";
        }
        final String classQuery = "MATCH (c:Class)-[:BELONGS]->(s:Structure {source:'" + this.source + "'}) " +
                "WHERE HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports " +
                "WITH distinct s.UAI as uai, c, " + condition +
                " RETURN distinct " +
                "coalesce(" + (hasAcademyPrefix ? "ccode" : "ccode[size(ccode)-1]") + ", c.id) as `" + GROUPS_CODE + "`, " +
                "uai as `" + STRUCTURE_UAI + "`, " +
                "c.name as `" + GROUPS_DESC + "`, " +
                "\"" + GROUPS_DIVISION_NAME + "\" as `" + GROUPS_STATUS + "` " +
                "order by `" + STRUCTURE_UAI + "`, `" + GROUPS_CODE + "` " +
                " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT).put("entId", entId);

        paginator.neoStream(classQuery, params, skip, handler);
    }


    /**
     * Process groups info
     *
     * @param groups Array of groups from Neo4j
     */
    private Either<String, JsonObject> processGroupsInfo(JsonArray groups) {
        Either<String, JsonObject> event = processSimpleArray(groups, GROUPS_NODE, GROUPS_NODE_MANDATORY);
        if (event.isLeft()) {
            return new Either.Left<>("Error when processing groups infos : " + event.left().getValue());
        } else {
            return event;
        }
    }

    /**
     * Get groups content from Neo4j
     * Use external id for groups when available
     *
     * @param handler results
     */
    private void getDivisionGroupsPersonFromNeo4j(int skip, Handler<Either<String, JsonArray>> handler) {
        //in 1D (no more split(c.externalId,"$")[1] for code)
        final String condition;
        if (hasAcademyPrefix) {
            condition = "CASE WHEN c.externalId =~ '(" + this.config.getString("academy-prefix") + ")-[A-Z0-9-]+' THEN reduce(v=c.externalId, prefix in split('" +
                    this.config.getString("academy-prefix") +"', '|') | replace(v, prefix + '-', '')) ELSE c.externalId END as ccode";
        } else {
            condition = "split(c.externalId,\"-\") as ccode";
        }
        final String classQuery = "MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(s:Structure {source:'" + this.source + "'})" +
                "WHERE HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports " +
                "AND pg.filter IN ['Student', 'Teacher'] " +
                "AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) " +
                "WITH distinct s.UAI as uai, u.id as uid, c, " + condition +
                " return distinct uai as `" + STRUCTURE_UAI + "`, " +
                "uid as `" + PERSON_ID + "`, " +
                "coalesce(" + (hasAcademyPrefix ? "ccode" : "ccode[size(ccode)-1]") + ", c.id) as `" + GROUPS_CODE + "` " +
                "order by `" + PERSON_ID + "`, `" + GROUPS_CODE + "`, `" + STRUCTURE_UAI + "` " +
                " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT).put("entId", entId);

        paginator.neoStream(classQuery, params, skip, handler);
    }

    /**
     * Get groups content from Neo4j
     * Use external id for groups when available
     *
     * @param handler results
     */
    private void getOtherGroupsPersonFromNeo4j(int skip, Handler<Either<String, JsonArray>> handler) {
        final String condition;
        if (hasAcademyPrefix) {
            condition = "CASE WHEN fg.externalId =~ '(" + this.config.getString("academy-prefix") + ")-[A-Z0-9-]+' THEN reduce(v=fg.externalId, prefix in split('" +
                    this.config.getString("academy-prefix") +"', '|') | replace(v, prefix + '-', '')) ELSE fg.externalId END as fgcode";
        } else {
            condition = "split(fg.externalId,\"-\") as fgcode";
        }
        final String groupsQuery = "MATCH (u:User)-[:IN]->(fg:FunctionalGroup)-[:DEPENDS]->(s:Structure {source:'" + this.source + "'}) " +
                "WHERE HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports " +
                "AND head(u.profiles) IN ['Student', 'Teacher'] " +
                "AND NOT(HAS(u.deleteDate)) " +
                "AND NOT(HAS(u.disappearanceDate)) " +
                "with distinct s.UAI as uai, u.id as uid, fg, " + condition +
                " return distinct uai as `" + STRUCTURE_UAI + "`, " +
                "uid as `" + PERSON_ID + "`, " +
                "coalesce(" + (hasAcademyPrefix ? "fgcode" : "fgcode[size(fgcode)-1]") + ", fg.id) as `" + GROUPS_CODE + "` " +
                "order by `" + PERSON_ID + "`, `" + GROUPS_CODE + "`, `" + STRUCTURE_UAI + "`" +
                " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT).put("entId", entId);

        paginator.neoStream(groupsQuery, params, skip, handler);
    }

    /**
     * Process groups content
     *
     * @param groupPerson Array of group content from Neo4j
     */
    private Either<String, JsonObject> processGroupPersonInfo(JsonArray groupPerson) {
        Either<String, JsonObject> event =
                processSimpleArray(groupPerson, GROUPS_PERSON_NODE, GROUPS_PERSON_NODE_MANDATORY);
        if (event.isLeft()) {
            return new Either.Left<>("Error when processing groups content : " + event.left().getValue());
        } else {
            return event;
        }
    }
}
