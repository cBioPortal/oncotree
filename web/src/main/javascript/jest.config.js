/** @type {import('ts-jest').JestConfigWithTsJest} **/
export default {
    testEnvironment: "jsdom",
    transform: {
      "^.+.tsx?$": [
        "ts-jest",
        { tsconfig: { target: "ES2020", lib: ["ES2020", "DOM"] } },
      ],
    },
  };
  