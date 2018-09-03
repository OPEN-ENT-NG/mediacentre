module.exports = {
  transform: {
    ".(ts|tsx)": "<rootDir>/node_modules/ts-jest/preprocessor.js"
  },
  testRegex: "(/__tests__/.*|\\.(test|spec))\\.(ts|tsx|js)$",
  testPathIgnorePatterns: [
    "bin",
    "<rootDir>/src/main/resources/public/ts/model/__tests__/setup.ts"
  ],
  moduleFileExtensions: ["ts", "tsx", "js"],
  verbose: true,
  testURL: "http://localhost/",
  setupFiles: [
    "<rootDir>/src/main/resources/public/ts/model/__tests__/setup.ts"
  ]
};
