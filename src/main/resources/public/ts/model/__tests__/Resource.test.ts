import { Resources, Resource, Structures } from "../index";

declare let global: any;

const windowObj = {
  focus: jest.fn()
};
global.open = jest.fn().mockReturnValue(windowObj);

const structures = new Structures();
structures.sync();

describe("open()", () => {
  it("it should open a new window tab with the urlAccesRessource", async () => {
    const resources = new Resources();
    await resources.sync(structures.all[0]);

    const resource: Resource = resources.all[0];
    resource.open();

    expect(global.open).toBeCalled();
    expect(global.open.mock.calls[0][0]).toEqual(
      resources.all[0].urlAccesRessource
    );
    expect(global.open.mock.calls[0][1]).toEqual("_blank");
    expect(windowObj.focus).toBeCalled();
  });
});
