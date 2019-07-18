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
    $scope.$apply();

    $scope.export = () => {
        ParameterService.export();
    };

    $scope.createGarGroup = async (structureId: string) => {
        $scope.createButton = true;
        $scope.$apply();

        let response = await ParameterService.createGroupGarToStructure(GROUP_GAR_NAME, structureId);
        if (response.status === 200) {
            $scope.structureGarLists = await ParameterService.getStructureGar();
            $scope.createButton = false;
        }
        $scope.createButton = false;
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
