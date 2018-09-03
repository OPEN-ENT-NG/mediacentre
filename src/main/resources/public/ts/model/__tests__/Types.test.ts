import { Types } from "../Type";

import data from "../__mocks__/ressources";

const types = new Types(data.listeRessources.ressource[0].niveauEducatif);

describe("constructor(arr = [])", () => {
  it("should init Types collection with and empty array named 'all'", () => {
    const types = new Types();

    expect(types.all).toBeTruthy();
    expect(types.all).toBeInstanceOf(Array);
    expect(types.all.length).toEqual(0);
  });

  it(`should init Types collection with an array containing 2 items which are '${
    data.listeRessources.ressource[0].niveauEducatif[0].nom
  }' and '${data.listeRessources.ressource[0].niveauEducatif[1].nom}'`, () => {
    expect(types.all.length).toEqual(2);
    expect(types.all[0].nom).toEqual(
      data.listeRessources.ressource[0].niveauEducatif[0].nom
    );
    expect(types.all[1].nom).toEqual(
      data.listeRessources.ressource[0].niveauEducatif[1].nom
    );
  });
});
