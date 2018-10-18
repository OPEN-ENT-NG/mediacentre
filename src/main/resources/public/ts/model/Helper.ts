export class Helper {
  /**
   * Generate regexp based on words array
   * @param words words to search
   */
  static generateRegexp(words: string[]): RegExp {
    function escapeRegExp(reg: string): string {
      return reg.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
    }
    let reg;
    if (words.length > 0) {
      reg = ".*(";
      words.map(
        (word: string) => (reg += `${escapeRegExp(word.toLowerCase())}|`)
      );
      reg = reg.slice(0, -1);
      reg += ").*";
    } else {
      reg = ".*";
    }
    return new RegExp(reg);
  }
}
