import { Eventer } from "entcore-toolkit";

import { Resource, Resources, Types, Structures } from "../index";
import data from "../__mocks__/ressources";

const resources = new Resources();
const structures = new Structures();
structures.sync();
const eventerCallback = jest.fn();

describe("constructor()", () => {
  it("should init Resources collection correctly", () => {
    expect(resources.all).toBeTruthy();
    expect(resources.all).toBeInstanceOf(Array);
    expect(resources.all.length).toEqual(0);
    expect(resources.eventer).toBeInstanceOf(Eventer);
    expect(resources.levels).toBeTruthy();
    expect(resources.levels).toBeInstanceOf(Types);
    expect(resources.teachingFields).toBeTruthy();
    expect(resources.teachingFields).toBeInstanceOf(Types);
    expect(resources.educationalTypes).toBeTruthy();
    expect(resources.educationalTypes).toBeInstanceOf(Types);
    expect(resources.typologies).toBeTruthy();
    expect(resources.typologies).toBeInstanceOf(Types);
    expect(Object.keys(resources).length).toEqual(6);
    expect(Object.keys(resources)).toEqual([
      "all",
      "eventer",
      "levels",
      "teachingFields",
      "educationalTypes",
      "typologies"
    ]);
  });
});

describe("async sync(structure: Structure): Promise<void> ", () => {
  it("should load 'all' array with 2 objects", async () => {
    await resources.sync(structures.all[0]);
    expect(resources.all.length).toEqual(2);
  });

  it("should trigger eventerCallback twice when launching Resources sychronization", async () => {
    resources.eventer.on("loading", eventerCallback);
    await resources.sync(structures.all[0]);
    expect(eventerCallback).toBeCalled();
    expect(eventerCallback.mock.calls.length).toEqual(2);
  });
});

describe("getValues(key: string): Type[]", () => {
  it(`should returns an array of 2 objects which are '${
    data.listeRessources.ressource[0].niveauEducatif[0].nom
  }', '${data.listeRessources.ressource[0].niveauEducatif[1].nom}'`, () => {
    const levels = resources.getValues("niveauEducatif");

    expect(levels.length).toEqual(2);
    expect(levels[0].nom).toEqual(
      data.listeRessources.ressource[0].niveauEducatif[0].nom
    );
    expect(levels[1].nom).toEqual(
      data.listeRessources.ressource[0].niveauEducatif[1].nom
    );
  });

  describe("filters(filters): Resource[]", () => {
    it("should return all data when passing empties arrays as parameter", async () => {
      const filters = {
        levels: [],
        teachingFields: [],
        educationalTypes: [],
        typologies: []
      };
      const filteredData: Resource[] = resources.filter(filters);
      expect(JSON.stringify(filteredData)).toEqual(
        JSON.stringify(data.listeRessources.ressource)
      );
    });
  });

  const level = data.listeRessources.ressource[0].niveauEducatif[1];
  const typology = data.listeRessources.ressource[0].typologieDocument[1];
  const educationalType = data.listeRessources.ressource[0].typePedagogique[3];
  const teachingField =
    data.listeRessources.ressource[0].domaineEnseignement[0];

  it(`should return each resources matching '${level.nom}' level`, () => {
    const filters = {
      levels: [level],
      teachingFields: [],
      educationalTypes: [],
      typologies: []
    };
    const filteredData: Resource[] = resources.filter(filters);
    expect(JSON.stringify(filteredData)).toBe(
      JSON.stringify([data.listeRessources.ressource[0]])
    );
  });

  it(`should return each resources`, () => {
    const filters = {
      levels: [level],
      teachingFields: [teachingField],
      educationalTypes: [educationalType],
      typologies: [typology]
    };
    const filteredData: Resource[] = resources.filter(filters);
    expect(JSON.stringify(filteredData)).toBe(
      JSON.stringify(data.listeRessources.ressource)
    );
  });
});
