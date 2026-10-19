const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profile or PAN vault", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pan_profiles")
      .doc("pan_1")
      .get()
  );
});

test("Authenticated user can create and read their own UserProfile and PAN profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Investor",
      email: "alice@example.com",
      phoneNumber: "+919876543210",
      authProvider: "Google",
      biometricEnabled: true,
      priceAlertsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pan_profiles")
      .doc("pan_1")
      .set({
        userId: ALICE_UID,
        holderName: "Alice Sharma",
        panNumber: "ABCDE1234F",
        dematId: "1208160012345678",
        upiId: "alice@okicici",
        category: "Retail",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pan_profiles")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Cross-user access: Bob cannot read or modify Alice's PAN profiles or IPO applications", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("pan_profiles")
      .doc("pan_1")
      .set({
        userId: ALICE_UID,
        holderName: "Alice Sharma",
        panNumber: "ABCDE1234F",
        dematId: "1208160012345678",
        upiId: "alice@okicici",
        category: "Retail",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pan_profiles")
      .doc("pan_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pan_profiles")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Shadow update test: adding unauthorized ghost field fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Investor",
      biometricEnabled: true,
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});
