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
const OWNER_UID = "owner_admin_999";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("1. Unauthenticated user cannot read media_items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("media_items").where("isPublished", "==", true).get());
});

test("2. Cross-user PII isolation: Bob cannot read Alice's private user profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("3. Unfiltered query rejection: Alice cannot list watchlists without userId or isPublic filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("watchlists").get());
});

test("4. Aligned query succeeds: Alice can list her own watchlists and published media_items", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("media_items").doc("film_1").set({
      titleAr: "المدرعة بوتيمكين",
      titleOriginal: "Броненосец Потёмкин",
      kind: "movie",
      year: 1925,
      country: "الاتحاد السوفيتي",
      streamStatus: "LICENSED_ACTIVE",
      isPublished: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("media_items").where("isPublished", "==", true).get());
  await assertSucceeds(aliceDb.collection("watchlists").where("userId", "==", ALICE_UID).get());
});

test("5. Email spoofing blocked: unverified owner email cannot create media_items", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "hxbdhdhdhhl@gmail.com",
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofDb.collection("media_items").doc("spoof_item").set({
      titleAr: "مزيف",
      titleOriginal: "Fake",
      kind: "movie",
      year: 2024,
      country: "USA",
      streamStatus: "ARCHIVE_METADATA_ONLY",
      isPublished: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("6. Verified owner email CAN create media_items and video_sources", async () => {
  const adminDb = testEnv.authenticatedContext(OWNER_UID, {
    email: "hxbdhdhdhhl@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("media_items").doc("soviet_1").set({
      titleAr: "فجر هنا هادئ",
      titleOriginal: "А зори здесь тихие",
      kind: "series",
      year: 1972,
      country: "الاتحاد السوفيتي",
      streamStatus: "LICENSED_ACTIVE",
      isPublished: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("7. Composite review ID prevents duplicate or spoofed review IDs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = {
    userId: ALICE_UID,
    authorName: "Alice",
    mediaId: "film_1",
    rating: 5,
    comment: "تحفة سينمائية خالدة",
    status: "approved",
    createdAt: new Date(Date.now() - 500),
    updatedAt: new Date(Date.now() - 500),
  };

  await assertFails(aliceDb.collection("reviews").doc("wrong_id").set(payload));
  await assertSucceeds(aliceDb.collection("reviews").doc(`${ALICE_UID}_film_1`).set(payload));
});

test("8. Fake free VIP self-grant is rejected while verified Play token is accepted", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = Date.now();
  await assertFails(
    bobDb.collection("subscriptions").doc(BOB_UID).set({
      userId: BOB_UID,
      planId: "vip_monthly",
      status: "active",
      provider: "admin_grant",
      purchaseToken: "fake_admin_grant",
      startsAtEpochMs: now,
      expiresAtEpochMs: now + 2592000000,
      createdAt: new Date(now - 500),
      updatedAt: new Date(now - 500),
    })
  );

  await assertSucceeds(
    bobDb.collection("subscriptions").doc(BOB_UID).set({
      userId: BOB_UID,
      planId: "vip_monthly",
      status: "active",
      provider: "google_play",
      purchaseToken: "gp_token_verified_987654",
      startsAtEpochMs: now,
      expiresAtEpochMs: now + 2592000000,
      createdAt: new Date(now - 500),
      updatedAt: new Date(now - 500),
    })
  );
});
