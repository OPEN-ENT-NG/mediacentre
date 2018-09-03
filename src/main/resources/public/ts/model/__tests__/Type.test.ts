import { Type } from "../Type";

import data from "../__mocks__/ressources";

const level = data.listeRessources.ressource[0].niveauEducatif[0];
const { nom, uri } = level;
const type = new Type(nom, uri);

describe(" constructor(nom: string = '', uri: string = '')", () => {
  test("Initializing Type with no arguments should render empty string as 'nom' and 'uri'", () => {
    let type = new Type();
    expect(type.nom).toEqual("");
    expect(type.uri).toEqual("");
  });

  test(`Initializing Type with '${level.nom}' and '${
    level.uri
  }' should init Type '${level.nom}' and '${level.uri}' strings`, () => {
    let { nom, uri } = level;
    expect(type.nom).toEqual(nom);
    expect(type.uri).toEqual(uri);
  });
});

describe("toString(): string", () => {
  test("toString function should return the Type nom key", () => {
    expect(type.toString()).toEqual(level.nom);
  });
});
