import { Mix } from "entcore-toolkit";

export class Type {
  uri: string;
  nom: string;

  constructor(nom: string = "", uri: string = "") {
    this.nom = nom;
    this.uri = uri;
  }

  toString(): string {
    return this.nom;
  }
}

export class Types {
  all: Type[];

  constructor(arr = []) {
    this.all = Mix.castArrayAs(Type, arr);
  }
}
