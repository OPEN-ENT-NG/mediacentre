import { ng, template } from "entcore";
import {ParameterService} from "../services";

/**
 Parameter controller
 ------------------.
 **/
export const parameterController = ng.controller("ParameterController", [
    "$scope", "ParameterService", async ($scope, ParameterService: ParameterService) => {
    template.open("main", "parameter");

    const GROUP_GAR_NAME = "RESP-AFFECT-GAR";
    $scope.structureGarLists = await ParameterService.getStructureGar();

    /* button handler */
    $scope.createButton = false;
    $scope.addButton = false;

    $scope.export = () => {
        ParameterService.export();
    };

    $scope.createGarGroup = async (structureId: string) => {
        $scope.createButton = true;
        $scope.$apply();

        await ParameterService
            .createGroupGarToStructure(GROUP_GAR_NAME, structureId)
            .then(async res => {
                if (res["id"]) {
                    $scope.structureGarLists = await ParameterService.getStructureGar();
                    $scope.createButton = false;
                }
            })
            .catch(
                (err) => {
                    $scope.createButton = false;
                    throw err;
                }
            );
        $scope.$apply();
    };

    $scope.addUser = async (groupId, structureId) => {
        $scope.addButton = true;
        $scope.$apply();
        await ParameterService.addUsersToGarGroup(groupId, structureId);
        $scope.addButton = false;
        $scope.$apply();
    };

}]);
