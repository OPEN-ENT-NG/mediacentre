import { model } from "entcore";

export class Structure {
  id: string;
  name: string;

  constructor(id: string, name: string) {
    this.id = id;
    this.name = name;
  }

  getId(): string {
    return this.id;
  }

  getName(): string {
    return this.name;
  }
}

export class Structures {
  all: Structure[];

  constructor() {
    this.all = [];
  }

  sync(): void {
    for (let i = 0; i < model.me.structures.length; i++) {
      this.all.push(
        new Structure(model.me.structures[i], model.me.structureNames[i])
      );
    }
  }
}
