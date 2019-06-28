import { Event } from "../index";
import data from "../__mocks__/ressources";

import http from "axios";
import MockAdapter from "axios-mock-adapter";

const httpMock = new MockAdapter(http);

const resource = data.listeRessources.ressource[0];
const event = new Event(resource);

const mockedEvent = {
  idRessource: resource.idRessource,
  nomRessource: resource.nomRessource,
  nomEditeur: resource.nomEditeur,
  urlVignette: resource.urlVignette,
  typePedagogique: resource.typePedagogique,
  niveauEducatif: resource.niveauEducatif,
  typePresentation: resource.typePresentation,
  typologieDocument: resource.typologieDocument,
  domaineEnseignement: resource.domaineEnseignement
};

describe("constructor()", () => {
  it("should initialize Event with same information as the mock", () => {
    expect(event.idRessource).toBe(resource.idRessource);
    expect(event.nomRessource).toBe(resource.nomRessource);
    expect(event.nomEditeur).toBe(resource.nomEditeur);
    expect(event.urlVignette).toBe(resource.urlVignette);
    expect(event.typePedagogique).toEqual(resource.typePedagogique);
    expect(event.niveauEducatif).toEqual(resource.niveauEducatif);
    expect(event.typePresentation).toEqual(resource.typePresentation);
    expect(event.typologieDocument).toEqual(resource.typologieDocument);
    expect(event.domaineEnseignement).toEqual(resource.domaineEnseignement);
  });
});

describe("toJSON()", () => {
  it("should match with the mocked event", () => {
    expect(event.toJSON()).toEqual(mockedEvent);
  });
});

describe("save(): Promise<any>", () => {
  it("should send data in body matching mocked event", async () => {
    // Here is a little be tricky. You can't expect a body sent with a Post request. So here we
    // mock the post data and return it to expect the response and compare the response body with the
    // mocked event
    httpMock
      .onPost("/gar/event")
      .reply(config => [200, JSON.parse(config.data)]);
    const { data } = await event.save();

    expect(data).toEqual(mockedEvent);
  });
});
