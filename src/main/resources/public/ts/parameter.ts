import {ng} from 'entcore';
import {parameterController} from "./controllers";
import {ParameterService} from "./services";

ng.controllers.push(parameterController);
ng.services.push(ParameterService);

