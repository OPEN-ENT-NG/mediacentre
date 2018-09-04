import { Structure } from "../index";
import { model } from "../__mocks__/entcore";

const structure: Structure = new Structure(
  model.me.structures[0],
  model.me.structureNames[0]
);

describe("constructor()", () => {
  it(`should create a Structure with ${
    model.me.structureNames[0]
  } as name and ${model.me.structures[0]} as id`, () => {
    expect(structure.id).toEqual(model.me.structures[0]);
    expect(structure.name).toEqual(model.me.structureNames[0]);
  });
});

describe("getId(): string", () => {
  it(`should return ${model.me.structures[0]} as identifier`, () => {
    expect(structure.getId()).toEqual(model.me.structures[0]);
  });
});

describe("getName(): string", () => {
  it(`should return ${model.me.structureNames[0]} as identifier`, () => {
    expect(structure.getName()).toEqual(model.me.structureNames[0]);
  });
});
