import { Structures } from "../index";
import { model } from "../__mocks__/entcore";

const structures: Structures = new Structures();

describe("constructor()", () => {
  it("should initialize Structures object with an empty array named 'all'", () => {
    expect(structures).toBeTruthy();
    expect("all" in structures).toBeTruthy();
    expect(structures.all.length).toEqual(0);
  });
});

describe("sync(): void", () => {
  it(`should fill the all array with 1 element with ${
    model.me.structureNames[0]
  } as name and ${model.me.structures[0]} as identifier`, () => {
    structures.sync();

    expect(structures.all.length).toEqual(1);
    expect(structures.all[0].getName()).toEqual(model.me.structureNames[0]);
    expect(structures.all[0].getId()).toEqual(model.me.structures[0]);
  });
});
