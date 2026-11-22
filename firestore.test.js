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

test("Unauthenticated user: denied across all user-scoped collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.doc(`users/${ALICE_UID}`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/installments`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/priceTargets`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/productNotes`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/chatMessages`).get());

  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}`).set({
      userId: ALICE_UID,
      displayName: "Unauth",
      preferredTheme: "dark",
      languageCode: "ES",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("User profile: owner create, partial update, theme toggle, and security guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}`).set({
      userId: ALICE_UID,
      displayName: "Alice PVS",
      email: "alice@example.com",
      preferredTheme: "light",
      languageCode: "ES",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update toggling dark theme
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}`).update({
      preferredTheme: "dark",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field / invalid enum / future timestamp
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}`).update({
      preferredTheme: "neon",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}`).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}`).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice's profile, installments, targets, notes, or chat", async () => {
  const created = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`users/${ALICE_UID}`).set({
      userId: ALICE_UID,
      displayName: "Alice",
      preferredTheme: "dark",
      languageCode: "ES",
      createdAt: created,
      updatedAt: created,
    });
    await db.doc(`users/${ALICE_UID}/installments/inst_1`).set({
      userId: ALICE_UID,
      title: "Smart TV 50",
      code: "TV-50",
      category: "Tecnología",
      initialPaid: 97.25,
      totalAmount: 389.0,
      biweeklyQuota: 48.63,
      totalQuotas: 6,
      paidQuotas: 4,
      nextPaymentDate: "22 Nov 2026",
      sortOrder: 0,
      createdAt: created,
      updatedAt: created,
    });
    await db.doc(`users/${ALICE_UID}/priceTargets/TV-50`).set({
      userId: ALICE_UID,
      code: "TV-50",
      productName: "Smart TV 50",
      targetPrice: 350.0,
      createdAt: created,
      updatedAt: created,
    });
    await db.doc(`users/${ALICE_UID}/productNotes/note_1`).set({
      userId: ALICE_UID,
      productCode: "TV-50",
      productName: "Smart TV 50",
      noteText: "Garantía 2 años",
      createdAt: created,
      updatedAt: created,
    });
    await db.doc(`users/${ALICE_UID}/chatMessages/msg_1`).set({
      userId: ALICE_UID,
      role: "user",
      text: "Hola PVS",
      createdAt: created,
      updatedAt: created,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/installments`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/installments/inst_1`).update({ paidQuotas: 5 }));
  await assertFails(bobDb.collection(`users/${ALICE_UID}/priceTargets`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/productNotes`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/chatMessages`).get());
});

test("CloudInstallment: Kotlin-aligned create, payment update, and reject id field / overpaid quota", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 8000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/installments/inst_1`).set({
      userId: ALICE_UID,
      title: "Smart TV 50\" 4K UHD",
      code: "TV-50",
      category: "Tecnología",
      initialPaid: 97.25,
      totalAmount: 389.0,
      biweeklyQuota: 48.63,
      totalQuotas: 6,
      paidQuotas: 4,
      nextPaymentDate: "22 Nov 2026",
      sortOrder: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List query matching Kotlin snapshots()
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/installments`).orderBy("sortOrder").get()
  );

  // Partial payment update
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/installments/inst_1`).update({
      paidQuotas: 5,
      nextPaymentDate: "07 Dic 2026",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject paidQuotas > totalQuotas
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/installments/inst_1`).update({
      paidQuotas: 10,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId `id` leaked inside document map
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/installments/inst_2`).set({
      id: "inst_2",
      userId: ALICE_UID,
      title: "Bad Doc",
      code: "BAD-1",
      category: "Test",
      initialPaid: 0,
      totalAmount: 100,
      biweeklyQuota: 50,
      totalQuotas: 2,
      paidQuotas: 0,
      nextPaymentDate: "30 Nov 2026",
      sortOrder: 1,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("PriceTargets, ProductNotes & ChatMessages: Kotlin-aligned CRUD & bounds checks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 6000);

  // Price Target
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/priceTargets/TV-50`).set({
      userId: ALICE_UID,
      code: "TV-50",
      productName: "Smart TV 50",
      targetPrice: 340.0,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/priceTargets/TV-50`).update({
      targetPrice: 325.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Product Note
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/productNotes/note_1`).set({
      userId: ALICE_UID,
      productCode: "TV-50",
      productName: "Smart TV 50",
      noteText: "Envío gratis en tienda central",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/productNotes/note_1`).update({
      noteText: "Envío gratis y garantía 2 años",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Chat Message (with & without optional transcript / groundingNote)
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/chatMessages/msg_user_1`).set({
      userId: ALICE_UID,
      role: "user",
      text: "¿Me conviene la TV-50?",
      audioTranscript: "Transcripción de voz: Me conviene la TV-50",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/chatMessages/msg_model_1`).set({
      userId: ALICE_UID,
      role: "model",
      text: "Sí, está 15% debajo del promedio.",
      groundingNote: "Fuentes verificadas vía Google Search",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/chatMessages/msg_future`).set({
      userId: ALICE_UID,
      role: "user",
      text: "Future",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
